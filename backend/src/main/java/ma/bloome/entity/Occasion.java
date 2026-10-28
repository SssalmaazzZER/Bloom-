package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "occasions")
@Getter
@Setter
public class Occasion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
