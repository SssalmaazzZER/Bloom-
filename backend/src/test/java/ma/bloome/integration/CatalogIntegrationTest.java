package ma.bloome.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.bloome.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("Listing returns paginated active products with metadata")
    void listsProductsWithPagination() throws Exception {
        JsonNode page = read(mvc.perform(get("/api/v1/products").param("size", "5"))
                .andExpect(status().isOk()).andReturn());
        assertThat(page.get("content")).hasSize(5);
        assertThat(page.get("totalElements").asInt()).isGreaterThanOrEqualTo(16);
        assertThat(page.get("first").asBoolean()).isTrue();
        assertThat(page.get("content").get(0).has("price")).isTrue();
    }

    @Test
    @DisplayName("Search matches name and description, case-insensitively")
    void searchesByNameAndDescription() throws Exception {
        JsonNode page = read(mvc.perform(get("/api/v1/products").param("q", "PIVOINE")).andReturn());
        List<String> names = names(page);
        assertThat(names).contains("Jardin de Pivoines");
        JsonNode none = read(mvc.perform(get("/api/v1/products").param("q", "xyz-introuvable")).andReturn());
        assertThat(none.get("totalElements").asInt()).isZero();
    }

    @Test
    @DisplayName("LIKE wildcards in the search term are escaped")
    void escapesLikeWildcards() throws Exception {
        JsonNode page = read(mvc.perform(get("/api/v1/products").param("q", "%")).andReturn());
        assertThat(page.get("totalElements").asInt()).isZero();
    }

    @Test
    @DisplayName("Category, price, color, occasion and availability filters use real data")
    void filtersCombine() throws Exception {
        JsonNode plants = read(mvc.perform(get("/api/v1/products").param("category", "plantes").param("size", "48")).andReturn());
        assertThat(plants.get("content")).allSatisfy(p -> assertThat(p.get("categorySlug").asText()).isEqualTo("plantes"));

        JsonNode cheap = read(mvc.perform(get("/api/v1/products").param("minPrice", "200").param("maxPrice", "400").param("size", "48")).andReturn());
        assertThat(cheap.get("content")).isNotEmpty().allSatisfy(p -> {
            BigDecimal price = p.get("price").decimalValue();
            assertThat(price).isBetween(new BigDecimal("200"), new BigDecimal("400"));
        });

        JsonNode white = read(mvc.perform(get("/api/v1/products").param("color", "WHITE").param("occasion", "condoleances")).andReturn());
        assertThat(names(white)).contains("Fleurs de Coton", "Douce Mémoire").doesNotContain("Velvet Roses");

        JsonNode all = read(mvc.perform(get("/api/v1/products").param("category", "plantes").param("size", "48")).andReturn());
        JsonNode inStock = read(mvc.perform(get("/api/v1/products").param("category", "plantes").param("inStock", "true").param("size", "48")).andReturn());
        assertThat(names(all)).contains("Calathea Orbifolia");
        assertThat(names(inStock)).doesNotContain("Calathea Orbifolia");
    }

    @Test
    @DisplayName("Sorting by price ascending and descending")
    void sortsByPrice() throws Exception {
        JsonNode asc = read(mvc.perform(get("/api/v1/products").param("sort", "PRICE_ASC").param("size", "48")).andReturn());
        List<BigDecimal> prices = new ArrayList<>();
        asc.get("content").forEach(p -> prices.add(p.get("price").decimalValue()));
        assertThat(prices).isSorted();
        JsonNode desc = read(mvc.perform(get("/api/v1/products").param("sort", "PRICE_DESC").param("size", "1")).andReturn());
        assertThat(desc.get("content").get(0).get("price").decimalValue()).isEqualByComparingTo(prices.get(prices.size() - 1));
    }

    @Test
    @DisplayName("Invalid filter values are rejected with a 400")
    void rejectsInvalidFilters() throws Exception {
        mvc.perform(get("/api/v1/products").param("minPrice", "500").param("maxPrice", "100"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
        mvc.perform(get("/api/v1/products").param("sort", "BOGUS"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Product details by slug include images, occasions and a delivery estimate")
    void productDetails() throws Exception {
        mvc.perform(get("/api/v1/products/jardin-de-pivoines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jardin de Pivoines"))
                .andExpect(jsonPath("$.images.length()").value(3))
                .andExpect(jsonPath("$.occasions").isNotEmpty())
                .andExpect(jsonPath("$.deliveryEstimate.message").isNotEmpty());
        mvc.perform(get("/api/v1/products/nexiste-pas"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Inactive products are hidden from the shop")
    void inactiveProductsHidden() throws Exception {
        long id = productId("bonsai-ficus");
        jdbc.update("update products set active = false where id = ?", id);
        try {
            mvc.perform(get("/api/v1/products/bonsai-ficus")).andExpect(status().isNotFound());
            JsonNode page = read(mvc.perform(get("/api/v1/products").param("q", "bonsaï")).andReturn());
            assertThat(names(page)).doesNotContain("Bonsaï Ficus");
        } finally {
            jdbc.update("update products set active = true where id = ?", id);
        }
    }

    @Test
    @DisplayName("Categories, occasions, facets and bouquet options are public")
    void referenceData() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/occasions")).andExpect(status().isOk()).andExpect(jsonPath("$[0].slug").exists());
        mvc.perform(get("/api/v1/catalog/facets")).andExpect(status().isOk()).andExpect(jsonPath("$.maxPrice").exists());
        mvc.perform(get("/api/v1/bouquet/options")).andExpect(status().isOk())
                .andExpect(jsonPath("$.stems.length()").value(12)).andExpect(jsonPath("$.minStems").value(5));
        mvc.perform(get("/api/v1/products/featured")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/bestsellers")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/delivery/options")).andExpect(status().isOk()).andExpect(jsonPath("$.cities").isArray());
    }

    private static List<String> names(JsonNode page) {
        List<String> names = new ArrayList<>();
        page.get("content").forEach(p -> names.add(p.get("name").asText()));
        return names;
    }
}
