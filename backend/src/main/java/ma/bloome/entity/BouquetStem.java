package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.bloome.entity.Enums.FlowerColor;

import java.math.BigDecimal;

/** A single flower variety that customers can add, stem by stem, to a custom bouquet. */
@Entity
@Table(name = "bouquet_stems")
@Getter
@Setter
public class BouquetStem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowerColor color;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "max_per_bouquet", nullable = false)
    private int maxPerBouquet;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
