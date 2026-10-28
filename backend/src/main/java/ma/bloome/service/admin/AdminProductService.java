package ma.bloome.service.admin;

import ma.bloome.dto.AdminDtos.*;
import ma.bloome.dto.CatalogDtos.ProductFilter;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.Category;
import ma.bloome.entity.Occasion;
import ma.bloome.entity.Product;
import ma.bloome.entity.ProductImage;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.CatalogMapper;
import ma.bloome.repository.CategoryRepository;
import ma.bloome.repository.OccasionRepository;
import ma.bloome.repository.ProductRepository;
import ma.bloome.service.AdminProductQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Service
@Transactional
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OccasionRepository occasionRepository;
    private final CatalogMapper mapper;

    public AdminProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                               OccasionRepository occasionRepository, CatalogMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.occasionRepository = occasionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductView> list(String q, String category, Boolean active, Boolean lowStock, int page, int size) {
        ProductFilter filter = new ProductFilter(q, category == null || category.isBlank() ? null : List.of(category),
                null, null, null, null, null, null);
        Page<Product> result = productRepository.findAll(AdminProductQueries.adminFilter(filter, active, lowStock),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Order.asc("name"))));
        return PageResponse.of(result, mapper::toAdmin);
    }

    @Transactional(readOnly = true)
    public AdminProductView get(Long id) {
        return mapper.toAdmin(find(id));
    }

    public AdminProductView create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        product.setSlug(uniqueSlug(request.slug(), request.name(), null));
        return mapper.toAdmin(productRepository.saveAndFlush(product));
    }

    public AdminProductView update(Long id, ProductRequest request) {
        Product product = find(id);
        if (request.version() != null && request.version() != product.getVersion()) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "Ce produit a été modifié entre-temps (par exemple par une commande). Rechargez-le avant d'enregistrer.");
        }
        apply(product, request);
        product.setSlug(uniqueSlug(request.slug(), request.name(), id));
        return mapper.toAdmin(productRepository.saveAndFlush(product));
    }

    /**
     * Products are never hard-deleted: they are referenced by historical orders and carts.
     * "Delete" deactivates the product, hiding it from the shop and making it unpurchasable.
     */
    public AdminProductView deactivate(Long id) {
        Product product = find(id);
        product.setActive(false);
        product.setFeatured(false);
        return mapper.toAdmin(productRepository.saveAndFlush(product));
    }

    public AdminProductView adjustStock(Long id, int delta) {
        if (!productRepository.existsById(id)) {
            throw ApiException.notFound("Produit introuvable.");
        }
        if (productRepository.adjustStock(id, delta) == 0) {
            throw ApiException.badRequest("INVALID_STOCK", "Le stock ne peut pas devenir négatif.");
        }
        return mapper.toAdmin(productRepository.findWithCategoryById(id).orElseThrow());
    }

    private void apply(Product product, ProductRequest r) {
        if (r.compareAtPrice() != null && r.compareAtPrice().compareTo(r.price()) <= 0) {
            throw ApiException.badRequest("INVALID_PRICE", "Le prix barré doit être supérieur au prix de vente.");
        }
        Category category = categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Catégorie inconnue."));
        List<Occasion> occasions = occasionRepository.findAllById(r.occasionIds());
        if (occasions.size() != r.occasionIds().size()) {
            throw ApiException.badRequest("INVALID_OCCASION", "Une des occasions sélectionnées est inconnue.");
        }
        product.setName(r.name().trim());
        product.setShortDescription(r.shortDescription().trim());
        product.setDescription(r.description().trim());
        product.setComposition(blankToNull(r.composition()));
        product.setCareInstructions(blankToNull(r.careInstructions()));
        product.setCategory(category);
        product.setPrice(r.price().setScale(2, java.math.RoundingMode.HALF_UP));
        product.setCompareAtPrice(r.compareAtPrice() == null ? null : r.compareAtPrice().setScale(2, java.math.RoundingMode.HALF_UP));
        product.setStockQuantity(r.stockQuantity());
        product.setLowStockThreshold(r.lowStockThreshold());
        product.setMaxPerOrder(r.maxPerOrder());
        product.setActive(r.active());
        product.setFeatured(r.featured());
        product.setBestseller(r.bestseller());
        product.getColors().clear();
        product.getColors().addAll(r.colors());
        product.getOccasions().clear();
        product.getOccasions().addAll(occasions);
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < r.images().size(); i++) {
            ImageRequest img = r.images().get(i);
            images.add(new ProductImage(img.url().trim(), blankToNull(img.altText()), i));
        }
        product.replaceImages(images);
    }

    private String uniqueSlug(String requested, String name, Long currentId) {
        String base = requested == null || requested.isBlank() ? slugify(name) : requested;
        if (base.isBlank()) {
            base = "produit";
        }
        String candidate = base;
        int suffix = 2;
        while (currentId == null ? productRepository.existsBySlug(candidate) : productRepository.existsBySlugAndIdNot(candidate, currentId)) {
            if (requested != null && !requested.isBlank()) {
                throw ApiException.conflict("SLUG_TAKEN", "Ce slug est déjà utilisé par un autre produit.");
            }
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    static String slugify(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private Product find(Long id) {
        return productRepository.findWithCategoryById(id).orElseThrow(() -> ApiException.notFound("Produit introuvable."));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
