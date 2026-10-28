package ma.bloome.repository;

import ma.bloome.entity.CustomerOrder;
import ma.bloome.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    Page<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<CustomerOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    boolean existsByReference(String reference);

    long countByStatus(OrderStatus status);

    long countByUserId(Long userId);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.status in :statuses")
    BigDecimal sumTotalByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.user.id = :userId and o.status <> ma.bloome.entity.OrderStatus.CANCELLED")
    BigDecimal sumSpentByUser(@Param("userId") Long userId);

    @Query("""
            select o.user.id, count(o), coalesce(sum(case when o.status <> ma.bloome.entity.OrderStatus.CANCELLED then o.total else 0 end), 0)
            from CustomerOrder o where o.user.id in :userIds group by o.user.id
            """)
    List<Object[]> statsByUsers(@Param("userIds") Collection<Long> userIds);

    List<CustomerOrder> findTop8ByOrderByCreatedAtDesc();

    @Query("""
            select count(oi) > 0 from OrderItem oi
            where oi.order.user.id = :userId and oi.product.id = :productId
              and oi.order.status = ma.bloome.entity.OrderStatus.DELIVERED
            """)
    boolean hasDeliveredOrderWithProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("""
            select oi.product.id, oi.productName, sum(oi.quantity), sum(oi.lineTotal)
            from OrderItem oi
            where oi.product is not null and oi.order.status <> ma.bloome.entity.OrderStatus.CANCELLED
            group by oi.product.id, oi.productName
            order by sum(oi.quantity) desc
            """)
    List<Object[]> bestsellingProducts(Pageable pageable);
}
