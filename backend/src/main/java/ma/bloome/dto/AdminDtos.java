package ma.bloome.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ma.bloome.entity.Enums.FlowerColor;
import ma.bloome.entity.OrderStatus;
import ma.bloome.validation.SafeImageUrl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record ImageRequest(
            @NotBlank(message = "L'URL de l'image est requise.") @SafeImageUrl String url,
            @Size(max = 200) String altText
    ) {
    }

    public record ProductRequest(
            @NotBlank(message = "Le nom est requis.") @Size(max = 150) String name,
            @Size(max = 120) @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "Le slug ne peut contenir que des minuscules, chiffres et tirets.") String slug,
            @NotBlank(message = "La description courte est requise.") @Size(max = 300) String shortDescription,
            @NotBlank(message = "La description est requise.") @Size(max = 5000) String description,
            @Size(max = 1000) String composition,
            @Size(max = 1000) String careInstructions,
            @NotNull(message = "La catégorie est requise.") Long categoryId,
            @NotNull(message = "Le prix est requis.")
            @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif.")
            @DecimalMax(value = "99999.99", message = "Prix trop élevé.")
            @Digits(integer = 5, fraction = 2, message = "Prix invalide (2 décimales max).") BigDecimal price,
            @DecimalMin(value = "0.00") @DecimalMax(value = "99999.99") @Digits(integer = 5, fraction = 2) BigDecimal compareAtPrice,
            @Min(value = 0, message = "Le stock ne peut pas être négatif.") @Max(value = 100000) int stockQuantity,
            @Min(0) @Max(1000) int lowStockThreshold,
            @Min(value = 1, message = "La quantité maximale doit être d'au moins 1.") @Max(100) int maxPerOrder,
            boolean active,
            boolean featured,
            boolean bestseller,
            @NotNull Set<FlowerColor> colors,
            @NotNull Set<Long> occasionIds,
            @NotNull @Size(max = 10, message = "10 images maximum.") List<@Valid ImageRequest> images,
            /* Optimistic-locking token from the last read; null for creation. */
            Long version
    ) {
    }

    public record AdminProductView(
            Long id, String slug, String name, String shortDescription, String description,
            String composition, String careInstructions, Long categoryId, String categoryName,
            BigDecimal price, BigDecimal compareAtPrice, int stockQuantity, int lowStockThreshold,
            int maxPerOrder, boolean active, boolean featured, boolean bestseller, int salesCount,
            Set<FlowerColor> colors, Set<Long> occasionIds, List<CatalogDtos.ImageDto> images,
            boolean lowStock, long version, Instant createdAt, Instant updatedAt
    ) {
    }

    public record StockAdjustmentRequest(
            @NotNull(message = "Indiquez la variation de stock.") @Min(-100000) @Max(100000) Integer delta
    ) {
    }

    public record StatusUpdateRequest(
            @NotNull(message = "Le nouveau statut est requis.") OrderStatus status,
            @Size(max = 300) String note
    ) {
    }

    public record CategoryRequest(
            @NotBlank(message = "Le nom est requis.") @Size(max = 120) String name,
            @NotBlank(message = "Le slug est requis.") @Size(max = 80)
            @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug invalide.") String slug,
            @Size(max = 1000) String description,
            @SafeImageUrl String imageUrl,
            @Min(0) @Max(1000) int sortOrder
    ) {
    }

    public record LowStockItem(String kind, Long id, String name, int stockQuantity, int threshold) {
    }

    public record BestsellerRow(Long productId, String name, long quantitySold, BigDecimal revenue) {
    }

    public record Dashboard(
            long totalOrders,
            BigDecimal revenue,
            BigDecimal deliveredRevenue,
            String revenueDefinition,
            long pendingOrders,
            long totalCustomers,
            long activeProducts,
            long lowStockCount,
            Map<OrderStatus, Long> ordersByStatus,
            List<LowStockItem> lowStock,
            List<OrderDtos.OrderSummary> recentOrders,
            List<BestsellerRow> bestsellers
    ) {
    }

    public record CustomerSummary(Long id, String firstName, String lastName, String email, String phone,
                                  Instant createdAt, boolean enabled, long orderCount, BigDecimal totalSpent) {
    }

    public record CustomerDetail(CustomerSummary customer, List<OrderDtos.OrderSummary> orders) {
    }

    public record StemInventory(Long id, String name, String color, BigDecimal unitPrice, int stockQuantity,
                                boolean active, boolean lowStock) {
    }

    public record Inventory(List<AdminProductView> products, List<StemInventory> stems) {
    }
}
