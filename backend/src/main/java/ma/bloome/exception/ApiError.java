package ma.bloome.exception;

import java.time.Instant;
import java.util.List;

/** Uniform error body returned by every endpoint. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {
    }
}
