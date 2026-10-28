package ma.bloome.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.bloome.exception.ApiError;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Writes 401/403 responses using the same {@link ApiError} shape as the rest of the API. */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException ex) throws IOException {
        write(response, request, 401, "UNAUTHENTICATED", "Veuillez vous connecter pour continuer.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            write(response, request, 403, "CSRF_INVALID",
                    "Votre session a expiré. Rechargez la page et réessayez.");
        } else {
            write(response, request, 403, "FORBIDDEN", "Accès refusé.");
        }
    }

    public void write(HttpServletResponse response, HttpServletRequest request, int status,
                      String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new ApiError(Instant.now(), status, code, message, request.getRequestURI(), null));
    }
}
