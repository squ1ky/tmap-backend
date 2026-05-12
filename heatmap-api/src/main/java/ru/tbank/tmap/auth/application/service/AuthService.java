package ru.tbank.tmap.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.application.AuthResult;
import ru.tbank.tmap.auth.application.port.TokenIssuer;
import ru.tbank.tmap.auth.application.port.UserAccountPort;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserAccountPort userAccountPort;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResult register(String email, String password, String nickname) {
        String passwordHash = passwordEncoder.encode(password);

        final UserView user = userAccountPort.register(email, passwordHash, nickname);

        return issueTokens(user);
    }

    public AuthResult login(String email, String password) {
        final UserView user = userAccountPort.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.blocked() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public AuthResult refresh(final String plainRefreshToken) {
        final UUID userId = refreshTokenService.validateAndRevoke(plainRefreshToken);
        final UserView user = userAccountPort.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    public void logout(final UUID userId, final String plainRefreshToken) {
        refreshTokenService.revokeSpecificToken(userId, plainRefreshToken);
    }

    private AuthResult issueTokens(UserView user) {
        final String plainRefreshToken = refreshTokenService.issue(user.id());
        final String accessToken = tokenIssuer.generateAccessToken(
                user.id(),
                user.email(),
                user.role().name()
        );

        return new AuthResult(
                user.id(),
                user.email(),
                user.nickname(),
                user.role().name(),
                accessToken,
                plainRefreshToken
        );
    }
}
