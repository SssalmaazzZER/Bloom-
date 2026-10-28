package ma.bloome.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import ma.bloome.dto.CatalogDtos.ProductFilter;
import ma.bloome.entity.Enums.FlowerColor;
import ma.bloome.entity.Occasion;
import ma.bloome.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds parameterised criteria queries for catalog filtering. Multi-valued filters use
 * EXISTS sub-queries so pagination counts stay correct (no duplicate rows from joins).
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> matching(ProductFilter filter, boolean activeOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (activeOnly) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + escapeLike(filter.q().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("shortDescription")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.get("composition")), pattern, '\\')));
            }
            if (filter.categories() != null && !filter.categories().isEmpty()) {
                predicates.add(root.get("category").get("slug").in(filter.categories()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            if (Boolean.TRUE.equals(filter.inStock())) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }
            if (filter.colors() != null && !filter.colors().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                Join<Product, FlowerColor> color = p2.join("colors");
                sub.select(p2.get("id")).where(cb.equal(p2.get("id"), root.get("id")), color.in(filter.colors()));
                predicates.add(cb.exists(sub));
            }
            if (filter.occasions() != null && !filter.occasions().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                Join<Product, Occasion> occasion = p2.join("occasions");
                sub.select(p2.get("id")).where(cb.equal(p2.get("id"), root.get("id")),
                        occasion.get("slug").in(filter.occasions()));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
