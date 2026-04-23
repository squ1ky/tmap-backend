package ru.tbank.tmap.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
