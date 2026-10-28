package ma.bloome.service;

import ma.bloome.dto.CartDtos.*;
import ma.bloome.entity.*;
import ma.bloome.exception.ApiException;
import ma.bloome.exception.InsufficientStockException;
import ma.bloome.repository.CartRepository;
import ma.bloome.repository.ProductRepository;
import ma.bloome.repository.UserRepository;
import ma.bloome.service.BouquetService.ResolvedBouquet;
import ma.bloome.service.CartPricingService.LineInput;
import ma.bloome.service.CartPricingService.PricedCart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Server-side cart for authenticated customers plus stateless pricing ("quote") for the
 * anonymous cart kept in the browser. Every operation re-checks prices and stock.
 */
@Service
@Transactional
public class CartService {

    /** Maximum number of distinct lines in a cart. */
    public static final int MAX_LINES = 30;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BouquetService bouquetService;
    private final CartPricingService pricingService;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       UserRepository userRepository, BouquetService bouquetService,
                       CartPricingService pricingService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.bouquetService = bouquetService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public CartView getCart(Long userId, boolean giftWrapping) {
        return cartRepository.findByUserId(userId)
                .map(cart -> pricingService.toView(price(cart, giftWrapping)))
                .orElseGet(() -> pricingService.toView(pricingService.price(List.of(), giftWrapping)));
    }

