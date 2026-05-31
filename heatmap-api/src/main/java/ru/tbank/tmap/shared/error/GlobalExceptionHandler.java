package ru.tbank.tmap.shared.error;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.auth.application.exception.PasswordChangeValidationException;
import ru.tbank.tmap.auth.infrastructure.security.cookie.RefreshTokenCookieFactory;
import ru.tbank.tmap.user.api.exception.EmailAlreadyExistsException;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;
import ru.tbank.tmap.heatmap.domain.exception.ClusterNotFoundException;
import ru.tbank.tmap.loyalty.application.exception.VenueAccessDeniedException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleStateException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleUpdateValidationException;
import ru.tbank.tmap.user.domain.exception.UserAlreadyBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.venue.domain.exception.InvalidVenuePhotoException;
import ru.tbank.tmap.venue.domain.exception.VenueDeletionConflictException;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;

@Slf4j
@RestControllerAdvice
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class GlobalExceptionHandler {

    @ExceptionHandler(ClusterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClusterNotFound(final ClusterNotFoundException ex) {
        log.warn("Cluster not found: h3Index={}", ex.getH3Index());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(VenueNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVenueNotFound(final VenueNotFoundException ex) {
        log.warn("Venue not found: venueId={}", ex.getVenueId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(VenueModerationStateException.class)
    public ResponseEntity<ErrorResponse> handleVenueModerationState(final VenueModerationStateException ex) {
        log.warn("Venue moderation conflict: venueId={}, status={}", ex.getVenueId(), ex.getStatus());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(VenueDeletionConflictException.class)
    public ResponseEntity<ErrorResponse> handleVenueDeletionConflict(final VenueDeletionConflictException ex) {
        log.warn("Venue deletion conflict: venueId={}", ex.getVenueId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidVenuePhotoException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVenuePhoto(final InvalidVenuePhotoException ex) {
        log.warn("Invalid venue photo: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_FILE, ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(final MaxUploadSizeExceededException ex) {
        log.warn("Upload exceeds maximum allowed size");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(
                        ErrorCode.INVALID_FILE,
                        "File too large. Maximum size is 10 MB."));
    }

    @ExceptionHandler(LoyaltyRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoyaltyRuleNotFound(final LoyaltyRuleNotFoundException ex) {
        log.warn("Loyalty rule not found: ruleId={}", ex.getRuleId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(LoyaltyRuleStateException.class)
    public ResponseEntity<ErrorResponse> handleLoyaltyRuleState(final LoyaltyRuleStateException ex) {
        log.warn("Loyalty rule conflict: ruleId={}, message={}", ex.getRuleId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(VenueAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleVenueAccessDenied(final VenueAccessDeniedException ex) {
        log.warn("Venue access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ErrorCode.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(LoyaltyRuleUpdateValidationException.class)
    public ResponseEntity<ErrorResponse> handleLoyaltyRuleUpdateValidation(
            final LoyaltyRuleUpdateValidationException ex
    ) {
        log.warn("Loyalty rule update validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(final ResponseStatusException ex) {
        final HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        final String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        if (status.is4xxClientError()) {
            log.warn("Client error: status={}, message={}", status.value(), message);
        } else {
            log.error("Response status exception", ex);
        }
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ErrorCode.valueOf(status.name()), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        final String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        message.isBlank() ? "Validation failed" : message
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException ex) {
        final String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        message.isBlank() ? "Validation failed" : message
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        "Invalid value for parameter: " + ex.getName()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(final HttpMessageNotReadableException ex) {
        log.warn("Request body parse error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, "Malformed request body"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(PasswordChangeValidationException.class)
    public ResponseEntity<ErrorResponse> handlePasswordChangeValidation(
            final PasswordChangeValidationException ex
    ) {
        log.warn("Password change validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ErrorResponse> handleArithmeticException(final ArithmeticException ex) {
        log.error("Numeric overflow or precision loss", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_ERROR,
                        "Numeric value exceeds supported range"
                ));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(final EmailAlreadyExistsException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(final InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ErrorCode.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(final UserNotFoundException ex) {
        log.warn("User not found: id={} email={}", ex.getId(), ex.getEmail());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyBlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyBlocked(final UserAlreadyBlockedException ex) {
        log.warn("User already blocked: id={}", ex.getId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(UserNotBlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotBlocked(final UserNotBlockedException ex) {
        log.warn("User is not blocked: id={}", ex.getId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(final InvalidRefreshTokenException ex) {
        log.warn("Invalid refresh token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ErrorCode.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestCookie(final MissingRequestCookieException ex) {
        String cookieName = ex.getCookieName();
        log.warn("Missing required cookie: {}", cookieName);

        String message = String.format("Required cookie '%s' is missing", cookieName);

        if (RefreshTokenCookieFactory.COOKIE_NAME.equals(cookieName)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(ErrorCode.UNAUTHORIZED, message));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR, "Something went wrong"));
    }
}
