package ma.bloome.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.bloome.dto.AccountDtos.*;
import ma.bloome.dto.CatalogDtos.ProductSummary;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.AccountService;
import ma.bloome.service.NewsletterService;
import ma.bloome.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Account, wishlist & newsletter")
public class AccountController {

    private final AccountService accountService;
    private final WishlistService wishlistService;
    private final NewsletterService newsletterService;

    public AccountController(AccountService accountService, WishlistService wishlistService,
                             NewsletterService newsletterService) {
        this.accountService = accountService;
        this.wishlistService = wishlistService;
        this.newsletterService = newsletterService;
    }

    @PutMapping("/account/profile")
    public UserDto updateProfile(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody ProfileUpdateRequest request) {
        return accountService.updateProfile(user.getId(), request);
    }

    @PutMapping("/account/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(user.getId(), request);
    }

    @GetMapping("/account/addresses")
    public List<AddressDto> addresses(@AuthenticationPrincipal AppUserDetails user) {
        return accountService.addresses(user.getId());
    }

    @PostMapping("/account/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto createAddress(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody AddressRequest request) {
        return accountService.createAddress(user.getId(), request);
    }

    @PutMapping("/account/addresses/{id}")
    public AddressDto updateAddress(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                                    @Valid @RequestBody AddressRequest request) {
        return accountService.updateAddress(user.getId(), id, request);
    }

    @DeleteMapping("/account/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        accountService.deleteAddress(user.getId(), id);
    }

    @GetMapping("/wishlist")
    public List<ProductSummary> wishlist(@AuthenticationPrincipal AppUserDetails user) {
        return wishlistService.list(user.getId());
    }

    @PostMapping("/wishlist/items/{productId}")
    public List<ProductSummary> addToWishlist(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long productId) {
        return wishlistService.add(user.getId(), productId);
    }

    @DeleteMapping("/wishlist/items/{productId}")
    public List<ProductSummary> removeFromWishlist(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long productId) {
        return wishlistService.remove(user.getId(), productId);
    }

    @PostMapping("/newsletter")
    public MessageResponse subscribe(@Valid @RequestBody NewsletterRequest request) {
        newsletterService.subscribe(request.email());
        return new MessageResponse("Merci ! Vous recevrez bientôt nos nouveautés de saison.");
    }
}
