package ru.tbank.tmap.user.presentation;

import org.openapitools.model.AdminUserModerationPage;
import org.openapitools.model.AdminUserModerationResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.user.domain.User;

import java.util.List;

@Component
public class AdminUserMapper {

    public AdminUserModerationResponse toResponse(final User user) {
        return new AdminUserModerationResponse()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(toApiRole(user.getRole()))
                .blocked(user.isBlocked())
                .createdAt(user.getCreatedAt());
    }

    public AdminUserModerationPage toPage(final Page<User> page) {
        List<AdminUserModerationResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new AdminUserModerationPage()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements());
    }

    public ru.tbank.tmap.user.domain.UserRole toDomainRole(final UserRole role) {
        return role == null ? null : ru.tbank.tmap.user.domain.UserRole.valueOf(role.name());
    }

    private UserRole toApiRole(final ru.tbank.tmap.user.domain.UserRole role) {
        return UserRole.valueOf(role.name());
    }
}
