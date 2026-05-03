package ru.tbank.tmap.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.auth.domain.exception.EmailAlreadyExistsException;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.auth.refresh.RefreshTokenService;
import ru.tbank.tmap.auth.jwt.JwtService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthResult register(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        final User user = User.create(email, passwordEncoder.encode(password), nickname);
        userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isBlocked() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public AuthResult refresh(final String plainRefreshToken) {
        return refreshTokenService.rotate(plainRefreshToken);
    }

    public void logout(final UUID userId, final String plainRefreshToken) {
        refreshTokenService.revokeSpecificToken(userId, plainRefreshToken);
    }

    private AuthResult issueTokens(User user) {
        final String plainRefreshToken = refreshTokenService.issue(user);

        return new AuthResult(
                user.getId(),
                user.getRole(),
                jwtService.generateAccessToken(user),
                plainRefreshToken
        );
    }
}
