package ru.tbank.tmap.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    private final String testEmail = "security@tbank.ru";
    private final String testPasswordHash = "encoded_hash_123";

    @Test
    @DisplayName("Должен возвращать UserDetails с активным статусом, если пользователь найден и не заблокирован")
    void loadUserByUsername_whenUserExistsAndNotBlocked_thenReturnActiveUserDetails() {
        User mockUser = mock(User.class);
        given(mockUser.getEmail()).willReturn(testEmail);
        given(mockUser.getPasswordHash()).willReturn(testPasswordHash);
        given(mockUser.isBlocked()).willReturn(false);
        given(mockUser.getRole()).willReturn(UserRole.USER);

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testEmail);
        assertThat(userDetails.getPassword()).isEqualTo(testPasswordHash);
        assertThat(userDetails.isEnabled()).isTrue();

        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("Должен возвращать UserDetails с выключенным статусом, если пользователь заблокирован")
    void loadUserByUsername_whenUserIsBlocked_thenReturnDisabledUserDetails() {
        User mockUser = mock(User.class);
        given(mockUser.getEmail()).willReturn(testEmail);
        given(mockUser.getPasswordHash()).willReturn(testPasswordHash);
        given(mockUser.isBlocked()).willReturn(true);
        given(mockUser.getRole()).willReturn(UserRole.BUSINESS_OWNER);

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();

        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_BUSINESS_OWNER"));
    }

    @Test
    @DisplayName("Должен выбрасывать UsernameNotFoundException, если пользователь не найден")
    void loadUserByUsername_whenUserNotFound_thenThrowUsernameNotFoundException() {
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(testEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + testEmail);
    }
}