package ma.bloome.service;

import ma.bloome.dto.PageResponse;
import ma.bloome.dto.ReviewDtos.*;
import ma.bloome.entity.Product;
import ma.bloome.entity.Review;
import ma.bloome.entity.User;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.CatalogMapper.Rating;
import ma.bloome.repository.CustomerOrderRepository;
import ma.bloome.repository.ReviewRepository;
import ma.bloome.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reviews are restricted to verified buyers: a customer may review a product once, and only
 * after an order containing it has been DELIVERED.
 */
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;
    private final RatingService ratingService;

    public ReviewService(ReviewRepository reviewRepository, CustomerOrderRepository orderRepository,
                         UserRepository userRepository, CatalogService catalogService, RatingService ratingService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
        this.ratingService = ratingService;
    }

    @Transactional(readOnly = true)
    public ReviewPage list(String productIdOrSlug, Long currentUserId, int page, int size) {
        Product product = catalogService.findVisible(productIdOrSlug);
        var reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 20)));
        Rating rating = ratingService.ratingFor(product.getId());
        boolean already = currentUserId != null && reviewRepository.existsByProductIdAndUserId(product.getId(), currentUserId);
        boolean canReview = currentUserId != null && !already
                && orderRepository.hasDeliveredOrderWithProduct(currentUserId, product.getId());
        return new ReviewPage(PageResponse.of(reviews, this::toDto), rating.average(), rating.count(), canReview, already);
    }

    public ReviewDto create(String productIdOrSlug, Long userId, ReviewRequest request) {
        Product product = catalogService.findVisible(productIdOrSlug);
        if (reviewRepository.existsByProductIdAndUserId(product.getId(), userId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "Vous avez déjà laissé un avis sur ce produit.");
        }
        if (!orderRepository.hasDeliveredOrderWithProduct(userId, product.getId())) {
            throw ApiException.forbidden("Seuls les clients ayant reçu ce produit peuvent laisser un avis.");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(userRepository.getReferenceById(userId));
        review.setRating((short) request.rating());
        review.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());
        review.setComment(request.comment().trim());
        review = reviewRepository.saveAndFlush(review);
        return toDto(reviewRepository.findById(review.getId()).orElseThrow());
    }

    private ReviewDto toDto(Review r) {
        User u = r.getUser();
        String author = u.getFirstName() + " " + (u.getLastName().isEmpty() ? "" : u.getLastName().charAt(0) + ".");
        return new ReviewDto(r.getId(), author.trim(), r.getRating(), r.getTitle(), r.getComment(), r.getCreatedAt(), true);
    }
}
