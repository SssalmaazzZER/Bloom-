package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.bloome.entity.Enums.FlowerColor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "short_description", nullable = false)
    private String shortDescription;

    @Column(nullable = false)
    private String description;

    private String composition;

    @Column(name = "care_instructions")
    private String careInstructions;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(name = "max_per_order", nullable = false)
    private int maxPerOrder = 10;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean bestseller;

    @Column(name = "sales_count", nullable = false)
    private int salesCount;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    private Set<FlowerColor> colors = EnumSet.noneOf(FlowerColor.class);

    @ManyToMany
    @JoinTable(name = "product_occasions",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "occasion_id"))
    @BatchSize(size = 50)
    private Set<Occasion> occasions = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity > 0 && stockQuantity <= lowStockThreshold;
    }

    public boolean isPurchasable() {
        return active && stockQuantity > 0;
    }

    public String getPrimaryImageUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }

    /** Largest quantity a customer may hold in a single order. */
    public int getMaxOrderableQuantity() {
        return Math.min(maxPerOrder, stockQuantity);
    }

    public void replaceImages(List<ProductImage> newImages) {
        images.clear();
        for (ProductImage image : newImages) {
            image.setProduct(this);
            images.add(image);
        }
    }
}
