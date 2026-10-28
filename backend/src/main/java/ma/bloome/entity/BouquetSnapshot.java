package ma.bloome.entity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable, order-time copy of a custom bouquet, stored as JSONB on the order item so
 * historical orders stay accurate even if stems, wrappings or prices change later.
 */
public record BouquetSnapshot(
        String wrappingName,
        BigDecimal wrappingPrice,
        String ribbonName,
        BigDecimal ribbonPrice,
        String message,
        List<StemLine> stems
) {
    public record StemLine(Long stemId, String name, String color, BigDecimal unitPrice, int quantity) {
    }
}
