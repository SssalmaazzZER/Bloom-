package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.bloome.entity.Enums.OrderItemType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Order line with name, price and configuration captured at order time. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private OrderItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bouquet_configuration_id")
    private BouquetConfiguration bouquetConfiguration;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_snapshot", columnDefinition = "jsonb")
    private BouquetSnapshot configurationSnapshot;
}
