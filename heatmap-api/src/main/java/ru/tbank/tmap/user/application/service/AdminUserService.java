package ru.tbank.tmap.user.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.user.domain.UserSearchCriteria;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<User> search(final UserSearchCriteria criteria, final Pageable pageable) {
        return userRepository.search(criteria, pageable);
    }

    @Transactional
    public User block(final UUID userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));
        user.block();
        return user;
    }

    @Transactional
    public User unblock(final UUID userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));
        user.unblock();
        return user;
    }
}
