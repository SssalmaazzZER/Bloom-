package ma.bloome.mapper;

import ma.bloome.dto.AccountDtos.AddressDto;
import ma.bloome.dto.AccountDtos.UserDto;
import ma.bloome.entity.Address;
import ma.bloome.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public UserDto toUser(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getRole().name(), u.getCreatedAt());
    }

    public AddressDto toAddress(Address a) {
        return new AddressDto(a.getId(), a.getLabel(), a.getRecipientName(), a.getPhone(), a.getLine1(),
                a.getLine2(), a.getCity(), a.getPostalCode(), a.isDefaultAddress());
    }
}
