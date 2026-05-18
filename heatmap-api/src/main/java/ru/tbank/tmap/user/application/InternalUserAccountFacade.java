package ru.tbank.tmap.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.api.exception.EmailAlreadyExistsException;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalUserAccountFacade implements UserAccountFacade {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(final UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findById(UUID id) {
        return userRepository.findById(id).map(this::toView);
    }

    @Override
    public UserView register(String email, String passwordHash, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        final User user = User.create(email, passwordHash, nickname);
        return toView(userRepository.save(user));
    }

    @Override
    public UserView promoteToBusinessOwner(UUID id) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.byId(id));
        user.promoteToBusinessOwner();
        return toView(user);
    }

    @Override
    public UserView updatePasswordHash(final UUID id, final String passwordHash) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.byId(id));
        user.setPasswordHash(passwordHash);
        return toView(user);
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getRole(),
                user.isBlocked()
        );
    }
}
