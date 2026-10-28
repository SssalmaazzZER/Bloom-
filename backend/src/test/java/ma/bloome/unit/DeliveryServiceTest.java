package ma.bloome.unit;

import ma.bloome.config.BloomeProperties;
import ma.bloome.entity.Enums.DeliverySlot;
import ma.bloome.exception.ApiException;
import ma.bloome.service.DeliveryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryServiceTest {

    private static final ZoneId CASA = ZoneId.of("Africa/Casablanca");

    private DeliveryService at(int hour) {
        Instant now = ZonedDateTime.of(LocalDate.of(2026, 10, 27), LocalTime.of(hour, 0), CASA).toInstant();
        BloomeProperties.Shop shop = new BloomeProperties.Shop("Africa/Casablanca", "MAD", new BigDecimal("35.00"),
                new BigDecimal("500.00"), new BigDecimal("25.00"), 14, 30, List.of("Rabat", "Salé", "Kénitra"));
        BloomeProperties props = new BloomeProperties(null, null, null, shop, null);
        return new DeliveryService(props, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void sameDayBeforeCutoffOnlyLaterSlots() {
        DeliveryService service = at(10);
        assertThat(service.options().earliestDate()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(service.options().earliestDateSlots()).containsExactly("AFTERNOON", "EVENING");
        assertThatThrownBy(() -> service.validate("Rabat", LocalDate.of(2026, 10, 27), DeliverySlot.MORNING))
                .isInstanceOf(ApiException.class).hasMessageContaining("créneau");
    }

    @Test
    void afterCutoffNextDay() {
        DeliveryService service = at(15);
        assertThat(service.options().earliestDate()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThatThrownBy(() -> service.validate("Rabat", LocalDate.of(2026, 10, 27), DeliverySlot.EVENING))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cityMatchingIgnoresCaseAndAccents() {
        DeliveryService service = at(9);
        assertThat(service.validate("  kenitra ", LocalDate.of(2026, 10, 29), DeliverySlot.MORNING)).isEqualTo("Kénitra");
        assertThatThrownBy(() -> service.validate("Marrakech", LocalDate.of(2026, 10, 29), DeliverySlot.MORNING))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void tooFarAhead() {
        assertThatThrownBy(() -> at(9).validate("Rabat", LocalDate.of(2026, 12, 31), DeliverySlot.MORNING))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void freeDeliveryThreshold() {
        DeliveryService service = at(9);
        assertThat(service.deliveryFeeFor(new BigDecimal("499.99"))).isEqualByComparingTo("35.00");
        assertThat(service.deliveryFeeFor(new BigDecimal("500.00"))).isEqualByComparingTo("0.00");
        assertThat(service.deliveryFeeFor(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }
}
