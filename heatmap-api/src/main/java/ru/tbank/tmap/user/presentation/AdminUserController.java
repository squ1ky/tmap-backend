package ru.tbank.tmap.user.presentation;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.AdminUsersApi;
import org.openapitools.model.AdminUserModerationPage;
import org.openapitools.model.AdminUserModerationResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.user.application.service.AdminUserService;
import ru.tbank.tmap.user.domain.UserSearchCriteria;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminUserController implements AdminUsersApi {

    private final AdminUserService adminUserService;
    private final AdminUserMapper adminUserMapper;

    @Override
    public ResponseEntity<AdminUserModerationPage> searchAdminUsers(
            final String nickname,
            final String email,
            final UserRole role,
            final Boolean blocked,
            final OffsetDateTime createdFrom,
            final OffsetDateTime createdTo,
            final Integer page,
            final Integer size
    ) {
        final Pageable pageable = PageRequest.of(page, size);
        final UserSearchCriteria criteria = new UserSearchCriteria(
                nickname,
                email,
                adminUserMapper.toDomainRole(role),
                blocked,
                createdFrom,
                createdTo
        );
        return ResponseEntity.ok(adminUserMapper.toPage(adminUserService.search(criteria, pageable)));
    }

    @Override
    public ResponseEntity<AdminUserModerationResponse> blockAdminUser(final UUID id) {
        return ResponseEntity.ok(adminUserMapper.toResponse(adminUserService.block(id)));
    }

    @Override
    public ResponseEntity<AdminUserModerationResponse> unblockAdminUser(final UUID id) {
        return ResponseEntity.ok(adminUserMapper.toResponse(adminUserService.unblock(id)));
    }
}
