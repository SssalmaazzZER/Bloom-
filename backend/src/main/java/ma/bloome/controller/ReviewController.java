package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.bloome.dto.ReviewDtos.ReviewDto;
import ma.bloome.dto.ReviewDtos.ReviewPage;
import ma.bloome.dto.ReviewDtos.ReviewRequest;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{idOrSlug}/reviews")
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Reviews of a product, with rating summary and whether the caller may review")
    public ReviewPage list(@PathVariable String idOrSlug, @AuthenticationPrincipal AppUserDetails user,
                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return reviewService.list(idOrSlug, user == null ? null : user.getId(), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Review a product (verified buyers with a delivered order only)")
    public ReviewDto create(@PathVariable String idOrSlug, @AuthenticationPrincipal AppUserDetails user,
                            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(idOrSlug, user.getId(), request);
    }
}
