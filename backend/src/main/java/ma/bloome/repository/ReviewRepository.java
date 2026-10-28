package ma.bloome.repository;

import ma.bloome.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("select avg(r.rating), count(r) from Review r where r.product.id = :productId")
    List<Object[]> ratingSummary(@Param("productId") Long productId);

    @Query("select r.product.id, avg(r.rating), count(r) from Review r where r.product.id in :ids group by r.product.id")
    List<Object[]> ratingSummaries(@Param("ids") Collection<Long> productIds);
}
