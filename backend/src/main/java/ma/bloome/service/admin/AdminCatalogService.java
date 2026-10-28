package ma.bloome.service.admin;

import ma.bloome.dto.AdminDtos.CategoryRequest;
import ma.bloome.dto.AdminDtos.Inventory;
import ma.bloome.dto.AdminDtos.StemInventory;
import ma.bloome.dto.CatalogDtos.CategoryDto;
import ma.bloome.entity.BouquetStem;
import ma.bloome.entity.Category;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.CatalogMapper;
import ma.bloome.repository.BouquetStemRepository;
import ma.bloome.repository.CategoryRepository;
import ma.bloome.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Category management and inventory overview (products + bouquet stems). */
@Service
@Transactional
public class AdminCatalogService {

    private static final int STEM_LOW_STOCK = 30;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BouquetStemRepository stemRepository;
    private final CatalogMapper mapper;

    public AdminCatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
                               BouquetStemRepository stemRepository, CatalogMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.stemRepository = stemRepository;
        this.mapper = mapper;
    }

    public CategoryDto createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("SLUG_TAKEN", "Ce slug de catégorie est déjà utilisé.");
        }
        Category category = new Category();
        apply(category, request);
        return mapper.toCategory(categoryRepository.saveAndFlush(category), 0);
    }

    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Catégorie introuvable."));
        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw ApiException.conflict("SLUG_TAKEN", "Ce slug de catégorie est déjà utilisé.");
        }
        apply(category, request);
        return mapper.toCategory(categoryRepository.saveAndFlush(category), 0);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Catégorie introuvable."));
        if (categoryRepository.hasProducts(id)) {
            throw ApiException.conflict("CATEGORY_NOT_EMPTY",
                    "Cette catégorie contient des produits. Déplacez-les avant de la supprimer.");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Inventory inventory() {
        var products = productRepository.findAll(Sort.by("stockQuantity", "name")).stream()
                .map(mapper::toAdmin).toList();
        List<StemInventory> stems = stemRepository.findAllByOrderBySortOrderAsc().stream().map(this::toStem).toList();
        return new Inventory(products, stems);
    }

    public StemInventory adjustStemStock(Long stemId, int delta) {
        if (!stemRepository.existsById(stemId)) {
            throw ApiException.notFound("Fleur introuvable.");
        }
        if (stemRepository.adjustStock(stemId, delta) == 0) {
            throw ApiException.badRequest("INVALID_STOCK", "Le stock ne peut pas devenir négatif.");
        }
        return stemRepository.findById(stemId).map(this::toStem).orElseThrow();
    }

    private StemInventory toStem(BouquetStem s) {
        return new StemInventory(s.getId(), s.getName(), s.getColor().name(), s.getUnitPrice(), s.getStockQuantity(),
                s.isActive(), s.getStockQuantity() <= STEM_LOW_STOCK);
    }

    private void apply(Category category, CategoryRequest r) {
        category.setName(r.name().trim());
        category.setSlug(r.slug());
        category.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        category.setImageUrl(r.imageUrl() == null || r.imageUrl().isBlank() ? null : r.imageUrl().trim());
        category.setSortOrder(r.sortOrder());
    }
}
