package ma.bloome.service;

import ma.bloome.dto.AccountDtos.*;
import ma.bloome.entity.Address;
import ma.bloome.entity.User;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.AccountMapper;
import ma.bloome.repository.AddressRepository;
import ma.bloome.repository.UserRepository;
import ma.bloome.validation.PhoneNumber;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountService {

    public static final int MAX_ADDRESSES = 10;

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper mapper;

    public AccountService(UserRepository userRepository, AddressRepository addressRepository,
                          PasswordEncoder passwordEncoder, AccountMapper mapper) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    public UserDto updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = user(userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : PhoneNumber.Validator.normalize(request.phone()));
        return mapper.toUser(userRepository.saveAndFlush(user));
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = user(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Le mot de passe actuel est incorrect.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("SAME_PASSWORD", "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<AddressDto> addresses(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(mapper::toAddress).toList();
    }

    public AddressDto createAddress(Long userId, AddressRequest request) {
        if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw ApiException.badRequest("ADDRESS_LIMIT", "Vous pouvez enregistrer jusqu'à " + MAX_ADDRESSES + " adresses.");
        }
        Address address = new Address();
        address.setUser(userRepository.getReferenceById(userId));
        boolean first = addressRepository.countByUserId(userId) == 0;
        apply(address, request);
        address.setDefaultAddress(request.defaultAddress() || first);
        address = addressRepository.saveAndFlush(address);
        if (address.isDefaultAddress()) {
            addressRepository.clearDefaultExcept(userId, address.getId());
        }
        return mapper.toAddress(address);
    }

    public AddressDto updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = owned(userId, addressId);
        apply(address, request);
        if (request.defaultAddress()) {
            address.setDefaultAddress(true);
            addressRepository.saveAndFlush(address);
            addressRepository.clearDefaultExcept(userId, address.getId());
        }
        return mapper.toAddress(address);
    }

    public void deleteAddress(Long userId, Long addressId) {
        Address address = owned(userId, addressId);
        addressRepository.delete(address);
    }

    private void apply(Address address, AddressRequest r) {
        address.setLabel(r.label().trim());
        address.setRecipientName(r.recipientName().trim());
        address.setPhone(PhoneNumber.Validator.normalize(r.phone()));
        address.setLine1(r.line1().trim());
        address.setLine2(r.line2() == null || r.line2().isBlank() ? null : r.line2().trim());
        address.setCity(r.city().trim());
        address.setPostalCode(r.postalCode() == null || r.postalCode().isBlank() ? null : r.postalCode().trim());
    }

    private Address owned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> ApiException.notFound("Adresse introuvable."));
    }

    private User user(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Compte introuvable."));
    }
}
