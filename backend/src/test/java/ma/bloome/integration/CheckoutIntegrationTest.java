package ma.bloome.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static ma.bloome.support.Payloads.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("Successful COD checkout: backend total, persisted snapshots, stock decrement, cart emptied")
    void successfulCheckout() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long velvet = productId("velvet-roses"); // 590.00
        setProductStock(velvet, 10);
        int roseStemBefore = stemStock(1);
        mvc.perform(postJson("/api/v1/cart/items", productItem(velvet, 2)).with(as(user))).andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(1, 1, List.of(stem(1, 5)), 2)).with(as(user)))
                .andExpect(status().isOk());

        // 2 x 590 + 2 x (0 + 0 + 5 x 22) = 1180 + 220 = 1400; free delivery; gift wrapping +25
        Map<String, Object> request = with(checkout(newKey(), validDeliveryDate()), "giftWrapping", true);
        request = with(request, "expectedTotal", 1425.00);
        JsonNode order = read(mvc.perform(postJson("/api/v1/orders", request).with(as(user)))
                .andExpect(status().isCreated()).andReturn());

        assertThat(order.get("reference").asText()).matches("BLM-\\d{6}-[A-Z2-9]{5}");
        assertThat(order.get("status").asText()).isEqualTo("PENDING");
        assertThat(order.get("paymentStatus").asText()).isEqualTo("UNPAID");
        assertThat(order.get("subtotal").decimalValue()).isEqualByComparingTo("1400.00");
        assertThat(order.get("deliveryFee").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(order.get("giftWrappingFee").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("1425.00");
        assertThat(order.get("recipientName").asText()).isEqualTo("Nadia Bennani");
        assertThat(order.get("customerPhone").asText()).isEqualTo("0612345678");
        assertThat(order.get("items")).hasSize(2);
        assertThat(order.get("history").get(0).get("status").asText()).isEqualTo("PENDING");

        assertThat(productStock(velvet)).isEqualTo(8);
        assertThat(stemStock(1)).isEqualTo(roseStemBefore - 10);
        mvc.perform(get("/api/v1/cart").with(as(user))).andExpect(jsonPath("$.items.length()").value(0));

        // Snapshot survives later price changes.
        jdbc.update("update products set price = 999 where id = ?", velvet);
        mvc.perform(get("/api/v1/orders/" + order.get("id").asLong()).with(as(user)))
                .andExpect(jsonPath("$.items[0].unitPrice").value(590.0))
                .andExpect(jsonPath("$.items[1].bouquet.stems[0].name").value("Rose rouge"));
        jdbc.update("update products set price = 590 where id = ?", velvet);
    }

    @Test
    @DisplayName("Insufficient stock at checkout: 409, nothing decremented, cart preserved")
    void insufficientStockRollsBack() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long dahlia = productId("dahlia-d-automne");
        long soleil = productId("soleil-d-ete");
        setProductStock(dahlia, 5);
        setProductStock(soleil, 30);
        mvc.perform(postJson("/api/v1/cart/items", productItem(soleil, 2)).with(as(user))).andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/cart/items", productItem(dahlia, 3)).with(as(user))).andExpect(status().isOk());
        setProductStock(dahlia, 1); // someone else bought them in the meantime

        mvc.perform(postJson("/api/v1/orders", checkout(newKey(), validDeliveryDate())).with(as(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CART_NEEDS_REVIEW"));
        assertThat(productStock(soleil)).isEqualTo(30);
        mvc.perform(get("/api/v1/cart").with(as(user))).andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("Atomic reservation: when stock vanishes between pricing and reservation, the whole order rolls back")
    void reservationFailureRollsBackEarlierDecrements() throws Exception {
        // Stem stock is checked per bouquet line; two bouquet lines together exceed the stock.
        User user = createUser(Role.CUSTOMER);
        jdbc.update("update bouquet_stems set stock_quantity = 12 where id = 4");
        long monstera = productId("monstera-deliciosa");
        setProductStock(monstera, 9);
        mvc.perform(postJson("/api/v1/cart/items", productItem(monstera, 1)).with(as(user))).andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(2, 2, List.of(stem(4, 8)), 1)).with(as(user))).andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/cart/items", bouquetItem(3, 3, List.of(stem(4, 7)), 1)).with(as(user))).andExpect(status().isOk());

        mvc.perform(postJson("/api/v1/orders", checkout(newKey(), validDeliveryDate())).with(as(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertThat(productStock(monstera)).isEqualTo(9);   // product decrement rolled back
        assertThat(stemStock(4)).isEqualTo(12);
        mvc.perform(get("/api/v1/cart").with(as(user))).andExpect(jsonPath("$.items.length()").value(3));
        jdbc.update("update bouquet_stems set stock_quantity = 60 where id = 4");
    }

    @Test
    @DisplayName("Concurrent checkouts for the last units: exactly one succeeds, stock never goes negative")
    void concurrentCheckoutsDoNotOversell() throws Exception {
        long hortensia = productId("hortensia-bleu-nuit");
        setProductStock(hortensia, 1);
        int customers = 6;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < customers; i++) {
            User u = createUser(Role.CUSTOMER);
            users.add(u);
            mvc.perform(postJson("/api/v1/cart/items", productItem(hortensia, 1)).with(as(u))).andExpect(status().isOk());
        }
        ExecutorService pool = Executors.newFixedThreadPool(customers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (User u : users) {
            results.add(pool.submit(() -> {
                start.await();
                return mvc.perform(postJson("/api/v1/orders", checkout(newKey(), validDeliveryDate())).with(as(u)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(customers - 1);
        assertThat(productStock(hortensia)).isZero();
        setProductStock(hortensia, 3);
    }

    @Test
    @DisplayName("Repeated submission with the same idempotency key returns the same order once")
    void idempotentCheckout() throws Exception {
        User user = createUser(Role.CUSTOMER);
        long petit = productId("petit-bonheur");
        setProductStock(petit, 40);
        mvc.perform(postJson("/api/v1/cart/items", productItem(petit, 1)).with(as(user))).andExpect(status().isOk());
        String key = newKey();
        JsonNode first = read(mvc.perform(postJson("/api/v1/orders", checkout(key, validDeliveryDate())).with(as(user)))
                .andExpect(status().isCreated()).andReturn());
        JsonNode second = read(mvc.perform(postJson("/api/v1/orders", checkout(key, validDeliveryDate())).with(as(user)))
                .andExpect(status().isOk()).andReturn());
        assertThat(second.get("reference").asText()).isEqualTo(first.get("reference").asText());
        assertThat(productStock(petit)).isEqualTo(39);
        Integer orders = jdbc.queryForObject("select count(*) from customer_orders where user_id = ?", Integer.class, user.getId());
        assertThat(orders).isEqualTo(1);
    }

    @Test
    @DisplayName("Checkout validation: empty cart, unknown city, past date, bad phone, changed price")
    void checkoutValidation() throws Exception {
        User user = createUser(Role.CUSTOMER);
        mvc.perform(postJson("/api/v1/orders", checkout(newKey(), validDeliveryDate())).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CART_EMPTY"));

        long trio = productId("trio-de-succulentes"); // 180.00
        setProductStock(trio, 35);
        mvc.perform(postJson("/api/v1/cart/items", productItem(trio, 1)).with(as(user))).andExpect(status().isOk());

        mvc.perform(postJson("/api/v1/orders", with(checkout(newKey(), validDeliveryDate()), "city", "Agadir")).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CITY_NOT_SERVED"));
        LocalDate yesterday = LocalDate.now(ZoneId.of("Africa/Casablanca")).minusDays(1);
        mvc.perform(postJson("/api/v1/orders", checkout(newKey(), yesterday)).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DELIVERY_DATE_TOO_EARLY"));
        mvc.perform(postJson("/api/v1/orders", with(checkout(newKey(), validDeliveryDate()), "customerPhone", "12345")).with(as(user)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("customerPhone"));
        mvc.perform(postJson("/api/v1/orders", with(checkout(newKey(), validDeliveryDate()), "customerEmail", "pas-un-email")).with(as(user)))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/v1/orders", with(checkout(newKey(), validDeliveryDate()), "expectedTotal", 1.00)).with(as(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
        // 180 + 35 delivery
        mvc.perform(postJson("/api/v1/orders", with(checkout(newKey(), validDeliveryDate()), "expectedTotal", 215.00)).with(as(user)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Customers only see their own orders; others get 404")
    void orderOwnership() throws Exception {
        User owner = createUser(Role.CUSTOMER);
        User stranger = createUser(Role.CUSTOMER);
        long orderId = placeOrder(owner, "petit-bonheur");
        mvc.perform(get("/api/v1/orders/" + orderId).with(as(owner))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/" + orderId).with(as(stranger))).andExpect(status().isNotFound());
        mvc.perform(postJson("/api/v1/orders/" + orderId + "/cancel", Map.of()).with(as(stranger))).andExpect(status().isNotFound());
        JsonNode mine = read(mvc.perform(get("/api/v1/orders/my").with(as(stranger))).andReturn());
        assertThat(mine.get("totalElements").asInt()).isZero();
        mvc.perform(get("/api/v1/orders/my").with(as(owner))).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Cancellation: allowed while PENDING (stock restored), refused once PREPARING")
    void cancellationRules() throws Exception {
        User user = createUser(Role.CUSTOMER);
        User admin = createUser(Role.ADMIN);
        long rosee = productId("rosee-du-matin");
        setProductStock(rosee, 10);

        long first = placeOrder(user, "rosee-du-matin");
        assertThat(productStock(rosee)).isEqualTo(9);
        mvc.perform(postJson("/api/v1/orders/" + first + "/cancel", Map.of("reason", "Erreur de date")).with(as(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("VOID"));
        assertThat(productStock(rosee)).isEqualTo(10);
        mvc.perform(postJson("/api/v1/orders/" + first + "/cancel", Map.of()).with(as(user)))
                .andExpect(status().isConflict());

        long second = placeOrder(user, "rosee-du-matin");
        mvc.perform(patchJson("/api/v1/admin/orders/" + second + "/status", Map.of("status", "CONFIRMED")).with(as(admin)))
                .andExpect(status().isOk());
        mvc.perform(patchJson("/api/v1/admin/orders/" + second + "/status", Map.of("status", "PREPARING")).with(as(admin)))
                .andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/orders/" + second + "/cancel", Map.of()).with(as(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CANCELLATION_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Admin status transitions follow the lifecycle; delivery marks COD as paid; reviews unlock")
    void adminTransitions() throws Exception {
        User user = createUser(Role.CUSTOMER);
        User admin = createUser(Role.ADMIN);
        long orderId = placeOrder(user, "orchidee-elegance");
        String url = "/api/v1/admin/orders/" + orderId + "/status";

        mvc.perform(patchJson(url, Map.of("status", "DELIVERED")).with(as(admin)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mvc.perform(patchJson(url, Map.of("status", "BOGUS")).with(as(admin))).andExpect(status().isBadRequest());

        // Reviews are refused before delivery.
        mvc.perform(postJson("/api/v1/products/orchidee-elegance/reviews",
                        Map.of("rating", 5, "comment", "Magnifique orchidée, très bien emballée.")).with(as(user)))
                .andExpect(status().isForbidden());

        for (String next : new String[]{"CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY"}) {
            mvc.perform(patchJson(url, Map.of("status", next, "note", "ok")).with(as(admin)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        mvc.perform(patchJson(url, Map.of("status", "CANCELLED")).with(as(admin))).andExpect(status().isConflict());
        mvc.perform(patchJson(url, Map.of("status", "DELIVERED")).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(0))
                .andExpect(jsonPath("$.history.length()").value(5));

        mvc.perform(postJson("/api/v1/products/orchidee-elegance/reviews",
                        Map.of("rating", 5, "title", "Superbe", "comment", "Magnifique orchidée, très bien emballée.")).with(as(user)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorName").value("Test C."));
        mvc.perform(postJson("/api/v1/products/orchidee-elegance/reviews",
                        Map.of("rating", 4, "comment", "Deuxième avis sur le même produit.")).with(as(user)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/products/orchidee-elegance/reviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.average").value(5.0));
    }

    @Test
    @DisplayName("Admin can search orders and dashboard revenue excludes cancelled orders")
    void adminOrderSearchAndDashboard() throws Exception {
        User user = createUser(Role.CUSTOMER);
        User admin = createUser(Role.ADMIN);
        JsonNode before = read(mvc.perform(get("/api/v1/admin/dashboard").with(as(admin))).andReturn());
        long kept = placeOrder(user, "soleil-d-ete");
        long cancelled = placeOrder(user, "soleil-d-ete");
        mvc.perform(postJson("/api/v1/orders/" + cancelled + "/cancel", Map.of()).with(as(user))).andExpect(status().isOk());

        JsonNode after = read(mvc.perform(get("/api/v1/admin/dashboard").with(as(admin))).andReturn());
        assertThat(after.get("totalOrders").asLong()).isEqualTo(before.get("totalOrders").asLong() + 2);
        // Soleil d'Été 320 + 35 delivery = 355 counted once.
        assertThat(after.get("revenue").decimalValue().subtract(before.get("revenue").decimalValue()))
                .isEqualByComparingTo("355.00");

        String reference = read(mvc.perform(get("/api/v1/orders/" + kept).with(as(user))).andReturn()).get("reference").asText();
        mvc.perform(get("/api/v1/admin/orders").param("q", reference.toLowerCase()).with(as(admin)))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(kept));
        mvc.perform(get("/api/v1/admin/orders").param("status", "CANCELLED").with(as(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/customers").param("q", user.getEmail()).with(as(admin)))
                .andExpect(jsonPath("$.content[0].orderCount").value(2))
                .andExpect(jsonPath("$.content[0].totalSpent").value(355.0))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    private long placeOrder(User user, String slug) throws Exception {
        long id = productId(slug);
        if (productStock(id) < 1) {
            setProductStock(id, 10);
        }
        mvc.perform(postJson("/api/v1/cart/items", productItem(id, 1)).with(as(user))).andExpect(status().isOk());
        MvcResult result = mvc.perform(postJson("/api/v1/orders", checkout(newKey(), validDeliveryDate())).with(as(user)))
                .andExpect(status().isCreated()).andReturn();
        return read(result).get("id").asLong();
    }
}
