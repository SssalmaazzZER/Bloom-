package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_images")
@Getter
@Setter
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProductImage() {
    }

    public ProductImage(String url, String altText, int sortOrder) {
        this.url = url;
        this.altText = altText;
        this.sortOrder = sortOrder;
    }
}
