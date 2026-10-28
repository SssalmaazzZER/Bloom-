package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.bloome.entity.Enums.DeliverySlot;
import ma.bloome.entity.Enums.PaymentMethod;
import ma.bloome.entity.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "delivery_instructions")
    private String deliveryInstructions;

    @Column(name = "gift_message")
    private String giftMessage;

    @Column(name = "gift_wrapping", nullable = false)
    private boolean giftWrapping;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_slot", nullable = false)
    private DeliverySlot deliverySlot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "gift_wrapping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal giftWrappingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void recordStatus(OrderStatus from, OrderStatus to, User changedBy, String note) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrder(this);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setChangedBy(changedBy);
        entry.setNote(note);
        entry.setChangedAt(Instant.now());
        history.add(entry);
    }
}
