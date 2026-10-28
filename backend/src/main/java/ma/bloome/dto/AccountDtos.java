package ma.bloome.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ma.bloome.validation.PhoneNumber;
import ma.bloome.validation.StrongPassword;

import java.time.Instant;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Le prénom est requis.") @Size(max = 80) String firstName,
            @NotBlank(message = "Le nom est requis.") @Size(max = 80) String lastName,
            @NotBlank(message = "L'e-mail est requis.") @Email(message = "Adresse e-mail invalide.") @Size(max = 255) String email,
            @StrongPassword String password,
            @PhoneNumber @Size(max = 30) String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "L'e-mail est requis.") @Size(max = 255) String email,
            @NotBlank(message = "Le mot de passe est requis.") @Size(max = 72) String password
    ) {
    }

    /** Never contains the password hash. */
    public record UserDto(Long id, String email, String firstName, String lastName, String phone,
                          String role, Instant createdAt) {
    }

    public record ProfileUpdateRequest(
            @NotBlank(message = "Le prénom est requis.") @Size(max = 80) String firstName,
            @NotBlank(message = "Le nom est requis.") @Size(max = 80) String lastName,
            @PhoneNumber @Size(max = 30) String phone
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Le mot de passe actuel est requis.") @Size(max = 72) String currentPassword,
            @StrongPassword String newPassword
    ) {
    }

    public record AddressRequest(
            @NotBlank(message = "Donnez un nom à cette adresse.") @Size(max = 60) String label,
            @NotBlank(message = "Le nom du destinataire est requis.") @Size(max = 120) String recipientName,
            @NotBlank(message = "Le téléphone est requis.") @PhoneNumber @Size(max = 30) String phone,
            @NotBlank(message = "L'adresse est requise.") @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank(message = "La ville est requise.") @Size(max = 80) String city,
            @Size(max = 12) String postalCode,
            boolean defaultAddress
    ) {
    }

    public record AddressDto(Long id, String label, String recipientName, String phone, String line1,
                             String line2, String city, String postalCode, boolean defaultAddress) {
    }

    public record NewsletterRequest(
            @NotBlank(message = "L'e-mail est requis.") @Email(message = "Adresse e-mail invalide.") @Size(max = 255) String email
    ) {
    }

    public record MessageResponse(String message) {
    }
}
