package ru.tbank.tmap.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.user.application.service.AdminUserService;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRole;
import ru.tbank.tmap.user.domain.UserSearchCriteria;
import ru.tbank.tmap.user.domain.UserTestFactory;
import ru.tbank.tmap.user.domain.exception.UserAlreadyBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

@WebMvcTest(AdminUserController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        AdminUserMapper.class,
})
class AdminUserControllerTest {

    private static final UUID USER_ID = UserTestFactory.DEFAULT_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAdminUsers_whenUserIsAdmin_thenReturnUsers() throws Exception {
        final User user = UserTestFactory.createUser();
        final UserSearchCriteria criteria = new UserSearchCriteria(
                "Tatarin",
                null,
                null,
                null,
                null,
                null
        );
        given(adminUserService.search(criteria, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users/search")
                        .param("nickname", "Tatarin")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.items[0].nickname").value(UserTestFactory.DEFAULT_NICKNAME))
                .andExpect(jsonPath("$.items[0].email").value(UserTestFactory.DEFAULT_EMAIL))
                .andExpect(jsonPath("$.items[0].role").value("USER"))
                .andExpect(jsonPath("$.items[0].roles[0]").value("USER"))
                .andExpect(jsonPath("$.items[0].blocked").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchAdminUsers_whenUserIsNotAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/search"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockAdminUser_whenUserIsAdmin_thenReturnBlockedUser() throws Exception {
        final User blocked = UserTestFactory.createUser(true);
        given(adminUserService.block(USER_ID)).willReturn(blocked);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.blocked").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockAdminUser_whenUserIsNotAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockAdminUser_whenUserDoesNotExist_thenReturnNotFound() throws Exception {
        given(adminUserService.block(USER_ID))
                .willThrow(UserNotFoundException.byId(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockAdminUser_whenUserIsAlreadyBlocked_thenReturnConflict() throws Exception {
        given(adminUserService.block(USER_ID))
                .willThrow(new UserAlreadyBlockedException(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unblockAdminUser_whenUserIsAdmin_thenReturnUnblockedUser() throws Exception {
        final User unblocked = UserTestFactory.createUser(false);
        given(adminUserService.unblock(USER_ID)).willReturn(unblocked);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unblock", USER_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unblockAdminUser_whenUserDoesNotExist_thenReturnNotFound() throws Exception {
        given(adminUserService.unblock(USER_ID))
                .willThrow(UserNotFoundException.byId(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unblock", USER_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unblockAdminUser_whenUserIsNotBlocked_thenReturnConflict() throws Exception {
        given(adminUserService.unblock(USER_ID))
                .willThrow(new UserNotBlockedException(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unblock", USER_ID)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAdminUserRole_whenUserIsAdmin_thenReturnUpdatedUser() throws Exception {
        final User owner = UserTestFactory.createUser(UserRole.BUSINESS_OWNER, false);
        given(adminUserService.updateRole(USER_ID, UserRole.BUSINESS_OWNER)).willReturn(owner);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", USER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "BUSINESS_OWNER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.role").value("BUSINESS_OWNER"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.roles[1]").value("BUSINESS_OWNER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateAdminUserRole_whenUserIsNotAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", USER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "BUSINESS_OWNER"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAdminUserRole_whenUserDoesNotExist_thenReturnNotFound() throws Exception {
        given(adminUserService.updateRole(USER_ID, UserRole.BUSINESS_OWNER))
                .willThrow(UserNotFoundException.byId(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", USER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "BUSINESS_OWNER"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAdminUserRole_whenRoleIsInvalid_thenReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", USER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
