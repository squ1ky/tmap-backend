package ru.tbank.tmap.test.security;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.tbank.tmap.auth.application.port.TokenIssuer;
import ru.tbank.tmap.infrastructure.security.SecurityConfig;

@TestConfiguration
@Import(SecurityConfig.class)
public class TestSecurityConfig {

    @Bean
    @Primary
    public TokenIssuer mockTokenIssuer() {
        return Mockito.mock(TokenIssuer.class);
    }

    @Bean
    @Primary
    public UserDetailsService mockUserDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    @Primary
    public AuthenticationProvider mockAuthenticationProvider() {
        return Mockito.mock(AuthenticationProvider.class);
    }

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> new CorsConfiguration().applyPermitDefaultValues();
    }
}