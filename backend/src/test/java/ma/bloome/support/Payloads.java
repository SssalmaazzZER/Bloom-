package ma.bloome.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Request bodies built as plain maps, exactly as a browser would send them. */
public final class Payloads {
    private Payloads() {
    }

    public static Map<String, Object> productItem(long productId, int quantity) {
        Map<String, Object> m = new HashMap<>();
        m.put("productId", productId);
        m.put("quantity", quantity);
        return m;
    }

    public static Map<String, Object> bouquetItem(long wrappingId, long ribbonId, List<Map<String, Object>> stems, int quantity) {
        Map<String, Object> bouquet = new HashMap<>();
        bouquet.put("wrappingId", wrappingId);
        bouquet.put("ribbonId", ribbonId);
        bouquet.put("message", "Avec tout mon amour");
        bouquet.put("stems", stems);
        Map<String, Object> m = new HashMap<>();
        m.put("bouquet", bouquet);
        m.put("quantity", quantity);
        return m;
    }

    public static Map<String, Object> stem(long stemId, int quantity) {
        return Map.of("stemId", stemId, "quantity", quantity);
    }

    public static Map<String, Object> checkout(String key, LocalDate date) {
        Map<String, Object> m = new HashMap<>();
        m.put("customerName", "Yasmine Alaoui");
        m.put("customerEmail", "yasmine@example.com");
        m.put("customerPhone", "06 12 34 56 78");
        m.put("recipientName", "Nadia Bennani");
        m.put("recipientPhone", "0661234567");
        m.put("addressLine1", "12 rue des Orangers");
        m.put("city", "Rabat");
        m.put("postalCode", "10000");
        m.put("deliveryInstructions", "Sonner deux fois");
        m.put("giftMessage", "Joyeux anniversaire !");
        m.put("giftWrapping", false);
        m.put("deliveryDate", date.toString());
        m.put("deliverySlot", "AFTERNOON");
        m.put("paymentMethod", "CASH_ON_DELIVERY");
        m.put("idempotencyKey", key);
        return m;
    }

    public static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> copy = new HashMap<>(base);
        copy.put(key, value);
        return copy;
    }

    public static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
