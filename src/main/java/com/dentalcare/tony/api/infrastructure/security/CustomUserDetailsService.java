package com.dentalcare.tony.api.infrastructure.security;

import com.dentalcare.tony.api.domain.entity.User;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final JpaUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameOrEmail(login, login)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + login
                        ));

        return UserPrincipal.from(user);
    }
}
