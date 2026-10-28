package ma.bloome.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle. Allowed transitions are the single source of truth for
 * both customer cancellation and administrator status updates.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(PREPARING, CANCELLED),
            PREPARING, EnumSet.of(OUT_FOR_DELIVERY, CANCELLED),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    /** Statuses from which the customer may still cancel on their own. */
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE = EnumSet.of(PENDING, CONFIRMED);

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public Set<OrderStatus> allowedTransitions() {
        Set<OrderStatus> allowed = TRANSITIONS.get(this);
        return allowed.isEmpty() ? EnumSet.noneOf(OrderStatus.class) : EnumSet.copyOf(allowed);
    }

    public boolean isCustomerCancellable() {
        return CUSTOMER_CANCELLABLE.contains(this);
    }
}
