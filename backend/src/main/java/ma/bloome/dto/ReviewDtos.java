package ma.bloome.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ReviewDtos {
    private ReviewDtos() {
    }

    public record ReviewRequest(
            @Min(value = 1, message = "La note doit être comprise entre 1 et 5.")
            @Max(value = 5, message = "La note doit être comprise entre 1 et 5.") int rating,
            @Size(max = 120) String title,
            @NotBlank(message = "Votre avis ne peut pas être vide.") @Size(min = 10, max = 2000,
                    message = "Votre avis doit contenir entre 10 et 2000 caractères.") String comment
    ) {
    }

    /** Author is shown as "Prénom N." — never the e-mail address. */
    public record ReviewDto(Long id, String authorName, int rating, String title, String comment,
                            Instant createdAt, boolean verifiedPurchase) {
    }

    public record ReviewPage(PageResponse<ReviewDto> reviews, Double average, long count,
                             boolean canReview, boolean alreadyReviewed) {
    }
}
