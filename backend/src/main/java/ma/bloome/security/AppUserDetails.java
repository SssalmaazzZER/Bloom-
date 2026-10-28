package ma.bloome.security;

import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Principal stored in the HTTP session. Holds only what authorisation needs
 * (id, email, role); the password hash is erased after authentication.
 */
public class AppUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final Role role;
    private final boolean enabled;
    private String passwordHash;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.passwordHash = user.getPasswordHash();
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
