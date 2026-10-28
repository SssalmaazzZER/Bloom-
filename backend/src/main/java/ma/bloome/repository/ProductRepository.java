package ma.bloome.repository;

import ma.bloome.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findWithCategoryById(Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByActiveTrueAndFeaturedTrueOrderBySalesCountDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByActiveTrueAndBestsellerTrueOrderBySalesCountDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            select p from Product p
            where p.active = true and p.id <> :productId and p.category.id = :categoryId
            order by p.salesCount desc
            """)
    List<Product> findRelated(@Param("productId") Long productId, @Param("categoryId") Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("select p from Product p where p.active = true and p.stockQuantity <= p.lowStockThreshold order by p.stockQuantity asc, p.name asc")
    List<Product> findLowStock();

    long countByActiveTrue();

    @Query("select count(p) from Product p where p.active = true and p.stockQuantity <= p.lowStockThreshold")
    long countLowStock();

    /**
     * Atomically reserves stock. The WHERE clause guarantees stock never goes negative, and the
     * row lock taken by UPDATE serialises concurrent checkouts on the same product.
     * Returns 1 on success, 0 when there is not enough stock (or the product is inactive).
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update versioned Product p
            set p.stockQuantity = p.stockQuantity - :quantity, p.salesCount = p.salesCount + :quantity
            where p.id = :id and p.active = true and p.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update versioned Product p
            set p.stockQuantity = p.stockQuantity + :quantity,
                p.salesCount = case when p.salesCount >= :quantity then p.salesCount - :quantity else 0 end
            where p.id = :id
            """)
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** Admin stock adjustment by a signed delta; refuses to go below zero. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update versioned Product p
            set p.stockQuantity = p.stockQuantity + :delta
            where p.id = :id and p.stockQuantity + :delta >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    @Query("select p.stockQuantity from Product p where p.id = :id")
    Integer findStockById(@Param("id") Long id);

    @Query("select min(p.price) from Product p where p.active = true")
    java.math.BigDecimal findMinActivePrice();

    @Query("select max(p.price) from Product p where p.active = true")
    java.math.BigDecimal findMaxActivePrice();
}
