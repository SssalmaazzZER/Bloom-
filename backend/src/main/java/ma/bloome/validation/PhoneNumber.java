package ma.bloome.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * Moroccan phone number: 0XXXXXXXXX or +212XXXXXXXXX (spaces, dots and dashes are ignored).
 * Null/blank values are considered valid; combine with @NotBlank when required.
 */
@Documented
@Constraint(validatedBy = PhoneNumber.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "Numéro de téléphone invalide (ex. 06 12 34 56 78).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PhoneNumber, String> {
        private static final Pattern PATTERN = Pattern.compile("^(?:\\+212|00212|0)[5-7]\\d{8}$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            return PATTERN.matcher(normalize(value)).matches();
        }

        public static String normalize(String value) {
            return value.replaceAll("[\\s.\\-()]", "");
        }
    }
}
