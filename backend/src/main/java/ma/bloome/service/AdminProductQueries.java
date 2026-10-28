package ma.bloome.service;

import ma.bloome.dto.CatalogDtos.ProductFilter;
import ma.bloome.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/** Admin-side product queries reuse the catalog specification without the "active only" restriction. */
public final class AdminProductQueries {
    private AdminProductQueries() {
    }

    public static Specification<Product> adminFilter(ProductFilter filter, Boolean active, Boolean lowStock) {
        Specification<Product> spec = ProductSpecifications.matching(filter, false);
        if (active != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("active"), active));
        }
        if (Boolean.TRUE.equals(lowStock)) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("lowStockThreshold")));
        }
        return spec;
    }
}
