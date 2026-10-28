package ma.bloome.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** At least 8 characters (max 72, the BCrypt limit) with at least one letter and one digit. */
@Documented
@Constraint(validatedBy = StrongPassword.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Le mot de passe doit contenir au moins 8 caractères, dont une lettre et un chiffre.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StrongPassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            return value.length() >= 8 && value.length() <= 72
                    && value.chars().anyMatch(Character::isLetter)
                    && value.chars().anyMatch(Character::isDigit);
        }
    }
}
