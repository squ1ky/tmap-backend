package ru.tbank.tmap.infrastructure.security;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.tbank.tmap.auth.application.port.TokenIssuer;

@TestConfiguration
@Import(SecurityConfig.class)
public class TestSecurityConfig {

    @Bean
    public TokenIssuer mockTokenIssuer() {
        return Mockito.mock(TokenIssuer.class);
    }

    @Bean
    public UserDetailsService mockUserDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    public AuthenticationProvider mockAuthenticationProvider() {
        return Mockito.mock(AuthenticationProvider.class);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> new CorsConfiguration().applyPermitDefaultValues();
    }
}