package ru.tbank.tmap.shared.utils;

import java.util.Optional;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;

@NoArgsConstructor
public class SecurityUtils {

    public static CustomUserDetails getPrincipal() {
        return (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public static String currentUserEmail() {
        return getPrincipal().getUsername();
    }

    public static UUID currentUserId() {
        return getPrincipal().getUserId();
    }

    public static Optional<UUID> currentUserIdOptional() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return Optional.empty();
        }
        return Optional.of(principal.getUserId());
    }
}
