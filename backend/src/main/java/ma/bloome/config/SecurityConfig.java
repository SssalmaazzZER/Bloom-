package ma.bloome.config;

import ma.bloome.security.AppUserDetailsService;
import ma.bloome.security.AuthRateLimitFilter;
import ma.bloome.security.JsonSecurityHandlers;
import ma.bloome.security.SpaCsrfTokenRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

/**
 * Authentication strategy: server-side HTTP session identified by an HttpOnly, SameSite=Lax
 * cookie (BLOOME_SESSION), with CSRF protection via the double-submit XSRF-TOKEN cookie.
 * No token is ever stored in browser storage. Authorization is enforced here and in services.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Delegating encoder: BCrypt today, upgradable without invalidating stored hashes.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager manager = new ProviderManager(provider);
        manager.setEraseCredentialsAfterAuthentication(true);
        return manager;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuthRateLimitFilter authRateLimitFilter(BloomeProperties properties, JsonSecurityHandlers handlers, Clock clock) {
        return new AuthRateLimitFilter(properties, handlers, clock);
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<AuthRateLimitFilter> rateLimitFilterRegistration(
            AuthRateLimitFilter filter) {
        // Registered inside the security chain only (avoid double execution as a servlet filter).
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** XSRF-TOKEN cookie readable by the SPA (double-submit pattern), SameSite=Lax. */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(c -> c.sameSite("Lax").path("/"));
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonSecurityHandlers handlers,
                                                   SecurityContextRepository securityContextRepository,
                                                   CsrfTokenRepository csrfRepository,
                                                   AuthRateLimitFilter rateLimitFilter) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Infrastructure & docs
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Authentication
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me", "/api/v1/auth/csrf").permitAll()
                        // Public catalog
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**",
                                "/api/v1/categories", "/api/v1/occasions", "/api/v1/catalog/facets",
                                "/api/v1/bouquet/options", "/api/v1/delivery/options").permitAll()
                        // Anonymous cart pricing & newsletter
                        .requestMatchers(HttpMethod.POST, "/api/v1/cart/quote", "/api/v1/newsletter").permitAll()
                        // Administration
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Everything else (cart, wishlist, orders, account, reviews POST) requires a login
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(BloomeProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
