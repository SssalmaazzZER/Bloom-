package ma.bloome.dto;

import ma.bloome.entity.Enums.FlowerColor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record CategoryDto(Long id, String slug, String name, String description, String imageUrl,
                              int sortOrder, long productCount) {
    }

    public record OccasionDto(Long id, String slug, String name, String description, String imageUrl) {
    }

    public record ImageDto(String url, String altText) {
    }

    public record ProductSummary(
            Long id,
            String slug,
            String name,
            String shortDescription,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String imageUrl,
            String categorySlug,
            String categoryName,
            boolean inStock,
            boolean lowStock,
            int availableQuantity,
            boolean featured,
            boolean bestseller,
            Set<FlowerColor> colors,
            Double ratingAverage,
            long ratingCount,
            Instant createdAt
    ) {
    }

    public record ProductDetail(
            Long id,
            String slug,
            String name,
            String shortDescription,
            String description,
            String composition,
            String careInstructions,
            BigDecimal price,
            BigDecimal compareAtPrice,
            CategoryDto category,
            List<ImageDto> images,
            Set<FlowerColor> colors,
            List<OccasionDto> occasions,
            boolean inStock,
            boolean lowStock,
            int availableQuantity,
            boolean featured,
            boolean bestseller,
            Double ratingAverage,
            long ratingCount,
            DeliveryDtos.DeliveryEstimate deliveryEstimate
    ) {
    }

    /** Values that help the catalog UI build its filter controls. */
    public record CatalogFacets(BigDecimal minPrice, BigDecimal maxPrice, List<FlowerColor> colors,
                                List<CategoryDto> categories, List<OccasionDto> occasions) {
    }

    public enum ProductSort {
        POPULARITY, NEWEST, PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC
    }

    /** Catalog query; every field is optional. */
    public record ProductFilter(
            String q,
            List<String> categories,
            List<String> occasions,
            List<FlowerColor> colors,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            ProductSort sort
    ) {
    }
}
