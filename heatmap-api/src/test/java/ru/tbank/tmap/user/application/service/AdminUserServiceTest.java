package ru.tbank.tmap.user.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.user.domain.UserSearchCriteria;
import ru.tbank.tmap.user.domain.UserTestFactory;
import ru.tbank.tmap.user.domain.exception.UserAlreadyBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotBlockedException;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final UUID USER_ID = UserTestFactory.DEFAULT_ID;

    @Mock
    private UserRepository userRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository);
    }

    @Test
    void search_whenCriteriaProvided_thenDelegatesToRepository() {
        final UserSearchCriteria criteria = new UserSearchCriteria(
                "Tatarin",
                null,
                null,
                null,
                null,
                null
        );

        final Pageable pageable = PageRequest.of(0, 20);
        final User user = UserTestFactory.createUser();
        final Page<User> expected = new PageImpl<>(java.util.List.of(user), pageable, 1);
        given(userRepository.search(criteria, pageable)).willReturn(expected);

        final Page<User> result = adminUserService.search(criteria, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void block_whenUserIsNotBlocked_thenBlockUser() {
        final User user = UserTestFactory.createUser(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        final User result = adminUserService.block(USER_ID);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result).isSameAs(user);
    }

    @Test
    void block_whenUserDoesNotExist_thenReturnNotFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.block(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void block_whenUserIsAlreadyBlocked_thenReturnConflict() {
        final User user = UserTestFactory.createUser(true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.block(USER_ID))
                .isInstanceOf(UserAlreadyBlockedException.class);
    }

    @Test
    void unblock_whenUserIsBlocked_thenUnblockUser() {
        final User user = UserTestFactory.createUser(true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        final User result = adminUserService.unblock(USER_ID);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result).isSameAs(user);
    }

    @Test
    void unblock_whenUserDoesNotExist_thenReturnNotFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.unblock(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void unblock_whenUserIsNotBlocked_thenReturnConflict() {
        final User user = UserTestFactory.createUser(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.unblock(USER_ID))
                .isInstanceOf(UserNotBlockedException.class);
    }
}
