package ma.bloome.security;

import ma.bloome.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalized)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
