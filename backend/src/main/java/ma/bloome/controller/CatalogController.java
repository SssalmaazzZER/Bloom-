package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import ma.bloome.dto.BouquetDtos.BouquetOptions;
import ma.bloome.dto.CatalogDtos.*;
import ma.bloome.dto.DeliveryDtos.DeliveryOptions;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.Enums.FlowerColor;
import ma.bloome.service.BouquetService;
import ma.bloome.service.CatalogService;
import ma.bloome.service.DeliveryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final BouquetService bouquetService;
    private final DeliveryService deliveryService;

    public CatalogController(CatalogService catalogService, BouquetService bouquetService, DeliveryService deliveryService) {
        this.catalogService = catalogService;
        this.bouquetService = bouquetService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/products")
    @Operation(summary = "Search, filter, sort and paginate active products")
    public PageResponse<ProductSummary> products(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> occasion,
            @RequestParam(required = false) List<FlowerColor> color,
            @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "POPULARITY") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalogService.search(new ProductFilter(q, category, occasion, color, minPrice, maxPrice, inStock, sort), page, size);
    }

    @GetMapping("/products/featured")
    public List<ProductSummary> featured(@RequestParam(defaultValue = "8") int limit) {
        return catalogService.featured(limit);
    }

    @GetMapping("/products/bestsellers")
    public List<ProductSummary> bestsellers(@RequestParam(defaultValue = "8") int limit) {
        return catalogService.bestsellers(limit);
    }

    @GetMapping("/products/{idOrSlug}")
    @Operation(summary = "Product details by numeric id or slug")
    public ProductDetail product(@PathVariable @Size(max = 120) String idOrSlug) {
        return catalogService.getProduct(idOrSlug);
    }

    @GetMapping("/products/{idOrSlug}/related")
    public List<ProductSummary> related(@PathVariable @Size(max = 120) String idOrSlug,
                                        @RequestParam(defaultValue = "4") int limit) {
        return catalogService.related(idOrSlug, limit);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalogService.categories();
    }

    @GetMapping("/occasions")
    public List<OccasionDto> occasions() {
        return catalogService.occasions();
    }

    @GetMapping("/catalog/facets")
    @Operation(summary = "Price range, colors, categories and occasions for building filters")
    public CatalogFacets facets() {
        return catalogService.facets();
    }

    @GetMapping("/bouquet/options")
    @Operation(summary = "Stems, wrappings and ribbons available in the bouquet builder")
    public BouquetOptions bouquetOptions() {
        return bouquetService.options();
    }

    @GetMapping("/delivery/options")
    @Operation(summary = "Delivery cities, slots, dates and fees")
    public DeliveryOptions deliveryOptions() {
        return deliveryService.options();
    }
}
