package ma.bloome.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProductIntegrationTest extends IntegrationTest {

    private Map<String, Object> product(String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("shortDescription", "Un bouquet de test pour l'administration.");
        m.put("description", "Description complète du bouquet de test, avec suffisamment de détails.");
        m.put("composition", "Roses, eucalyptus");
        m.put("categoryId", jdbc.queryForObject("select id from categories where slug = 'bouquets'", Long.class));
        m.put("price", 315.50);
        m.put("stockQuantity", 12);
        m.put("lowStockThreshold", 3);
        m.put("maxPerOrder", 5);
        m.put("active", true);
        m.put("featured", false);
        m.put("bestseller", false);
        m.put("colors", Set.of("PINK"));
        m.put("occasionIds", Set.of(jdbc.queryForObject("select id from occasions where slug = 'amour'", Long.class)));
        m.put("images", List.of(Map.of("url", "https://images.unsplash.com/photo-1563241527-3004b7be0ffd?w=800", "altText", "Bouquet")));
        return m;
    }

    @Test
    @DisplayName("Admin creates a product that immediately appears in the public catalog")
    void createsProduct() throws Exception {
        User admin = createUser(Role.ADMIN);
        JsonNode created = read(mvc.perform(postJson("/api/v1/admin/products", product("Brise Légère")).with(as(admin)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(created.get("slug").asText()).isEqualTo("brise-legere");
        assertThat(created.get("price").decimalValue()).isEqualByComparingTo("315.50");

        mvc.perform(get("/api/v1/products/brise-legere")).andExpect(status().isOk())
                .andExpect(jsonPath("$.occasions[0].slug").value("amour"));

        // Same name again → unique slug generated.
        mvc.perform(postJson("/api/v1/admin/products", product("Brise Légère")).with(as(admin)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("brise-legere-2"));
    }

    @Test
    @DisplayName("Invalid prices, stock, URLs and categories are rejected")
    void validatesProduct() throws Exception {
        User admin = createUser(Role.ADMIN);
        Map<String, Object> negative = product("Prix négatif");
        negative.put("price", -1);
        mvc.perform(postJson("/api/v1/admin/products", negative).with(as(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("price"));

        Map<String, Object> stock = product("Stock négatif");
        stock.put("stockQuantity", -5);
        mvc.perform(postJson("/api/v1/admin/products", stock).with(as(admin))).andExpect(status().isBadRequest());

        Map<String, Object> url = product("Image dangereuse");
        url.put("images", List.of(Map.of("url", "javascript:alert(1)")));
        mvc.perform(postJson("/api/v1/admin/products", url).with(as(admin))).andExpect(status().isBadRequest());

        Map<String, Object> compare = product("Prix barré incohérent");
        compare.put("compareAtPrice", 100);
        mvc.perform(postJson("/api/v1/admin/products", compare).with(as(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PRICE"));

        Map<String, Object> category = product("Catégorie inconnue");
        category.put("categoryId", 99999);
        mvc.perform(postJson("/api/v1/admin/products", category).with(as(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
    }

    @Test
    @DisplayName("Updates use optimistic locking; stale versions are refused")
    void optimisticLocking() throws Exception {
        User admin = createUser(Role.ADMIN);
        JsonNode created = read(mvc.perform(postJson("/api/v1/admin/products", product("Version Test")).with(as(admin))).andReturn());
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();

        Map<String, Object> update = product("Version Test");
        update.put("price", 330);
        update.put("version", version);
        JsonNode updated = read(mvc.perform(putJson("/api/v1/admin/products/" + id, update).with(as(admin)))
                .andExpect(status().isOk()).andReturn());
        assertThat(updated.get("version").asLong()).isGreaterThan(version);

        update.put("price", 340);
        update.put("version", version); // stale
        mvc.perform(putJson("/api/v1/admin/products/" + id, update).with(as(admin)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("Stock adjustments are atomic and cannot go negative; deactivation hides the product")
    void stockAndDeactivation() throws Exception {
        User admin = createUser(Role.ADMIN);
        long id = read(mvc.perform(postJson("/api/v1/admin/products", product("Stock Test")).with(as(admin))).andReturn())
                .get("id").asLong();
        mvc.perform(patchJson("/api/v1/admin/products/" + id + "/stock", Map.of("delta", 8)).with(as(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(20));
        mvc.perform(patchJson("/api/v1/admin/products/" + id + "/stock", Map.of("delta", -21)).with(as(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STOCK"));
        assertThat(productStock(id)).isEqualTo(20);

        mvc.perform(deleteReq("/api/v1/admin/products/" + id).with(as(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/v1/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/products/" + id).with(as(admin))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Inventory lists products and bouquet stems; stem stock can be adjusted")
    void inventory() throws Exception {
        User admin = createUser(Role.ADMIN);
        mvc.perform(get("/api/v1/admin/inventory").with(as(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stems.length()").value(12));
        int before = stemStock(12);
        mvc.perform(patchJson("/api/v1/admin/stems/12/stock", Map.of("delta", 5)).with(as(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(before + 5));
    }

    @Test
    @DisplayName("Categories: create, conflict on duplicate slug, refuse deleting non-empty")
    void categories() throws Exception {
        User admin = createUser(Role.ADMIN);
        Map<String, Object> cat = Map.of("name", "Fleurs séchées", "slug", "fleurs-sechees", "sortOrder", 9);
        JsonNode created = read(mvc.perform(postJson("/api/v1/admin/categories", cat).with(as(admin)))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(postJson("/api/v1/admin/categories", cat).with(as(admin))).andExpect(status().isConflict());
        long bouquets = jdbc.queryForObject("select id from categories where slug = 'bouquets'", Long.class);
        mvc.perform(deleteReq("/api/v1/admin/categories/" + bouquets).with(as(admin)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_NOT_EMPTY"));
        mvc.perform(deleteReq("/api/v1/admin/categories/" + created.get("id").asLong()).with(as(admin)))
                .andExpect(status().isNoContent());
    }
}