    public CartView addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreate(userId);
        if (request.productId() != null) {
            Product product = productRepository.findById(request.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> ApiException.notFound("Ce produit n'est plus proposé."));
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProduct() != null && i.getProduct().getId().equals(product.getId()))
                    .findFirst();
            int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
            assertProductQuantity(product, newQuantity);
            if (existing.isPresent()) {
                existing.get().setQuantity(newQuantity);
            } else {
                assertRoomForLine(cart);
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(newQuantity);
                cart.getItems().add(item);
            }
        } else {
            ResolvedBouquet resolved = bouquetService.resolve(request.bouquet());
            assertBouquetQuantity(resolved, request.quantity());
            assertRoomForLine(cart);
            BouquetConfiguration configuration = bouquetService.persist(resolved, cart.getUser());
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setBouquetConfiguration(configuration);
            item.setQuantity(request.quantity());
            cart.getItems().add(item);
        }
        cart.touch();
        cartRepository.flush();
        return pricingService.toView(price(cart, false));
    }

    public CartView updateItem(Long userId, Long itemId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Article introuvable dans votre panier."));
        CartItem item = findItem(cart, itemId);
        if (item.getProduct() != null) {
            assertProductQuantity(item.getProduct(), quantity);
        } else {
            assertBouquetQuantity(bouquetService.resolve(item.getBouquetConfiguration()), quantity);
        }
        item.setQuantity(quantity);
        cart.touch();
        return pricingService.toView(price(cart, false));
    }

    public CartView removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Article introuvable dans votre panier."));
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        cart.touch();
        return pricingService.toView(price(cart, false));
    }

    public CartView clear(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.touch();
        });
        return getCartView(userId);
    }

    /**
     * Merges the anonymous (browser) cart into the customer's cart after login. Quantities of
     * identical products/bouquets are added together and capped at what is available; invalid
     * or unavailable lines are skipped silently (they are reported by the next GET /cart).
     */
    public CartView merge(Long userId, List<CartItemRequest> guestItems) {
        Cart cart = getOrCreate(userId);
        for (CartItemRequest request : guestItems) {
            try {
                mergeOne(cart, request);
            } catch (ApiException ignored) {
                // An invalid guest line must not prevent the others from merging.
            }
        }
        cart.touch();
        cartRepository.flush();
        return pricingService.toView(price(cart, false));
    }

    /** Prices an anonymous cart without persisting anything. Unknown lines are flagged, not rejected. */
    @Transactional(readOnly = true)
    public CartView quote(List<CartItemRequest> items, boolean giftWrapping) {
        List<LineInput> inputs = new ArrayList<>();
        int index = 0;
        for (CartItemRequest request : items) {
            String id = request.clientKey() != null ? request.clientKey() : "line-" + index;
            index++;
            if (request.productId() != null) {
                Optional<Product> product = productRepository.findById(request.productId());
                inputs.add(product.map(p -> LineInput.product(id, p, request.quantity()))
                        .orElseGet(() -> LineInput.unavailable(id, request.quantity(), "Produit indisponible")));
            } else if (request.bouquet() != null) {
                try {
                    inputs.add(LineInput.bouquet(id, bouquetService.resolve(request.bouquet()), request.quantity()));
                } catch (ApiException e) {
                    inputs.add(LineInput.unavailable(id, request.quantity(), "Bouquet personnalisé"));
                }
            }
        }
        return pricingService.toView(pricingService.price(inputs, giftWrapping));
    }

    PricedCart price(Cart cart, boolean giftWrapping) {
        List<LineInput> inputs = cart.getItems().stream().map(this::toInput).toList();
        return pricingService.price(inputs, giftWrapping);
    }

    LineInput toInput(CartItem item) {
        String id = String.valueOf(item.getId());
        if (item.getProduct() != null) {
            return LineInput.product(id, item.getProduct(), item.getQuantity());
        }
        return LineInput.bouquet(id, bouquetService.resolve(item.getBouquetConfiguration()), item.getQuantity());
    }

    private CartView getCartView(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> pricingService.toView(price(cart, false)))
                .orElseGet(() -> pricingService.toView(pricingService.price(List.of(), false)));
    }

    private void mergeOne(Cart cart, CartItemRequest request) {
        if (request.productId() != null) {
            Product product = productRepository.findById(request.productId()).filter(Product::isPurchasable).orElse(null);
            if (product == null) {
                return;
            }
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProduct() != null && i.getProduct().getId().equals(product.getId()))
                    .findFirst();
            int wanted = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
            int capped = Math.min(wanted, product.getMaxOrderableQuantity());
            if (existing.isPresent()) {
                existing.get().setQuantity(Math.max(existing.get().getQuantity(), capped));
            } else if (capped > 0 && cart.getItems().size() < MAX_LINES) {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(capped);
                cart.getItems().add(item);
            }
            return;
        }
        if (request.bouquet() == null) {
            return;
        }
        ResolvedBouquet resolved = bouquetService.resolve(request.bouquet());
        Optional<CartItem> same = cart.getItems().stream()
                .filter(i -> i.getBouquetConfiguration() != null
                        && signature(bouquetService.resolve(i.getBouquetConfiguration())).equals(signature(resolved)))
                .findFirst();
        int max = Math.min(resolved.maxBouquets(), BouquetService.MAX_BOUQUETS_PER_LINE);
        if (same.isPresent()) {
            same.get().setQuantity(Math.max(1, Math.min(same.get().getQuantity() + request.quantity(), Math.max(max, 1))));
        } else if (max > 0 && cart.getItems().size() < MAX_LINES) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setBouquetConfiguration(bouquetService.persist(resolved, cart.getUser()));
            item.setQuantity(Math.min(request.quantity(), max));
            cart.getItems().add(item);
        }
    }

    private static String signature(ResolvedBouquet b) {
        StringBuilder sb = new StringBuilder();
        sb.append(b.wrapping().getId()).append('|').append(b.ribbon().getId()).append('|')
                .append(Objects.toString(b.message(), "")).append('|');
        b.stems().stream()
                .sorted(Comparator.comparing(s -> s.stem().getId()))
                .forEach(s -> sb.append(s.stem().getId()).append('x').append(s.quantity()).append(','));
        return sb.toString();
    }

    private Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.getReferenceById(userId);
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, Long itemId) {
        // Ownership: only items of the caller's own cart are reachable.
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Article introuvable dans votre panier."));
    }

    private void assertRoomForLine(Cart cart) {
        if (cart.getItems().size() >= MAX_LINES) {
            throw ApiException.badRequest("CART_FULL", "Votre panier contient déjà le nombre maximal d'articles.");
        }
    }

    private static void assertProductQuantity(Product product, int quantity) {
        if (!product.isPurchasable()) {
            throw new InsufficientStockException(product.getName(), 0);
        }
        if (quantity > product.getMaxPerOrder() && product.getMaxPerOrder() <= product.getStockQuantity()) {
            throw ApiException.badRequest("QUANTITY_LIMIT",
                    "Vous pouvez commander jusqu'à " + product.getMaxPerOrder() + " exemplaires de « " + product.getName() + " ».");
        }
        if (quantity > product.getStockQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity());
        }
    }

    private static void assertBouquetQuantity(ResolvedBouquet bouquet, int quantity) {
        if (!bouquet.available()) {
            throw ApiException.conflict("UNAVAILABLE", "Ce bouquet contient des éléments qui ne sont plus disponibles.");
        }
        if (quantity > BouquetService.MAX_BOUQUETS_PER_LINE) {
            throw ApiException.badRequest("QUANTITY_LIMIT",
                    "Vous pouvez commander jusqu'à " + BouquetService.MAX_BOUQUETS_PER_LINE + " exemplaires d'un bouquet personnalisé.");
        }
        if (quantity > bouquet.maxBouquets()) {
            throw new InsufficientStockException("Bouquet personnalisé", bouquet.maxBouquets());
        }
    }
}
