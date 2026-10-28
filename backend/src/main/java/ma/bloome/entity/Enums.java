package ma.bloome.entity;

/**
 * Holder for small, closed enumerations used by the domain model.
 * Each is persisted as its name (EnumType.STRING) and constrained by a CHECK in the schema.
 */
public final class Enums {
    private Enums() {
    }

    public enum Role { CUSTOMER, ADMIN }

    /** Only cash on delivery is supported today; card providers plug in via PaymentGateway. */
    public enum PaymentMethod { CASH_ON_DELIVERY }

    public enum PaymentStatus { UNPAID, PAID, REFUNDED, VOID }

    public enum DeliverySlot {
        MORNING(9, 12, "Matin (9h – 12h)"),
        AFTERNOON(13, 17, "Après-midi (13h – 17h)"),
        EVENING(17, 20, "Soirée (17h – 20h)");

        private final int startHour;
        private final int endHour;
        private final String label;

        DeliverySlot(int startHour, int endHour, String label) {
            this.startHour = startHour;
            this.endHour = endHour;
            this.label = label;
        }

        public int startHour() { return startHour; }
        public int endHour() { return endHour; }
        public String label() { return label; }
    }

    public enum FlowerColor { RED, PINK, WHITE, YELLOW, ORANGE, PURPLE, BLUE, GREEN, PASTEL, MIXED }

    public enum OrderItemType { PRODUCT, CUSTOM_BOUQUET }
}
