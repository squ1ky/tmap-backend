package ru.tbank.tmap.shared.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtils {

    public static String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
