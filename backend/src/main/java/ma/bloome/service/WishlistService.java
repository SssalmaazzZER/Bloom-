package ma.bloome.service;

import ma.bloome.dto.CatalogDtos.ProductSummary;
import ma.bloome.entity.Product;
import ma.bloome.entity.WishlistItem;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.CatalogMapper;
import ma.bloome.mapper.CatalogMapper.Rating;
import ma.bloome.repository.ProductRepository;
import ma.bloome.repository.UserRepository;
import ma.bloome.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CatalogMapper mapper;
    private final RatingService ratingService;

    public WishlistService(WishlistItemRepository wishlistRepository, ProductRepository productRepository,
                           UserRepository userRepository, CatalogMapper mapper, RatingService ratingService) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.ratingService = ratingService;
    }

    @Transactional(readOnly = true)
    public List<ProductSummary> list(Long userId) {
        List<Product> products = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WishlistItem::getProduct)
                .filter(Product::isActive)
                .toList();
        Map<Long, Rating> ratings = ratingService.ratingsFor(products.stream().map(Product::getId).toList());
        return products.stream().map(p -> mapper.toSummary(p, ratings.getOrDefault(p.getId(), Rating.NONE))).toList();
    }

    /** Idempotent: adding an already-saved product is a no-op. */
    public List<ProductSummary> add(Long userId, Long productId) {
        Product product = productRepository.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Ce produit n'est plus proposé."));
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            WishlistItem item = new WishlistItem();
            item.setUser(userRepository.getReferenceById(userId));
            item.setProduct(product);
            wishlistRepository.saveAndFlush(item);
        }
        return list(userId);
    }

    public List<ProductSummary> remove(Long userId, Long productId) {
        wishlistRepository.findByUserIdAndProductId(userId, productId).ifPresent(wishlistRepository::delete);
        wishlistRepository.flush();
        return list(userId);
    }
}
