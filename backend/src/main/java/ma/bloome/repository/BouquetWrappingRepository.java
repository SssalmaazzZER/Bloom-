package ma.bloome.repository;

import ma.bloome.entity.BouquetWrapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BouquetWrappingRepository extends JpaRepository<BouquetWrapping, Long> {
    List<BouquetWrapping> findByActiveTrueOrderBySortOrderAsc();
}
