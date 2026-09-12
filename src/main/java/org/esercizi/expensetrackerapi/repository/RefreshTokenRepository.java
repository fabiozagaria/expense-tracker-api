package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.security.refresh.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findAllByIdAndUserUsername(Long id, String username);

    Optional<RefreshToken> findByIdAndUserUsername(Long id, String username);

    Optional<RefreshToken> findByRefreshToken(String tokenHash);
}
