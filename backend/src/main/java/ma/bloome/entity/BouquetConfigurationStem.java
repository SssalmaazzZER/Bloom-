package ma.bloome.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "bouquet_configuration_stems")
@Getter
@Setter
public class BouquetConfigurationStem {

    @EmbeddedId
    private Key id;

    @MapsId("configurationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id")
    private BouquetConfiguration configuration;

    @MapsId("stemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stem_id")
    private BouquetStem stem;

    @Column(nullable = false)
    private int quantity;

    @Embeddable
    @Getter
    @Setter
    public static class Key implements Serializable {
        @Column(name = "configuration_id")
        private Long configurationId;

        @Column(name = "stem_id")
        private Long stemId;

        public Key() {
        }

        public Key(Long configurationId, Long stemId) {
            this.configurationId = configurationId;
            this.stemId = stemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(configurationId, key.configurationId) && Objects.equals(stemId, key.stemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(configurationId, stemId);
        }
    }
}
