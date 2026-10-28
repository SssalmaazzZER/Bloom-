package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A cart line: either a catalog product or a custom bouquet configuration (never both). */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "bouquet_configuration_id")
    private BouquetConfiguration bouquetConfiguration;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isCustomBouquet() {
        return bouquetConfiguration != null;
    }
}
