package ma.bloome.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Image references are stored as URLs (no binary uploads). Accepts absolute https URLs or
 * site-relative paths under /assets/. Rejects javascript:, data:, http: and malformed values.
 */
@Documented
@Constraint(validatedBy = SafeImageUrl.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeImageUrl {

    String message() default "L'URL de l'image doit commencer par https:// (ou /assets/).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<SafeImageUrl, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            if (value.length() > 500) {
                return false;
            }
            if (value.startsWith("/assets/")) {
                return !value.contains("..") && value.matches("^/assets/[A-Za-z0-9/_\\-.]+$");
            }
            try {
                URI uri = new URI(value);
                return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
