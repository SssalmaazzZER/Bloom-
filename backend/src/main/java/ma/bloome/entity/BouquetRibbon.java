package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "bouquet_ribbons")
@Getter
@Setter
public class BouquetRibbon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "swatch_color", nullable = false)
    private String swatchColor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
