package ma.bloome.repository;

import ma.bloome.entity.BouquetStem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BouquetStemRepository extends JpaRepository<BouquetStem, Long> {

    List<BouquetStem> findByActiveTrueOrderBySortOrderAsc();

    List<BouquetStem> findAllByOrderBySortOrderAsc();

    @Query("select s.stockQuantity from BouquetStem s where s.id = :id")
    Integer findStockById(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
            update BouquetStem s set s.stockQuantity = s.stockQuantity - :quantity
            where s.id = :id and s.active = true and s.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update BouquetStem s set s.stockQuantity = s.stockQuantity + :quantity where s.id = :id")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update BouquetStem s set s.stockQuantity = s.stockQuantity + :delta where s.id = :id and s.stockQuantity + :delta >= 0")
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
