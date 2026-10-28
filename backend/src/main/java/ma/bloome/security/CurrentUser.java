package ma.bloome.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Small helper to read the authenticated principal outside of controller parameters. */
public final class CurrentUser {
    private CurrentUser() {
    }

    public static Optional<AppUserDetails> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return auth.getPrincipal() instanceof AppUserDetails details ? Optional.of(details) : Optional.empty();
    }

    public static Optional<Long> id() {
        return get().map(AppUserDetails::getId);
    }
}
