package ma.bloome.service;

import ma.bloome.config.BloomeProperties;
import ma.bloome.dto.DeliveryDtos.DeliveryEstimate;
import ma.bloome.dto.DeliveryDtos.DeliveryOptions;
import ma.bloome.dto.DeliveryDtos.SlotDto;
import ma.bloome.entity.Enums.DeliverySlot;
import ma.bloome.exception.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Configurable delivery rules (bloome.shop.*): served cities, flat fee with a free-delivery
 * threshold, same-day cut-off hour, time slots and how far ahead a date may be booked.
 * All "today"/"now" computations use the shop's time zone (Africa/Casablanca).
 */
@Service
public class DeliveryService {

    /** Minimum preparation time, in hours, between ordering and the start of a same-day slot. */
    private static final int PREPARATION_HOURS = 2;

    private final BloomeProperties.Shop shop;
    private final Clock clock;

    public DeliveryService(BloomeProperties properties, Clock clock) {
        this.shop = properties.shop();
        this.clock = clock;
    }

    public DeliveryOptions options() {
        LocalDate earliest = earliestDate();
        List<SlotDto> slots = Arrays.stream(DeliverySlot.values())
                .map(s -> new SlotDto(s.name(), s.label(), s.startHour(), s.endHour()))
                .toList();
        List<String> earliestSlots = availableSlots(earliest).stream().map(Enum::name).toList();
        return new DeliveryOptions(shop.deliveryCities(), slots, earliest, earliestSlots, latestDate(),
                shop.deliveryFee(), shop.freeDeliveryThreshold(), shop.giftWrappingFee(),
                shop.sameDayCutoffHour(), shop.currency());
    }

    public DeliveryEstimate estimate() {
        LocalDate earliest = earliestDate();
        boolean sameDay = earliest.equals(today());
        String message = sameDay
                ? "Commandez avant " + shop.sameDayCutoffHour() + "h pour une livraison aujourd'hui."
                : "Livraison possible dès demain, partout à " + String.join(", ", shop.deliveryCities().subList(0,
                Math.min(3, shop.deliveryCities().size()))) + " et environs.";
        return new DeliveryEstimate(earliest, sameDay, message, shop.deliveryFee(), shop.freeDeliveryThreshold());
    }

    public BigDecimal deliveryFeeFor(BigDecimal subtotal) {
        if (subtotal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return subtotal.compareTo(shop.freeDeliveryThreshold()) >= 0 ? BigDecimal.ZERO.setScale(2) : shop.deliveryFee();
    }

    public BigDecimal giftWrappingFee() {
        return shop.giftWrappingFee();
    }

    public BigDecimal freeDeliveryThreshold() {
        return shop.freeDeliveryThreshold();
    }

    public String currency() {
        return shop.currency();
    }

    /** Returns the canonical (configured) spelling of the city, or throws if it is not served. */
    public String validate(String city, LocalDate date, DeliverySlot slot) {
        String canonicalCity = findCity(city).orElseThrow(() -> ApiException.badRequest("CITY_NOT_SERVED",
                "Nous livrons actuellement à : " + String.join(", ", shop.deliveryCities()) + "."));
        if (date.isBefore(earliestDate())) {
            throw ApiException.badRequest("DELIVERY_DATE_TOO_EARLY",
                    "Cette date de livraison n'est plus disponible. Choisissez une date à partir du " + earliestDate() + ".");
        }
        if (date.isAfter(latestDate())) {
            throw ApiException.badRequest("DELIVERY_DATE_TOO_LATE",
                    "Les livraisons se réservent au maximum " + shop.maxDaysAhead() + " jours à l'avance.");
        }
        if (!availableSlots(date).contains(slot)) {
            throw ApiException.badRequest("DELIVERY_SLOT_UNAVAILABLE",
                    "Ce créneau n'est plus disponible aujourd'hui. Choisissez un créneau plus tardif ou une autre date.");
        }
        return canonicalCity;
    }

    LocalDate today() {
        return LocalDate.now(clock.withZone(shop.zoneId()));
    }

    LocalDate earliestDate() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(shop.zoneId()));
        return now.getHour() < shop.sameDayCutoffHour() ? now.toLocalDate() : now.toLocalDate().plusDays(1);
    }

    LocalDate latestDate() {
        return today().plusDays(shop.maxDaysAhead());
    }

    List<DeliverySlot> availableSlots(LocalDate date) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(shop.zoneId()));
        if (!date.equals(now.toLocalDate())) {
            return List.of(DeliverySlot.values());
        }
        if (now.getHour() >= shop.sameDayCutoffHour()) {
            return List.of();
        }
        return Arrays.stream(DeliverySlot.values())
                .filter(s -> s.startHour() >= now.getHour() + PREPARATION_HOURS)
                .toList();
    }

    private Optional<String> findCity(String city) {
        if (city == null) {
            return Optional.empty();
        }
        String wanted = fold(city);
        return shop.deliveryCities().stream().filter(c -> fold(c).equals(wanted)).findFirst();
    }

    private static String fold(String value) {
        String noAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT);
    }
}
