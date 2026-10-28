package ma.bloome.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import ma.bloome.repository.UserRepository;
import ma.bloome.security.AppUserDetails;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base class for full-stack integration tests: real HTTP layer (MockMvc), real security
 * filters, real transactions and a real PostgreSQL database migrated by Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(IntegrationTest.CleanDatabase.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JdbcTemplate jdbc;

    @TestConfiguration
    public static class CleanDatabase {
        /** Fresh schema and demo catalog once per Spring context (i.e. once per test run). */
        @Bean
        public FlywayMigrationStrategy cleanMigrate() {
            return (Flyway flyway) -> {
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    protected User createUser(Role role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.bloome");
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFirstName("Test");
        user.setLastName(role == Role.ADMIN ? "Admin" : "Client");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    protected RequestPostProcessor as(User user) {
        AppUserDetails details = new AppUserDetails(user);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }

    protected MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
        return post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected MockHttpServletRequestBuilder putJson(String url, Object body) throws Exception {
        return put(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected MockHttpServletRequestBuilder patchJson(String url, Object body) throws Exception {
        return patch(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected String toJson(Object body) throws Exception {
        return body instanceof String s ? s : json.writeValueAsString(body);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected long productId(String slug) {
        return jdbc.queryForObject("select id from products where slug = ?", Long.class, slug);
    }

    protected int productStock(long id) {
        return jdbc.queryForObject("select stock_quantity from products where id = ?", Integer.class, id);
    }

    protected void setProductStock(long id, int stock) {
        jdbc.update("update products set stock_quantity = ?, version = version + 1 where id = ?", stock, id);
    }

    protected int stemStock(long id) {
        return jdbc.queryForObject("select stock_quantity from bouquet_stems where id = ?", Integer.class, id);
    }

    /** A delivery date that is always valid (two days from now in the shop's time zone). */
    protected static LocalDate validDeliveryDate() {
        return LocalDate.now(ZoneId.of("Africa/Casablanca")).plusDays(2);
    }

    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    protected MockHttpServletRequestBuilder deleteReq(String url) {
        return delete(url).with(csrf());
    }
}
