package ru.tbank.tmap.controller.auth;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.AuthApi;
import org.openapitools.model.AuthResponse;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.UserRole;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.config.security.cookie.RefreshTokenCookieFactory;
import ru.tbank.tmap.dto.auth.AuthResult;
import ru.tbank.tmap.service.auth.AuthService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Override
    public ResponseEntity<AuthResponse> registerUser(final RegisterRequest request) {
        final AuthResult result = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getNickname()
        );
        return buildAuthResponse(result, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<AuthResponse> loginUser(final LoginRequest request) {
        final AuthResult result = authService.login(request.getEmail(), request.getPassword());
        return buildAuthResponse(result, HttpStatus.OK);
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(final AuthResult result, final HttpStatus status) {
        final AuthResponse body = new AuthResponse()
                .userId(result.userId())
                .role(UserRole.fromValue(result.role().name()))
                .accessToken(result.accessToken());

        final ResponseCookie cookie = refreshTokenCookieFactory.create(result.plainRefreshToken());

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }
}
