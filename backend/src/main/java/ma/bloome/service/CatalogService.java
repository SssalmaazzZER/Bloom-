package ma.bloome.service;

import ma.bloome.dto.CatalogDtos.*;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.Enums.FlowerColor;
import ma.bloome.entity.Product;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.CatalogMapper;
import ma.bloome.mapper.CatalogMapper.Rating;
import ma.bloome.repository.CategoryRepository;
import ma.bloome.repository.OccasionRepository;
import ma.bloome.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int MAX_PAGE_SIZE = 48;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OccasionRepository occasionRepository;
    private final CatalogMapper mapper;
    private final RatingService ratingService;
    private final DeliveryService deliveryService;

    public CatalogService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          OccasionRepository occasionRepository, CatalogMapper mapper,
                          RatingService ratingService, DeliveryService deliveryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.occasionRepository = occasionRepository;
        this.mapper = mapper;
        this.ratingService = ratingService;
        this.deliveryService = deliveryService;
    }

    public PageResponse<ProductSummary> search(ProductFilter filter, int page, int size) {
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw ApiException.badRequest("INVALID_PRICE_RANGE", "Le prix minimum doit être inférieur au prix maximum.");
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                sortFor(filter.sort()));
        Page<Product> result = productRepository.findAll(ProductSpecifications.matching(filter, true), pageable);
        return PageResponse.ofMapped(result, toSummaries(result.getContent()));
    }

    public ProductDetail getProduct(String idOrSlug) {
        Product product = findVisible(idOrSlug);
        return mapper.toDetail(product, ratingService.ratingFor(product.getId()), deliveryService.estimate());
    }

    public List<ProductSummary> featured(int limit) {
        return toSummaries(productRepository.findByActiveTrueAndFeaturedTrueOrderBySalesCountDesc(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductSummary> bestsellers(int limit) {
        return toSummaries(productRepository.findByActiveTrueAndBestsellerTrueOrderBySalesCountDesc(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductSummary> related(String idOrSlug, int limit) {
        Product product = findVisible(idOrSlug);
        return toSummaries(productRepository.findRelated(product.getId(), product.getCategory().getId(),
                PageRequest.of(0, clamp(limit))));
    }

    public List<CategoryDto> categories() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : categoryRepository.countActiveProductsByCategory()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(c -> mapper.toCategory(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    public List<OccasionDto> occasions() {
        return occasionRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::toOccasion).toList();
    }

    public CatalogFacets facets() {
        BigDecimal min = productRepository.findMinActivePrice();
        BigDecimal max = productRepository.findMaxActivePrice();
        return new CatalogFacets(min == null ? BigDecimal.ZERO : min, max == null ? BigDecimal.ZERO : max,
                Arrays.asList(FlowerColor.values()), categories(), occasions());
    }

    Product findVisible(String idOrSlug) {
        var product = idOrSlug.chars().allMatch(Character::isDigit)
                ? productRepository.findWithCategoryById(Long.parseLong(idOrSlug))
                : productRepository.findBySlug(idOrSlug);
        return product.filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Ce produit est introuvable ou n'est plus proposé."));
    }

    private List<ProductSummary> toSummaries(List<Product> products) {
        Map<Long, Rating> ratings = ratingService.ratingsFor(products.stream().map(Product::getId).toList());
        return products.stream().map(p -> mapper.toSummary(p, ratings.getOrDefault(p.getId(), Rating.NONE))).toList();
    }

    private static Sort sortFor(ProductSort sort) {
        if (sort == null) {
            sort = ProductSort.POPULARITY;
        }
        return switch (sort) {
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case NAME_ASC -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            case NAME_DESC -> Sort.by(Sort.Order.desc("name"), Sort.Order.asc("id"));
            case POPULARITY -> Sort.by(Sort.Order.desc("salesCount"), Sort.Order.asc("id"));
        };
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 24);
    }
}
