package ma.bloome.repository;

import ma.bloome.entity.BouquetRibbon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BouquetRibbonRepository extends JpaRepository<BouquetRibbon, Long> {
    List<BouquetRibbon> findByActiveTrueOrderBySortOrderAsc();
}
