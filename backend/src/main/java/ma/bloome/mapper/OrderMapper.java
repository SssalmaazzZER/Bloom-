package ma.bloome.mapper;

import ma.bloome.dto.CartDtos.BouquetStemLine;
import ma.bloome.dto.CartDtos.BouquetSummary;
import ma.bloome.dto.OrderDtos.*;
import ma.bloome.entity.BouquetSnapshot;
import ma.bloome.entity.CustomerOrder;
import ma.bloome.entity.OrderItem;
import ma.bloome.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class OrderMapper {

    public OrderSummary toSummary(CustomerOrder o) {
        String preview = o.getItems().stream().map(OrderItem::getImageUrl).filter(u -> u != null).findFirst().orElse(null);
        int count = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummary(o.getId(), o.getReference(), o.getStatus(), o.getCreatedAt(), o.getTotal(), count,
                o.getDeliveryDate(), o.getDeliverySlot(), preview, o.getCustomerName(), o.getCity());
    }

    /**
     * @param forAdmin when true, includes the statuses the order may move to next.
     */
    public OrderDetail toDetail(CustomerOrder o, boolean forAdmin) {
        List<OrderItemDto> items = o.getItems().stream().map(this::toItem).toList();
        List<StatusEvent> history = o.getHistory().stream()
                .map(h -> new StatusEvent(h.getToStatus(), h.getChangedAt(), h.getNote()))
                .toList();
        Set<OrderStatus> transitions = forAdmin ? o.getStatus().allowedTransitions() : EnumSet.noneOf(OrderStatus.class);
        return new OrderDetail(
                o.getId(), o.getReference(), o.getStatus(), o.getPaymentMethod().name(), o.getPaymentStatus().name(),
                o.getCreatedAt(), o.getCancelledAt(),
                o.getCustomerName(), o.getCustomerEmail(), o.getCustomerPhone(),
                o.getRecipientName(), o.getRecipientPhone(),
                o.getAddressLine1(), o.getAddressLine2(), o.getCity(), o.getPostalCode(),
                o.getDeliveryInstructions(), o.getGiftMessage(), o.isGiftWrapping(),
                o.getDeliveryDate(), o.getDeliverySlot(), o.getDeliverySlot().label(),
                items, o.getSubtotal(), o.getDeliveryFee(), o.getGiftWrappingFee(), o.getDiscount(), o.getTotal(),
                history, o.getStatus().isCustomerCancellable(), transitions);
    }

    private OrderItemDto toItem(OrderItem item) {
        String slug = item.getProduct() != null ? item.getProduct().getSlug() : null;
        Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
        return new OrderItemDto(item.getId(), item.getItemType().name(), productId, slug, item.getProductName(),
                item.getImageUrl(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal(),
                toBouquetSummary(item.getConfigurationSnapshot(), item.getUnitPrice()));
    }

    public BouquetSummary toBouquetSummary(BouquetSnapshot snapshot, BigDecimal unitPrice) {
        if (snapshot == null) {
            return null;
        }
        List<BouquetStemLine> stems = snapshot.stems().stream()
                .map(s -> new BouquetStemLine(s.stemId(), s.name(), s.color(), null, s.unitPrice(), s.quantity(),
                        s.unitPrice().multiply(BigDecimal.valueOf(s.quantity()))))
                .toList();
        int count = snapshot.stems().stream().mapToInt(BouquetSnapshot.StemLine::quantity).sum();
        return new BouquetSummary(null, snapshot.wrappingName(), null, snapshot.wrappingPrice(),
                null, snapshot.ribbonName(), null, snapshot.ribbonPrice(), snapshot.message(), stems, count, unitPrice);
    }
}
