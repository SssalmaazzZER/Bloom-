package ma.bloome.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DeliveryDtos {
    private DeliveryDtos() {
    }

    public record SlotDto(String code, String label, int startHour, int endHour) {
    }

    public record DeliveryOptions(
            List<String> cities,
            List<SlotDto> slots,
            LocalDate earliestDate,
            List<String> earliestDateSlots,
            LocalDate latestDate,
            BigDecimal deliveryFee,
            BigDecimal freeDeliveryThreshold,
            BigDecimal giftWrappingFee,
            int sameDayCutoffHour,
            String currency
    ) {
    }

    /** Short, human-readable delivery promise shown on product pages. */
    public record DeliveryEstimate(LocalDate earliestDate, boolean sameDay, String message,
                                   BigDecimal deliveryFee, BigDecimal freeDeliveryThreshold) {
    }
}
