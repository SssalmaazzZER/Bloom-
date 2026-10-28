package ma.bloome.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ma.bloome.entity.Enums.DeliverySlot;
import ma.bloome.entity.Enums.PaymentMethod;
import ma.bloome.entity.OrderStatus;
import ma.bloome.validation.PhoneNumber;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class OrderDtos {
    private OrderDtos() {
    }

    public record CheckoutRequest(
            @NotBlank(message = "Votre nom est requis.") @Size(max = 160) String customerName,
            @NotBlank(message = "L'e-mail est requis.") @Email(message = "Adresse e-mail invalide.") @Size(max = 255) String customerEmail,
            @NotBlank(message = "Le téléphone est requis.") @PhoneNumber @Size(max = 30) String customerPhone,
            @Size(max = 160) String recipientName,
            @PhoneNumber @Size(max = 30) String recipientPhone,
            @NotBlank(message = "L'adresse de livraison est requise.") @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @NotBlank(message = "La ville est requise.") @Size(max = 80) String city,
            @Pattern(regexp = "^$|^\\d{5}$", message = "Le code postal doit contenir 5 chiffres.") String postalCode,
            @Size(max = 500, message = "Les instructions ne peuvent pas dépasser 500 caractères.") String deliveryInstructions,
            @Size(max = 300, message = "Le message ne peut pas dépasser 300 caractères.") String giftMessage,
            boolean giftWrapping,
            @NotNull(message = "Choisissez une date de livraison.") LocalDate deliveryDate,
            @NotNull(message = "Choisissez un créneau de livraison.") DeliverySlot deliverySlot,
            @NotNull(message = "Choisissez un mode de paiement.") PaymentMethod paymentMethod,
            @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "^[A-Za-z0-9-]+$") String idempotencyKey,
            /* Total the customer saw; if the server total differs the order is refused with PRICE_CHANGED. */
            BigDecimal expectedTotal
    ) {
    }

    public record OrderItemDto(Long id, String type, Long productId, String productSlug, String name,
                               String imageUrl, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                               CartDtos.BouquetSummary bouquet) {
    }

    public record StatusEvent(OrderStatus status, Instant at, String note) {
    }

    public record OrderSummary(Long id, String reference, OrderStatus status, Instant createdAt,
                               BigDecimal total, int itemCount, LocalDate deliveryDate, DeliverySlot deliverySlot,
                               String previewImageUrl, String customerName, String city) {
    }

    public record OrderDetail(
            Long id,
            String reference,
            OrderStatus status,
            String paymentMethod,
            String paymentStatus,
            Instant createdAt,
            Instant cancelledAt,
            String customerName,
            String customerEmail,
            String customerPhone,
            String recipientName,
            String recipientPhone,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String deliveryInstructions,
            String giftMessage,
            boolean giftWrapping,
            LocalDate deliveryDate,
            DeliverySlot deliverySlot,
            String deliverySlotLabel,
            List<OrderItemDto> items,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal giftWrappingFee,
            BigDecimal discount,
            BigDecimal total,
            List<StatusEvent> history,
            boolean cancellable,
            Set<OrderStatus> allowedTransitions
    ) {
    }

    public record CancelRequest(@Size(max = 300) String reason) {
    }
}
