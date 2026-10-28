package ma.bloome.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.bloome.config.BloomeProperties;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-client-IP limiter for credential endpoints (login, register, password change).
 * In-memory and per instance: adequate for a single node. In production behind several
 * instances, enforce limits at the reverse proxy / API gateway (see README).
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/account/password"
    );
    private static final long WINDOW_MILLIS = 60_000;

    private final int limit;
    private final JsonSecurityHandlers handlers;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(BloomeProperties properties, JsonSecurityHandlers handlers, Clock clock) {
        this.limit = Math.max(1, properties.rateLimit().authRequestsPerMinute());
        this.handlers = handlers;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) && !"PUT".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Window window = windows.compute(key, (k, w) -> (w == null || now - w.start >= WINDOW_MILLIS)
                ? new Window(now, 1) : new Window(w.start, w.count + 1));
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= WINDOW_MILLIS);
        }
        if (window.count > limit) {
            long retryAfter = Math.max(1, (WINDOW_MILLIS - (now - window.start)) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            handlers.write(response, request, 429, "TOO_MANY_REQUESTS",
                    "Trop de tentatives. Merci de patienter une minute avant de réessayer.");
            return;
        }
        chain.doFilter(request, response);
    }

    public void reset() {
        windows.clear();
    }

    private record Window(long start, int count) {
    }
}
