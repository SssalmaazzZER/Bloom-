package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.bloome.dto.AdminDtos.*;
import ma.bloome.dto.CatalogDtos.CategoryDto;
import ma.bloome.dto.OrderDtos.OrderDetail;
import ma.bloome.dto.OrderDtos.OrderSummary;
import ma.bloome.dto.PageResponse;
import ma.bloome.entity.OrderStatus;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.OrderService;
import ma.bloome.service.admin.AdminCatalogService;
import ma.bloome.service.admin.AdminDashboardService;
import ma.bloome.service.admin.AdminProductService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Administration API. Access is restricted to ROLE_ADMIN in SecurityConfig and again here. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration")
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final AdminProductService productService;
    private final AdminCatalogService catalogService;
    private final OrderService orderService;

    public AdminController(AdminDashboardService dashboardService, AdminProductService productService,
                           AdminCatalogService catalogService, OrderService orderService) {
        this.dashboardService = dashboardService;
        this.productService = productService;
        this.catalogService = catalogService;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Live metrics computed from the database")
    public Dashboard dashboard() {
        return dashboardService.dashboard();
    }

    // ---- products
    @GetMapping("/products")
    public PageResponse<AdminProductView> products(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(required = false) Boolean lowStock,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return productService.list(q, category, active, lowStock, page, size);
    }

    @GetMapping("/products/{id}")
    public AdminProductView product(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductView createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/products/{id}")
    public AdminProductView updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Deactivate a product (soft delete — order history is preserved)")
    public AdminProductView deleteProduct(@PathVariable Long id) {
        return productService.deactivate(id);
    }

    @PatchMapping("/products/{id}/stock")
    @Operation(summary = "Atomically adjust stock by a signed delta")
    public AdminProductView adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return productService.adjustStock(id, request.delta());
    }

    // ---- categories
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalogService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
    }

    // ---- inventory
    @GetMapping("/inventory")
    public Inventory inventory() {
        return catalogService.inventory();
    }

    @PatchMapping("/stems/{id}/stock")
    public StemInventory adjustStemStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return catalogService.adjustStemStock(id, request.delta());
    }

    // ---- orders
    @GetMapping("/orders")
    public PageResponse<OrderSummary> orders(@RequestParam(required = false) OrderStatus status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return orderService.search(status, q, from, to, page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderDetail order(@PathVariable Long id) {
        return orderService.adminGet(id);
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Move an order to the next status (only valid transitions are accepted)")
    public OrderDetail updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                    @AuthenticationPrincipal AppUserDetails admin) {
        return orderService.adminUpdateStatus(id, request.status(), request.note(), admin.getId());
    }

    // ---- customers
    @GetMapping("/customers")
    public PageResponse<CustomerSummary> customers(@RequestParam(required = false) String q,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return dashboardService.customers(q, page, size);
    }

    @GetMapping("/customers/{id}")
    public CustomerDetail customer(@PathVariable Long id) {
        return dashboardService.customer(id);
    }
}
