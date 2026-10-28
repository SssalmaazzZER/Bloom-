package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.bloome.dto.OrderDtos.*;
import ma.bloome.dto.PageResponse;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order from the current cart (cash on delivery). Idempotent per idempotencyKey.")
    public ResponseEntity<OrderDetail> checkout(@AuthenticationPrincipal AppUserDetails user,
                                                @Valid @RequestBody CheckoutRequest request) {
        OrderService.CheckoutResult result = orderService.checkout(user.getId(), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.order());
    }

    @GetMapping("/my")
    public PageResponse<OrderSummary> myOrders(@AuthenticationPrincipal AppUserDetails user,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return orderService.myOrders(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of the current customer's orders (404 for orders of other customers)")
    public OrderDetail get(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return orderService.myOrder(user.getId(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order while it is still PENDING or CONFIRMED")
    public OrderDetail cancel(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                              @Valid @RequestBody(required = false) CancelRequest request) {
        return orderService.cancelByCustomer(user.getId(), id, request == null ? null : request.reason());
    }
}
