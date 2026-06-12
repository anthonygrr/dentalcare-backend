package com.dentalcare.tony.api.infrastructure.security;

import com.dentalcare.tony.api.domain.entity.Permission;
import com.dentalcare.tony.api.domain.entity.Role;
import com.dentalcare.tony.api.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.getName().name()
                    )
            );

            for (Permission permission : role.getPermissions()) {
                authorities.add(
                        new SimpleGrantedAuthority(
                                permission.getName().name()
                        )
                );
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Spring Security usa este campo internamente.
     * Nosotros autenticamos por username o email.
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getEnabled());
    }
}
