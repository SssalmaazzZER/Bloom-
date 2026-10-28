package ma.bloome.repository;

import ma.bloome.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("select p.category.id, count(p) from Product p where p.active = true group by p.category.id")
    List<Object[]> countActiveProductsByCategory();

    @Query("select count(p) > 0 from Product p where p.category.id = :categoryId")
    boolean hasProducts(Long categoryId);
}
