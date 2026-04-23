package ru.tbank.tmap.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.domain.user.RefreshToken;

import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
}
