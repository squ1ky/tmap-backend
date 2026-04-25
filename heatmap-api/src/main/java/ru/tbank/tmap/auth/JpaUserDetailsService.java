package ru.tbank.tmap.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.user.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final boolean ACCOUNT_NON_EXPIRED = true;
    private static final boolean CREDENTIALS_NON_EXPIRED = true;
    private static final boolean ACCOUNT_NON_LOCKED = true;

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String email) {
        return userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash(),
                        !user.isBlocked(),
                        ACCOUNT_NON_EXPIRED,
                        CREDENTIALS_NON_EXPIRED,
                        ACCOUNT_NON_LOCKED,
                        List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
