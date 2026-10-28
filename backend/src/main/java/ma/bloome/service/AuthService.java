package ma.bloome.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import ma.bloome.dto.AccountDtos.LoginRequest;
import ma.bloome.dto.AccountDtos.RegisterRequest;
import ma.bloome.dto.AccountDtos.UserDto;
import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.exception.ApiException;
import ma.bloome.mapper.AccountMapper;
import ma.bloome.repository.UserRepository;
import ma.bloome.security.AppUserDetails;
import ma.bloome.validation.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AccountMapper mapper;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository, AccountMapper mapper,
                       CsrfTokenRepository csrfTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.mapper = mapper;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /** Registers a CUSTOMER (the role is never taken from the request) and signs them in. */
    @Transactional
    public UserDto register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Un compte existe déjà avec cette adresse e-mail.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : PhoneNumber.Validator.normalize(request.phone()));
        user.setRole(Role.CUSTOMER);
        userRepository.saveAndFlush(user);
        log.info("New customer registered: id={}", user.getId());
        establishSession(new AppUserDetails(user), httpRequest, httpResponse);
        return mapper.toUser(user);
    }

    @Transactional(readOnly = true)
    public UserDto login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizeEmail(request.email()), request.password()));
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        establishSession(principal, httpRequest, httpResponse);
        return userRepository.findById(principal.getId()).map(mapper::toUser)
                .orElseThrow(() -> ApiException.notFound("Compte introuvable."));
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        rotateCsrfToken(request, response);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        contextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return userRepository.findById(userId).map(mapper::toUser)
                .orElseThrow(() -> ApiException.notFound("Compte introuvable."));
    }

    private void establishSession(AppUserDetails principal, HttpServletRequest request, HttpServletResponse response) {
        principal.eraseCredentials();
        // Session fixation protection: a fresh session id is issued at every login.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        }
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(auth);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        rotateCsrfToken(request, response);
    }

    /**
     * Issues a fresh CSRF token whenever the authentication state changes (as Spring Security's
     * CsrfAuthenticationStrategy does for form login), so a token obtained before login cannot be reused.
     */
    private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken fresh = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(fresh, request, response);
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
