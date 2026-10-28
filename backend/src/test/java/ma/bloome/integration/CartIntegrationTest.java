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

import static ma.bloome.support.Payloads.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("Add, update and remove items; totals are computed server-side")
    void cartLifecycle() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long tulips = productId("eclat-de-tulipes");      // 290.00
        setProductStock(tulips, 20);

        // A client-supplied price must be ignored.
        Map<String, Object> body = new HashMap<>(productItem(tulips, 1));
        body.put("price", 1);
        body.put("total", 1);
        JsonNode cart = read(mvc.perform(postJson("/api/v1/cart/items", body).with(as(user)))
                .andExpect(status().isOk()).andReturn());
        assertThat(cart.get("subtotal").decimalValue()).isEqualByComparingTo("290.00");
        assertThat(cart.get("deliveryFee").decimalValue()).isEqualByComparingTo("35.00");
        assertThat(cart.get("total").decimalValue()).isEqualByComparingTo("325.00");

        // Adding the same product merges into one line.
        cart = read(mvc.perform(postJson("/api/v1/cart/items", productItem(tulips, 1)).with(as(user))).andReturn());
        assertThat(cart.get("items")).hasSize(1);
        String itemId = cart.get("items").get(0).get("id").asText();

        // Free delivery above the threshold (2 x 290 = 580 ≥ 500).
        assertThat(cart.get("deliveryFee").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(cart.get("total").decimalValue()).isEqualByComparingTo("580.00");

        cart = read(mvc.perform(patchJson("/api/v1/cart/items/" + itemId, Map.of("quantity", 3)).with(as(user)))
                .andExpect(status().isOk()).andReturn());
        assertThat(cart.get("subtotal").decimalValue()).isEqualByComparingTo("870.00");

        // Cart persists server-side across requests ("refresh").
        mvc.perform(get("/api/v1/cart").with(as(user))).andExpect(jsonPath("$.itemCount").value(3));

        mvc.perform(deleteReq("/api/v1/cart/items/" + itemId).with(as(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0.0));
    }

    @Test
    @DisplayName("Quantities above stock or the per-order limit are rejected")
    void validatesQuantities() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long peonies = productId("jardin-de-pivoines");
        setProductStock(peonies, 2);
        mvc.perform(postJson("/api/v1/cart/items", productItem(peonies, 3)).with(as(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        mvc.perform(postJson("/api/v1/cart/items", productItem(peonies, 0)).with(as(user)))
                .andExpect(status().isBadRequest());
        long calathea = productId("calathea-orbifolia"); // out of stock
        mvc.perform(postJson("/api/v1/cart/items", productItem(calathea, 1)).with(as(user)))
                .andExpect(status().isConflict());
        mvc.perform(postJson("/api/v1/cart/items", Map.of("quantity", 1)).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        setProductStock(peonies, 4);
    }

    @Test
    @DisplayName("A customer cannot modify another customer's cart item")
    void cartOwnership() throws Exception {
        User alice = createUser(Role.CUSTOMER);
        User bob = createUser(Role.CUSTOMER);
        long soleil = productId("soleil-d-ete");
        setProductStock(soleil, 30);
        JsonNode cart = read(mvc.perform(postJson("/api/v1/cart/items", productItem(soleil, 1)).with(as(alice))).andReturn());
        String aliceItem = cart.get("items").get(0).get("id").asText();
        mvc.perform(patchJson("/api/v1/cart/items/" + aliceItem, Map.of("quantity", 2)).with(as(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(deleteReq("/api/v1/cart/items/" + aliceItem).with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cart").with(as(alice))).andExpect(jsonPath("$.items[0].quantity").value(1));
    }

    @Test
    @DisplayName("Stock changes after adding are surfaced as line issues and block checkout readiness")
    void reportsStockChanges() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long lys = productId("lys-etoile");
        setProductStock(lys, 10);
        mvc.perform(postJson("/api/v1/cart/items", productItem(lys, 4)).with(as(user))).andExpect(status().isOk());
        setProductStock(lys, 2);
        mvc.perform(get("/api/v1/cart").with(as(user)))
                .andExpect(jsonPath("$.items[0].issue").value("QUANTITY_EXCEEDS_STOCK"))
                .andExpect(jsonPath("$.items[0].maxQuantity").value(2))
                .andExpect(jsonPath("$.checkoutReady").value(false));
        setProductStock(lys, 14);
    }

    @Test
    @DisplayName("Custom bouquets are validated and priced from the database")
    void customBouquet() throws Exception {
        User user = createUser(Role.CUSTOMER);
        // wrapping #3 = 20.00, ribbon #4 = 15.00, stem #1 rose rouge 22.00 x 6, stem #12 fougère 6.00 x 2
        JsonNode cart = read(mvc.perform(postJson("/api/v1/cart/items",
                        bouquetItem(3, 4, List.of(stem(1, 6), stem(12, 2)), 1)).with(as(user)))
                .andExpect(status().isOk()).andReturn());
        JsonNode line = cart.get("items").get(0);
        assertThat(line.get("type").asText()).isEqualTo("CUSTOM_BOUQUET");
        assertThat(line.get("unitPrice").decimalValue()).isEqualByComparingTo("179.00");
        assertThat(line.get("bouquet").get("stemCount").asInt()).isEqualTo(8);

        // Too few stems
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(3, 4, List.of(stem(1, 2)), 1)).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_BOUQUET"));
        // Above the per-stem limit (pivoine max 10)
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(3, 4, List.of(stem(4, 11)), 1)).with(as(user)))
                .andExpect(status().isBadRequest());
        // Unknown wrapping
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(999, 4, List.of(stem(1, 6)), 1)).with(as(user)))
                .andExpect(status().isBadRequest());
        // Duplicate stem
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(3, 4, List.of(stem(1, 3), stem(1, 3)), 1)).with(as(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Anonymous quote prices a browser cart without persisting it")
    void quoteForAnonymousCart() throws Exception {
        long velvet = productId("velvet-roses"); // 590.00
        setProductStock(velvet, 18);
        Map<String, Object> line = new HashMap<>(productItem(velvet, 1));
        line.put("clientKey", "local-1");
        Map<String, Object> unknown = new HashMap<>(productItem(999999, 1));
        unknown.put("clientKey", "local-2");
        mvc.perform(postJson("/api/v1/cart/quote?giftWrapping=true", Map.of("items", List.of(line, unknown))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("local-1"))
                .andExpect(jsonPath("$.items[1].issue").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.subtotal").value(590.0))
                .andExpect(jsonPath("$.giftWrappingFee").value(25.0))
                .andExpect(jsonPath("$.total").value(615.0));
    }

    @Test
    @DisplayName("Merging the anonymous cart after login sums quantities without duplicating lines")
    void mergesGuestCart() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long rosee = productId("rosee-du-matin");
        long petit = productId("petit-bonheur");
        setProductStock(rosee, 24);
        setProductStock(petit, 40);
        mvc.perform(postJson("/api/v1/cart/items", productItem(rosee, 1)).with(as(user))).andExpect(status().isOk());
        Map<String, Object> bouquet = bouquetItem(1, 1, List.of(stem(2, 5)), 1);

        JsonNode cart = read(mvc.perform(postJson("/api/v1/cart/merge", Map.of("items",
                        List.of(productItem(rosee, 2), productItem(petit, 1), bouquet, productItem(999999, 1))))
                        .with(as(user)))
                .andExpect(status().isOk()).andReturn());
        assertThat(cart.get("items")).hasSize(3);
        assertThat(quantityOf(cart, "Rosée du Matin")).isEqualTo(3);
        assertThat(quantityOf(cart, "Petit Bonheur")).isEqualTo(1);

        // Merging the same bouquet again increases its quantity instead of adding a new line.
        cart = read(mvc.perform(postJson("/api/v1/cart/merge", Map.of("items", List.of(bouquet))).with(as(user))).andReturn());
        assertThat(cart.get("items")).hasSize(3);
        assertThat(quantityOf(cart, "Bouquet personnalisé")).isEqualTo(2);
    }

    private static int quantityOf(JsonNode cart, String name) {
        for (JsonNode item : cart.get("items")) {
            if (item.get("name").asText().equals(name)) {
                return item.get("quantity").asInt();
            }
        }
        return 0;
    }
}
