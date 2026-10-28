package ma.bloome.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ma.bloome.dto.AccountDtos.LoginRequest;
import ma.bloome.dto.AccountDtos.RegisterRequest;
import ma.bloome.dto.AccountDtos.UserDto;
import ma.bloome.exception.ApiException;
import ma.bloome.security.AppUserDetails;
import ma.bloome.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer account and open a session")
    public UserDto register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) {
        return authService.register(request, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with e-mail and password (sets the BLOOME_SESSION cookie)")
    public UserDto login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                         HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Invalidate the current session")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user, or 401 when not signed in")
    public UserDto me(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Non connecté.");
        }
        return authService.me(principal.getId());
    }

    @GetMapping("/csrf")
    @Operation(summary = "Issue the XSRF-TOKEN cookie (call once before the first mutating request)")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok(Map.of("headerName", token.getHeaderName()));
    }
}
