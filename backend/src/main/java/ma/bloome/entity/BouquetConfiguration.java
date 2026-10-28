package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A customer's custom bouquet: one wrapping, one ribbon, a message and a set of stems. */
@Entity
@Table(name = "bouquet_configurations")
@Getter
@Setter
public class BouquetConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wrapping_id")
    private BouquetWrapping wrapping;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ribbon_id")
    private BouquetRibbon ribbon;

    private String message;

    @OneToMany(mappedBy = "configuration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BouquetConfigurationStem> stems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addStem(BouquetStem stem, int quantity) {
        BouquetConfigurationStem line = new BouquetConfigurationStem();
        line.setId(new BouquetConfigurationStem.Key(null, stem.getId()));
        line.setConfiguration(this);
        line.setStem(stem);
        line.setQuantity(quantity);
        stems.add(line);
    }
}
