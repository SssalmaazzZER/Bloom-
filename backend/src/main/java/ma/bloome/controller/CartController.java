package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.bloome.dto.CartDtos.*;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Current customer's cart, re-priced with live prices and stock")
    public CartView get(@AuthenticationPrincipal AppUserDetails user,
                        @RequestParam(defaultValue = "false") boolean giftWrapping) {
        return cartService.getCart(user.getId(), giftWrapping);
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product or a custom bouquet")
    public CartView add(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(user.getId(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartView update(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long itemId,
                           @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(user.getId(), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartView remove(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long itemId) {
        return cartService.removeItem(user.getId(), itemId);
    }

    @DeleteMapping
    public CartView clear(@AuthenticationPrincipal AppUserDetails user) {
        return cartService.clear(user.getId());
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge the anonymous browser cart into the customer's cart after login")
    public CartView merge(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody CartItemsRequest request) {
        return cartService.merge(user.getId(), request.items());
    }

    @PostMapping("/quote")
    @Operation(summary = "Price an anonymous cart (no login, nothing persisted)")
    public CartView quote(@Valid @RequestBody CartItemsRequest request,
                          @RequestParam(defaultValue = "false") boolean giftWrapping) {
        return cartService.quote(request.items(), giftWrapping);
    }
}
