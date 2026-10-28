package ma.bloome.service;

import ma.bloome.dto.CartDtos.CartLine;
import ma.bloome.dto.CartDtos.CartView;
import ma.bloome.dto.CartDtos.LineIssue;
import ma.bloome.entity.Product;
import ma.bloome.service.BouquetService.ResolvedBouquet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for cart arithmetic. Used for the persisted cart, anonymous cart
 * quotes and checkout, so the customer always sees exactly what will be charged.
 */
@Service
public class CartPricingService {

    private final DeliveryService deliveryService;
    private final BouquetService bouquetService;

    public CartPricingService(DeliveryService deliveryService, BouquetService bouquetService) {
        this.deliveryService = deliveryService;
        this.bouquetService = bouquetService;
    }

    /** One cart line before pricing: either a product or a resolved bouquet (or neither if unknown). */
    public record LineInput(String id, Product product, ResolvedBouquet bouquet, int quantity, String fallbackName) {
        public static LineInput product(String id, Product product, int quantity) {
            return new LineInput(id, product, null, quantity, null);
        }

        public static LineInput bouquet(String id, ResolvedBouquet bouquet, int quantity) {
            return new LineInput(id, null, bouquet, quantity, null);
        }

        public static LineInput unavailable(String id, int quantity, String name) {
            return new LineInput(id, null, null, quantity, name);
        }
    }

    public record PricedLine(LineInput input, BigDecimal unitPrice, BigDecimal lineTotal, int maxQuantity, LineIssue issue) {
    }

    public record PricedCart(List<PricedLine> lines, BigDecimal subtotal, BigDecimal deliveryFee,
                             BigDecimal giftWrappingFee, BigDecimal discount, BigDecimal total) {
        public boolean hasIssues() {
            return lines.stream().anyMatch(l -> l.issue() != null);
        }
    }

    public PricedCart price(List<LineInput> inputs, boolean giftWrapping) {
        List<PricedLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineInput input : inputs) {
            PricedLine line = priceLine(input);
            lines.add(line);
            if (line.issue() != LineIssue.UNAVAILABLE) {
                subtotal = subtotal.add(line.lineTotal());
            }
        }
        subtotal = money(subtotal);
        BigDecimal delivery = deliveryService.deliveryFeeFor(subtotal);
        BigDecimal wrapping = giftWrapping && !inputs.isEmpty() ? money(deliveryService.giftWrappingFee()) : money(BigDecimal.ZERO);
        BigDecimal discount = money(BigDecimal.ZERO);
        BigDecimal total = money(subtotal.add(delivery).add(wrapping).subtract(discount).max(BigDecimal.ZERO));
        return new PricedCart(lines, subtotal, money(delivery), wrapping, discount, total);
    }

    public CartView toView(PricedCart cart) {
        List<CartLine> lines = cart.lines().stream().map(this::toLine).toList();
        int count = cart.lines().stream().mapToInt(l -> l.input().quantity()).sum();
        BigDecimal threshold = deliveryService.freeDeliveryThreshold();
        BigDecimal remaining = money(threshold.subtract(cart.subtotal()).max(BigDecimal.ZERO));
        boolean ready = !cart.lines().isEmpty() && !cart.hasIssues();
        return new CartView(lines, count, cart.subtotal(), cart.deliveryFee(), cart.giftWrappingFee(),
                cart.discount(), cart.total(), threshold, remaining, ready, deliveryService.currency());
    }

    private PricedLine priceLine(LineInput input) {
        if (input.product() != null) {
            Product p = input.product();
            BigDecimal unit = p.getPrice();
            int max = p.isActive() ? Math.max(0, p.getMaxOrderableQuantity()) : 0;
            LineIssue issue = null;
            if (!p.isActive()) {
                issue = LineIssue.UNAVAILABLE;
            } else if (p.getStockQuantity() <= 0) {
                issue = LineIssue.OUT_OF_STOCK;
            } else if (input.quantity() > max) {
                issue = LineIssue.QUANTITY_EXCEEDS_STOCK;
            }
            return new PricedLine(input, unit, money(unit.multiply(BigDecimal.valueOf(input.quantity()))), max, issue);
        }
        if (input.bouquet() != null) {
            ResolvedBouquet b = input.bouquet();
            LineIssue issue = null;
            if (!b.available()) {
                issue = LineIssue.UNAVAILABLE;
            } else if (b.maxBouquets() <= 0) {
                issue = LineIssue.OUT_OF_STOCK;
            } else if (input.quantity() > b.maxBouquets()) {
                issue = LineIssue.QUANTITY_EXCEEDS_STOCK;
            }
            return new PricedLine(input, b.unitPrice(), money(b.unitPrice().multiply(BigDecimal.valueOf(input.quantity()))),
                    b.available() ? b.maxBouquets() : 0, issue);
        }
        return new PricedLine(input, money(BigDecimal.ZERO), money(BigDecimal.ZERO), 0, LineIssue.UNAVAILABLE);
    }

    private CartLine toLine(PricedLine line) {
        LineInput in = line.input();
        if (in.product() != null) {
            Product p = in.product();
            return new CartLine(in.id(), "PRODUCT", p.getId(), p.getSlug(), p.getName(), p.getPrimaryImageUrl(),
                    line.unitPrice(), in.quantity(), line.lineTotal(), line.maxQuantity(), line.issue(), null);
        }
        if (in.bouquet() != null) {
            String image = in.bouquet().stems().isEmpty() ? null : in.bouquet().stems().get(0).stem().getImageUrl();
            return new CartLine(in.id(), "CUSTOM_BOUQUET", null, null, "Bouquet personnalisé", image,
                    line.unitPrice(), in.quantity(), line.lineTotal(), line.maxQuantity(), line.issue(),
                    bouquetService.summary(in.bouquet()));
        }
        return new CartLine(in.id(), "UNKNOWN", null, null,
                in.fallbackName() == null ? "Article indisponible" : in.fallbackName(), null,
                line.unitPrice(), in.quantity(), line.lineTotal(), 0, line.issue(), null);
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
