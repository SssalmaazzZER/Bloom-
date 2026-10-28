package ma.bloome.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() {
    }

    public record StemSelection(
            @NotNull Long stemId,
            @Min(1) @Max(50) int quantity
    ) {
    }

    public record BouquetRequest(
            @NotNull(message = "Choisissez un emballage.") Long wrappingId,
            @NotNull(message = "Choisissez un ruban.") Long ribbonId,
            @Size(max = 300, message = "Le message ne peut pas dépasser 300 caractères.") String message,
            @NotEmpty(message = "Ajoutez au moins une fleur.") @Size(max = 20) List<@Valid StemSelection> stems
    ) {
    }

    /** Either {@code productId} or {@code bouquet} must be provided, not both. */
    public record CartItemRequest(
            Long productId,
            @Valid BouquetRequest bouquet,
            @Min(value = 1, message = "La quantité doit être d'au moins 1.")
            @Max(value = 100, message = "Quantité trop élevée.") int quantity,
            /* Opaque identifier used by anonymous (local) carts to match quote lines. */
            @Size(max = 64) String clientKey
    ) {
        @AssertTrue(message = "Indiquez soit un produit, soit un bouquet personnalisé.")
        public boolean isExactlyOneKind() {
            return (productId == null) != (bouquet == null);
        }
    }

    public record UpdateCartItemRequest(
            @Min(value = 1, message = "La quantité doit être d'au moins 1.")
            @Max(value = 100, message = "Quantité trop élevée.") int quantity
    ) {
    }

    public record CartItemsRequest(
            @NotNull @Size(max = 50, message = "Trop d'articles.") List<@Valid CartItemRequest> items
    ) {
    }

    public record BouquetStemLine(Long stemId, String name, String color, String imageUrl,
                                  BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record BouquetSummary(
            Long wrappingId, String wrappingName, String wrappingColor, BigDecimal wrappingPrice,
            Long ribbonId, String ribbonName, String ribbonColor, BigDecimal ribbonPrice,
            String message, List<BouquetStemLine> stems, int stemCount, BigDecimal unitPrice
    ) {
    }

    public enum LineIssue { UNAVAILABLE, OUT_OF_STOCK, QUANTITY_EXCEEDS_STOCK }

    public record CartLine(
            String id,
            String type,
            Long productId,
            String slug,
            String name,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            int maxQuantity,
            LineIssue issue,
            BouquetSummary bouquet
    ) {
    }

    public record CartView(
            List<CartLine> items,
            int itemCount,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal giftWrappingFee,
            BigDecimal discount,
            BigDecimal total,
            BigDecimal freeDeliveryThreshold,
            BigDecimal amountToFreeDelivery,
            boolean checkoutReady,
            String currency
    ) {
    }
}
