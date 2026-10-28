package ma.bloome.service.admin;

import ma.bloome.dto.AdminDtos.*;
import ma.bloome.dto.OrderDtos.OrderSummary;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.OrderStatus;
import ma.bloome.entity.User;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.OrderMapper;
import ma.bloome.repository.BouquetStemRepository;
import ma.bloome.repository.CustomerOrderRepository;
import ma.bloome.repository.ProductRepository;
import ma.bloome.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    /** Revenue counts every order that has not been cancelled (cash on delivery included, even before collection). */
    public static final String REVENUE_DEFINITION =
            "Chiffre d'affaires = somme des totaux TTC (livraison incluse) de toutes les commandes non annulées. "
                    + "« Encaissé » = commandes livrées (paiement à la livraison collecté).";

    private static final Set<OrderStatus> REVENUE_STATUSES = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
    private static final int STEM_LOW_STOCK = 30;

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final BouquetStemRepository stemRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public AdminDashboardService(CustomerOrderRepository orderRepository, ProductRepository productRepository,
                                 BouquetStemRepository stemRepository, UserRepository userRepository,
                                 OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stemRepository = stemRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    public Dashboard dashboard() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, orderRepository.countByStatus(status));
        }
        List<LowStockItem> lowStock = new ArrayList<>();
        productRepository.findLowStock().forEach(p ->
                lowStock.add(new LowStockItem("PRODUCT", p.getId(), p.getName(), p.getStockQuantity(), p.getLowStockThreshold())));
        stemRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(s -> s.getStockQuantity() <= STEM_LOW_STOCK)
                .forEach(s -> lowStock.add(new LowStockItem("STEM", s.getId(), s.getName(), s.getStockQuantity(), STEM_LOW_STOCK)));

        List<BestsellerRow> bestsellers = orderRepository.bestsellingProducts(PageRequest.of(0, 5)).stream()
                .map(r -> new BestsellerRow((Long) r[0], (String) r[1], ((Number) r[2]).longValue(), (BigDecimal) r[3]))
                .toList();

        return new Dashboard(
                orderRepository.count(),
                orderRepository.sumTotalByStatusIn(REVENUE_STATUSES),
                orderRepository.sumTotalByStatusIn(EnumSet.of(OrderStatus.DELIVERED)),
                REVENUE_DEFINITION,
                byStatus.get(OrderStatus.PENDING),
                userRepository.countByRole(Role.CUSTOMER),
                productRepository.countByActiveTrue(),
                lowStock.size(),
                byStatus,
                lowStock,
                orderRepository.findTop8ByOrderByCreatedAtDesc().stream().map(orderMapper::toSummary).toList(),
                bestsellers);
    }

    /** Customer list with aggregated order statistics; never exposes credentials. */
    public PageResponse<CustomerSummary> customers(String q, int page, int size) {
        Page<User> users = userRepository.search(Role.CUSTOMER, q == null ? "" : q.trim().toLowerCase(Locale.ROOT),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Order.desc("createdAt"))));
        Map<Long, Object[]> stats = new HashMap<>();
        if (!users.isEmpty()) {
            for (Object[] row : orderRepository.statsByUsers(users.map(User::getId).toList())) {
                stats.put((Long) row[0], row);
            }
        }
        List<CustomerSummary> content = users.getContent().stream().map(u -> {
            Object[] s = stats.get(u.getId());
            long count = s == null ? 0 : ((Number) s[1]).longValue();
            BigDecimal spent = s == null ? BigDecimal.ZERO.setScale(2) : (BigDecimal) s[2];
            return toCustomer(u, count, spent);
        }).toList();
        return PageResponse.ofMapped(users, content);
    }

    public CustomerDetail customer(Long id) {
        User user = userRepository.findById(id).filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> ApiException.notFound("Client introuvable."));
        List<OrderSummary> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, 50))
                .map(orderMapper::toSummary).getContent();
        return new CustomerDetail(toCustomer(user, orderRepository.countByUserId(id), orderRepository.sumSpentByUser(id)), orders);
    }

    private CustomerSummary toCustomer(User u, long orderCount, BigDecimal spent) {
        return new CustomerSummary(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                u.getCreatedAt(), u.isEnabled(), orderCount, spent);
    }
}
