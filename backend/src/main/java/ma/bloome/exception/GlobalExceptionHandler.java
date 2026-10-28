package ma.bloome.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Translates exceptions into {@link ApiError} bodies. Internal details (stack traces,
 * SQL, exception messages) are logged server-side and never returned to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        List<ApiError.FieldViolation> global = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new ApiError.FieldViolation(ge.getObjectName(), ge.getDefaultMessage()))
                .toList();
        List<ApiError.FieldViolation> all = new java.util.ArrayList<>(fields);
        all.addAll(global);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Certaines informations sont invalides. Merci de vérifier le formulaire.", request, all);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiError> handleConstraint(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Paramètres de requête invalides.", request, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadInput(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "La requête est mal formée.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Méthode non autorisée.", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Format de contenu non supporté.", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ressource introuvable.", request, null);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> handleBadCredentials(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou mot de passe incorrect.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Veuillez vous connecter.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Accès refusé.", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Cet élément a été modifié entre-temps. Rechargez la page et réessayez.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "L'opération entre en conflit avec des données existantes.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Une erreur inattendue est survenue. Merci de réessayer dans un instant.", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request, List<ApiError.FieldViolation> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(),
                fields == null || fields.isEmpty() ? null : fields);
        return ResponseEntity.status(status).body(body);
    }
}
