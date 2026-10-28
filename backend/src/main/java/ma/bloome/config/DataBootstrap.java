package ma.bloome.config;

import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates accounts at startup without hard-coding any production secret:
 * <ul>
 *   <li>An administrator, only when BLOOME_ADMIN_EMAIL and BLOOME_ADMIN_PASSWORD are set.</li>
 *   <li>Local demo accounts, only when BLOOME_DEMO_ACCOUNTS=true (development only).</li>
 * </ul>
 * Existing accounts are never modified.
 */
@Component
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    /** Development-only credentials, documented in the README. Never enable demo accounts in production. */
    static final String DEMO_ADMIN_EMAIL = "admin@bloome.local";
    static final String DEMO_ADMIN_PASSWORD = "BloomeAdmin2026";
    static final String DEMO_CUSTOMER_EMAIL = "client@bloome.local";
    static final String DEMO_CUSTOMER_PASSWORD = "BloomeClient2026";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BloomeProperties properties;

    public DataBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder, BloomeProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        BloomeProperties.Bootstrap bootstrap = properties.bootstrap();
        if (notBlank(bootstrap.adminEmail()) && notBlank(bootstrap.adminPassword())) {
            if (bootstrap.adminPassword().length() < 12) {
                log.warn("BLOOME_ADMIN_PASSWORD is shorter than 12 characters; administrator not created.");
            } else {
                createIfMissing(bootstrap.adminEmail(), bootstrap.adminPassword(), "Admin", "Bloomé", Role.ADMIN);
            }
        }
        if (bootstrap.demoAccounts()) {
            log.warn("BLOOME_DEMO_ACCOUNTS=true — creating development demo accounts. Do NOT use in production.");
            createIfMissing(DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD, "Salma", "Admin", Role.ADMIN);
            createIfMissing(DEMO_CUSTOMER_EMAIL, DEMO_CUSTOMER_PASSWORD, "Yasmine", "Alaoui", Role.CUSTOMER);
        }
    }

    private void createIfMissing(String email, String rawPassword, String firstName, String lastName, Role role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        userRepository.save(user);
        log.info("Bootstrap account created: {} ({})", normalized, role);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
