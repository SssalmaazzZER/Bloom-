package ma.bloome.unit;

import ma.bloome.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static ma.bloome.entity.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void forwardLifecycle() {
        assertThat(PENDING.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(PREPARING)).isTrue();
        assertThat(PREPARING.canTransitionTo(OUT_FOR_DELIVERY)).isTrue();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(DELIVERED)).isTrue();
    }

    @Test
    void noSkippingOrGoingBack() {
        assertThat(PENDING.canTransitionTo(DELIVERED)).isFalse();
        assertThat(PREPARING.canTransitionTo(CONFIRMED)).isFalse();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(CANCELLED)).isFalse();
        assertThat(PENDING.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    void terminalStates() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(DELIVERED.canTransitionTo(s)).isFalse();
            assertThat(CANCELLED.canTransitionTo(s)).isFalse();
        }
    }

    @Test
    void customerCancellationWindow() {
        assertThat(PENDING.isCustomerCancellable()).isTrue();
        assertThat(CONFIRMED.isCustomerCancellable()).isTrue();
        assertThat(PREPARING.isCustomerCancellable()).isFalse();
        assertThat(DELIVERED.isCustomerCancellable()).isFalse();
    }
}
