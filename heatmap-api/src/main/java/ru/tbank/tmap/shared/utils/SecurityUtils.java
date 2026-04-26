package ru.tbank.tmap.shared.utils;

import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.tbank.tmap.auth.userdetails.CustomUserDetails;

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
}
