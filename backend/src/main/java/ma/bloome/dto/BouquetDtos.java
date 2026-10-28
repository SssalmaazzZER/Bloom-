package ma.bloome.dto;

import java.math.BigDecimal;
import java.util.List;

public final class BouquetDtos {
    private BouquetDtos() {
    }

    public record StemOption(Long id, String name, String color, String description, String imageUrl,
                             BigDecimal unitPrice, int maxPerBouquet, int available) {
    }

    public record WrappingOption(Long id, String name, String description, String swatchColor, BigDecimal price) {
    }

    public record RibbonOption(Long id, String name, String swatchColor, BigDecimal price) {
    }

    public record BouquetOptions(List<StemOption> stems, List<WrappingOption> wrappings,
                                 List<RibbonOption> ribbons, int minStems, int maxStems, String currency) {
    }
}
