package ma.bloome.integration;

import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.security.AuthRateLimitFilter;
import ma.bloome.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndAccessIntegrationTest extends IntegrationTest {

    @Autowired
    ma.bloome.security.JsonSecurityHandlers securityHandlers;

    @Test
    @DisplayName("Registration creates a CUSTOMER (role in payload ignored), hashes the password and opens a session")
    void registersCustomer() throws Exception {
        String email = "New." + UUID.randomUUID() + "@Example.com";
        MvcResult result = mvc.perform(postJson("/api/v1/auth/register", Map.of(
                        "firstName", "Salma", "lastName", "Idrissi", "email", email,
                        "password", "Fleurs2026", "phone", "0612345678", "role", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        String hash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email.toLowerCase());
        assertThat(hash).startsWith("{bcrypt}").doesNotContain("Fleurs2026");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Salma"));
    }

    @Test
    @DisplayName("Duplicate e-mail → 409, weak password → 400 with field errors")
    void rejectsInvalidRegistrations() throws Exception {
        User existing = createUser(Role.CUSTOMER);
        mvc.perform(postJson("/api/v1/auth/register", Map.of("firstName", "A", "lastName", "B",
                        "email", existing.getEmail().toUpperCase(), "password", "Password123")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        mvc.perform(postJson("/api/v1/auth/register", Map.of("firstName", "A", "lastName", "B",
                        "email", "weak-" + UUID.randomUUID() + "@x.ma", "password", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    @DisplayName("Login succeeds with correct credentials, fails generically otherwise; logout ends the session")
    void loginAndLogout() throws Exception {
        User user = createUser(Role.CUSTOMER);
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", user.getEmail(), "password", "wrong-password1")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "nobody-" + UUID.randomUUID() + "@x.ma", "password", "Password123")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        MvcResult login = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", user.getEmail(), "password", "Password123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail())).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/v1/cart").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("State-changing requests without a CSRF token are refused")
    void csrfRequired() throws Exception {
        User user = createUser(Role.CUSTOMER);
        mvc.perform(post("/api/v1/cart/items").with(as(user)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    @DisplayName("Unauthenticated visitors cannot reach customer or admin endpoints")
    void anonymousIsRejected() throws Exception {
        for (String url : new String[]{"/api/v1/cart", "/api/v1/orders/my", "/api/v1/orders/1", "/api/v1/wishlist",
                "/api/v1/account/addresses", "/api/v1/admin/dashboard", "/api/v1/admin/orders", "/api/v1/admin/customers"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A customer cannot access administrator APIs; an administrator can")
    void roleBasedAccess() throws Exception {
        User customer = createUser(Role.CUSTOMER);
        User admin = createUser(Role.ADMIN);
        mvc.perform(get("/api/v1/admin/dashboard").with(as(customer))).andExpect(status().isForbidden());
        mvc.perform(postJson("/api/v1/admin/products", Map.of()).with(as(customer))).andExpect(status().isForbidden());
        mvc.perform(patchJson("/api/v1/admin/orders/1/status", Map.of("status", "DELIVERED")).with(as(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/dashboard").with(as(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueDefinition").isNotEmpty());
    }

    @Test
    @DisplayName("Credential endpoints are rate limited per client IP")
    void loginRateLimited() throws Exception {
        var properties = new ma.bloome.config.BloomeProperties(null, null,
                new ma.bloome.config.BloomeProperties.RateLimit(3), null, null);
        var filter = new AuthRateLimitFilter(properties, securityHandlers, java.time.Clock.systemUTC());
        int[] statuses = new int[5];
        for (int i = 0; i < 5; i++) {
            var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("10.9.8.7");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(request, response, new org.springframework.mock.web.MockFilterChain());
            statuses[i] = response.getStatus();
        }
        assertThat(statuses).containsExactly(200, 200, 200, 429, 429);

        var other = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/auth/login");
        other.setRemoteAddr("10.9.8.8");
        var otherResponse = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(other, otherResponse, new org.springframework.mock.web.MockFilterChain());
        assertThat(otherResponse.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Errors never leak stack traces or internal messages")
    void safeErrors() throws Exception {
        mvc.perform(postJson("/api/v1/auth/login", "{not json").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
