package ma.bloome.mapper;

import ma.bloome.dto.AdminDtos.AdminProductView;
import ma.bloome.dto.CatalogDtos.*;
import ma.bloome.dto.DeliveryDtos.DeliveryEstimate;
import ma.bloome.entity.Category;
import ma.bloome.entity.Occasion;
import ma.bloome.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogMapper {

    public record Rating(Double average, long count) {
        public static final Rating NONE = new Rating(null, 0);
    }

    public CategoryDto toCategory(Category c, long productCount) {
        return new CategoryDto(c.getId(), c.getSlug(), c.getName(), c.getDescription(), c.getImageUrl(),
                c.getSortOrder(), productCount);
    }

    public OccasionDto toOccasion(Occasion o) {
        return new OccasionDto(o.getId(), o.getSlug(), o.getName(), o.getDescription(), o.getImageUrl());
    }

    public ProductSummary toSummary(Product p, Rating rating) {
        return new ProductSummary(
                p.getId(), p.getSlug(), p.getName(), p.getShortDescription(), p.getPrice(), p.getCompareAtPrice(),
                p.getPrimaryImageUrl(), p.getCategory().getSlug(), p.getCategory().getName(),
                p.isPurchasable(), p.isLowStock(), p.isActive() ? Math.max(0, p.getMaxOrderableQuantity()) : 0,
                p.isFeatured(), p.isBestseller(), EnumSet.copyOf(withFallback(p)),
                rating.average(), rating.count(), p.getCreatedAt());
    }

    public ProductDetail toDetail(Product p, Rating rating, DeliveryEstimate estimate) {
        List<ImageDto> images = p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList();
        List<OccasionDto> occasions = p.getOccasions().stream()
                .sorted(Comparator.comparingInt(Occasion::getSortOrder))
                .map(this::toOccasion).toList();
        return new ProductDetail(
                p.getId(), p.getSlug(), p.getName(), p.getShortDescription(), p.getDescription(),
                p.getComposition(), p.getCareInstructions(), p.getPrice(), p.getCompareAtPrice(),
                toCategory(p.getCategory(), 0), images, EnumSet.copyOf(withFallback(p)), occasions,
                p.isPurchasable(), p.isLowStock(), p.isActive() ? Math.max(0, p.getMaxOrderableQuantity()) : 0,
                p.isFeatured(), p.isBestseller(), rating.average(), rating.count(), estimate);
    }

    public AdminProductView toAdmin(Product p) {
        return new AdminProductView(
                p.getId(), p.getSlug(), p.getName(), p.getShortDescription(), p.getDescription(),
                p.getComposition(), p.getCareInstructions(), p.getCategory().getId(), p.getCategory().getName(),
                p.getPrice(), p.getCompareAtPrice(), p.getStockQuantity(), p.getLowStockThreshold(),
                p.getMaxPerOrder(), p.isActive(), p.isFeatured(), p.isBestseller(), p.getSalesCount(),
                EnumSet.copyOf(withFallback(p)),
                p.getOccasions().stream().map(Occasion::getId).collect(Collectors.toSet()),
                p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList(),
                p.getStockQuantity() <= p.getLowStockThreshold(), p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private static java.util.Collection<ma.bloome.entity.Enums.FlowerColor> withFallback(Product p) {
        return p.getColors().isEmpty() ? EnumSet.noneOf(ma.bloome.entity.Enums.FlowerColor.class) : p.getColors();
    }
}
