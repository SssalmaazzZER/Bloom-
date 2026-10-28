package ma.bloome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

/** Typed view of the {@code bloome.*} configuration tree (see application.yml). */
@ConfigurationProperties(prefix = "bloome")
public record BloomeProperties(
        Cors cors,
        Bootstrap bootstrap,
        RateLimit rateLimit,
        Shop shop,
        Bouquet bouquet
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Bootstrap(String adminEmail, String adminPassword, boolean demoAccounts) {
    }

    public record RateLimit(int authRequestsPerMinute) {
    }

    public record Shop(
            String timeZone,
            String currency,
            BigDecimal deliveryFee,
            BigDecimal freeDeliveryThreshold,
            BigDecimal giftWrappingFee,
            int sameDayCutoffHour,
            int maxDaysAhead,
            List<String> deliveryCities
    ) {
        public ZoneId zoneId() {
            return ZoneId.of(timeZone);
        }
    }

    public record Bouquet(int minStems, int maxStems) {
    }
}
