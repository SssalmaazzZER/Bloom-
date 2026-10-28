package ma.bloome.service;

import jakarta.persistence.criteria.Predicate;
import ma.bloome.dto.OrderDtos.CheckoutRequest;
import ma.bloome.dto.OrderDtos.OrderDetail;
import ma.bloome.dto.OrderDtos.OrderSummary;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.*;
import ma.bloome.entity.Enums.OrderItemType;
import ma.bloome.entity.Enums.PaymentStatus;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.OrderMapper;
import ma.bloome.repository.CartRepository;
import ma.bloome.repository.CustomerOrderRepository;
import ma.bloome.repository.UserRepository;
import ma.bloome.service.BouquetService.ResolvedBouquet;
import ma.bloome.service.CartPricingService.PricedCart;
import ma.bloome.service.CartPricingService.PricedLine;
import ma.bloome.service.payment.PaymentGateway;
import ma.bloome.validation.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustomerOrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final CartPricingService pricingService;
    private final BouquetService bouquetService;
    private final DeliveryService deliveryService;
    private final InventoryService inventoryService;
    private final OrderReferenceGenerator referenceGenerator;
    private final OrderMapper mapper;
    private final Map<Enums.PaymentMethod, PaymentGateway> gateways;
    private final ZoneId shopZone;

    public OrderService(CustomerOrderRepository orderRepository, CartRepository cartRepository,
                        UserRepository userRepository, CartService cartService,
                        CartPricingService pricingService, BouquetService bouquetService,
                        DeliveryService deliveryService, InventoryService inventoryService,
                        OrderReferenceGenerator referenceGenerator, OrderMapper mapper,
                        List<PaymentGateway> gateways, ma.bloome.config.BloomeProperties properties) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.pricingService = pricingService;
        this.bouquetService = bouquetService;
        this.deliveryService = deliveryService;
        this.inventoryService = inventoryService;
        this.referenceGenerator = referenceGenerator;
        this.mapper = mapper;
        this.gateways = gateways.stream().collect(Collectors.toMap(PaymentGateway::method, Function.identity()));
        this.shopZone = properties.shop().zoneId();
    }

    public record CheckoutResult(OrderDetail order, boolean created) {
    }

    /**
     * Places an order from the customer's server-side cart in a single transaction:
     * lock cart → re-price → validate delivery → atomically reserve stock → persist order
     * with price snapshots → empty cart. Any failure rolls everything back and leaves the
     * cart untouched. Re-submitting the same idempotency key returns the original order.
     */
    public CheckoutResult checkout(Long userId, CheckoutRequest request) {
        Optional<CustomerOrder> previous = orderRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
        if (previous.isPresent()) {
            return new CheckoutResult(mapper.toDetail(previous.get(), false), false);
        }

        // Serialises concurrent checkouts of the same cart (double clicks, several tabs).
        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> ApiException.badRequest("CART_EMPTY", "Votre panier est vide."));
        previous = orderRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
        if (previous.isPresent()) {
            return new CheckoutResult(mapper.toDetail(previous.get(), false), false);
        }
        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("CART_EMPTY", "Votre panier est vide.");
        }

        PricedCart priced = cartService.price(cart, request.giftWrapping());
        if (priced.hasIssues()) {
            throw ApiException.conflict("CART_NEEDS_REVIEW",
                    "Certains articles de votre panier ont changé (stock ou disponibilité). Vérifiez votre panier avant de valider.");
        }
        if (request.expectedTotal() != null && request.expectedTotal().compareTo(priced.total()) != 0) {
            throw ApiException.conflict("PRICE_CHANGED",
                    "Le montant de votre commande a été mis à jour (" + priced.total() + " MAD). Vérifiez le récapitulatif puis validez à nouveau.");
        }
        String city = deliveryService.validate(request.city(), request.deliveryDate(), request.deliverySlot());
        PaymentGateway gateway = Optional.ofNullable(gateways.get(request.paymentMethod()))
                .orElseThrow(() -> ApiException.badRequest("PAYMENT_METHOD_UNAVAILABLE", "Ce mode de paiement n'est pas disponible."));

        // Reserve stock atomically (throws InsufficientStockException → full rollback).
        Map<Long, Integer> productQty = new HashMap<>();
        Map<Long, Integer> stemQty = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (PricedLine line : priced.lines()) {
            var in = line.input();
            if (in.product() != null) {
                productQty.merge(in.product().getId(), in.quantity(), Integer::sum);
                names.put("p:" + in.product().getId(), in.product().getName());
            } else if (in.bouquet() != null) {
                for (BouquetService.StemQuantity s : in.bouquet().stems()) {
                    stemQty.merge(s.stem().getId(), s.quantity() * in.quantity(), Integer::sum);
                    names.put("s:" + s.stem().getId(), s.stem().getName());
                }
            }
        }
        inventoryService.reserve(productQty, stemQty, names);

        User user = userRepository.getReferenceById(userId);
        CustomerOrder order = new CustomerOrder();
        order.setReference(referenceGenerator.next());
        order.setUser(user);
        order.setIdempotencyKey(request.idempotencyKey());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.paymentMethod());
        order.setCustomerName(request.customerName().trim());
        order.setCustomerEmail(request.customerEmail().trim().toLowerCase(Locale.ROOT));
        order.setCustomerPhone(PhoneNumber.Validator.normalize(request.customerPhone()));
        order.setRecipientName(blankToNull(request.recipientName()) != null ? request.recipientName().trim() : order.getCustomerName());
        order.setRecipientPhone(blankToNull(request.recipientPhone()) != null
                ? PhoneNumber.Validator.normalize(request.recipientPhone()) : order.getCustomerPhone());
        order.setAddressLine1(request.addressLine1().trim());
        order.setAddressLine2(blankToNull(request.addressLine2()));
        order.setCity(city);
        order.setPostalCode(blankToNull(request.postalCode()));
        order.setDeliveryInstructions(blankToNull(request.deliveryInstructions()));
        order.setGiftMessage(blankToNull(request.giftMessage()));
        order.setGiftWrapping(request.giftWrapping());
        order.setDeliveryDate(request.deliveryDate());
        order.setDeliverySlot(request.deliverySlot());
        order.setSubtotal(priced.subtotal());
        order.setDeliveryFee(priced.deliveryFee());
        order.setGiftWrappingFee(priced.giftWrappingFee());
        order.setDiscount(priced.discount());
        order.setTotal(priced.total());

        Map<String, CartItem> cartItemsById = cart.getItems().stream()
                .collect(Collectors.toMap(i -> String.valueOf(i.getId()), Function.identity()));
        for (PricedLine line : priced.lines()) {
            OrderItem item = toOrderItem(line);
            CartItem source = cartItemsById.get(line.input().id());
            if (source != null && source.getBouquetConfiguration() != null) {
                item.setBouquetConfiguration(source.getBouquetConfiguration());
            }
            order.addItem(item);
        }
        order.setPaymentStatus(gateway.initiate(order).status());
        order.recordStatus(null, OrderStatus.PENDING, user, "Commande reçue");
        order = orderRepository.save(order);

        cart.getItems().clear();
        cart.touch();

        log.info("Order {} placed by user {} — total {} MAD", order.getReference(), userId, order.getTotal());
        return new CheckoutResult(mapper.toDetail(order, false), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(Long userId, int page, int size) {
        Page<CustomerOrder> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponse.of(orders, mapper::toSummary);
    }

    /** Ownership is part of the query: another customer's order is indistinguishable from a missing one. */
    @Transactional(readOnly = true)
    public OrderDetail myOrder(Long userId, Long orderId) {
        return mapper.toDetail(findOwned(userId, orderId), false);
    }

    public OrderDetail cancelByCustomer(Long userId, Long orderId, String reason) {
        CustomerOrder order = findOwned(userId, orderId);
        if (!order.getStatus().isCustomerCancellable()) {
            throw ApiException.conflict("CANCELLATION_NOT_ALLOWED",
                    "Cette commande est déjà en préparation et ne peut plus être annulée en ligne. Contactez-nous.");
        }
        applyTransition(order, OrderStatus.CANCELLED, userRepository.getReferenceById(userId),
                reason == null || reason.isBlank() ? "Annulée par le client" : "Annulée par le client : " + reason.trim());
        return mapper.toDetail(order, false);
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> search(OrderStatus status, String q, LocalDate from, LocalDate to, int page, int size) {
        Specification<CustomerOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + ProductSpecifications.escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerEmail")), pattern, '\\'),
                        cb.like(cb.lower(root.get("recipientName")), pattern, '\\')));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(shopZone).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(shopZone).toInstant()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<CustomerOrder> orders = orderRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(orders, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderDetail adminGet(Long orderId) {
        return mapper.toDetail(find(orderId), true);
    }

    public OrderDetail adminUpdateStatus(Long orderId, OrderStatus target, String note, Long adminId) {
        CustomerOrder order = find(orderId);
        if (!order.getStatus().canTransitionTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Transition impossible : " + order.getStatus() + " → " + target + ".");
        }
        applyTransition(order, target, userRepository.getReferenceById(adminId), note);
        return mapper.toDetail(order, true);
    }

    private void applyTransition(CustomerOrder order, OrderStatus target, User actor, String note) {
        OrderStatus from = order.getStatus();
        if (!from.canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Transition impossible : " + from + " → " + target + ".");
        }
        order.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            inventoryService.release(order);
            order.setCancelledAt(Instant.now());
            order.setPaymentStatus(order.getPaymentStatus() == PaymentStatus.PAID ? PaymentStatus.REFUNDED : PaymentStatus.VOID);
        }
        if (target == OrderStatus.DELIVERED && order.getPaymentMethod() == Enums.PaymentMethod.CASH_ON_DELIVERY) {
            // Cash is collected by the courier on hand-over.
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        order.recordStatus(from, target, actor, blankToNull(note));
        orderRepository.saveAndFlush(order);
    }

    private OrderItem toOrderItem(PricedLine line) {
        var in = line.input();
        OrderItem item = new OrderItem();
        item.setQuantity(in.quantity());
        item.setUnitPrice(line.unitPrice());
        item.setLineTotal(line.lineTotal());
        if (in.product() != null) {
            item.setItemType(OrderItemType.PRODUCT);
            item.setProduct(in.product());
            item.setProductName(in.product().getName());
            item.setImageUrl(in.product().getPrimaryImageUrl());
        } else {
            ResolvedBouquet bouquet = in.bouquet();
            item.setItemType(OrderItemType.CUSTOM_BOUQUET);
            item.setProductName("Bouquet personnalisé");
            item.setImageUrl(bouquet.stems().isEmpty() ? null : bouquet.stems().get(0).stem().getImageUrl());
            item.setConfigurationSnapshot(bouquetService.snapshot(bouquet));
        }
        return item;
    }

    private CustomerOrder findOwned(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Commande introuvable."));
    }

    private CustomerOrder find(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Commande introuvable."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
