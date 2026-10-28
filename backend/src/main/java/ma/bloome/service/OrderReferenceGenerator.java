package ma.bloome.service;

import ma.bloome.config.BloomeProperties;
import ma.bloome.repository.CustomerOrderRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Human-friendly, non-sequential order references such as {@code BLM-261027-K7Q4X}.
 * Random suffix (no ambiguous characters) avoids leaking order volumes; uniqueness is
 * checked here and guaranteed by a UNIQUE constraint in the database.
 */
@Component
public class OrderReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final SecureRandom random = new SecureRandom();
    private final CustomerOrderRepository orderRepository;
    private final Clock clock;
    private final BloomeProperties properties;

    public OrderReferenceGenerator(CustomerOrderRepository orderRepository, Clock clock, BloomeProperties properties) {
        this.orderRepository = orderRepository;
        this.clock = clock;
        this.properties = properties;
    }

    public String next() {
        String prefix = "BLM-" + LocalDate.now(clock.withZone(properties.shop().zoneId())).format(DATE) + "-";
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 5; i++) {
                sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String reference = sb.toString();
            if (!orderRepository.existsByReference(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("Could not generate a unique order reference");
    }
}
