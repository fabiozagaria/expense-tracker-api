package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.security.refresh.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findAllByIdAndUserUsername(Long id, String username);

    Optional<RefreshToken> findByIdAndUserUsername(Long id, String username);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca atomica: la condizione sullo stato vive nella UPDATE, non in un controllo
     * precedente in memoria. Con richieste concorrenti sullo stesso token una sola ottiene 1.
     */
    @Modifying
    @Query("update RefreshToken t set t.revokeAt = :now "
            + "where t.id = :id and t.revokeAt is null and t.expireAt > :now")
    int revokeIfActive(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expireAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /** Chiude tutte le sessioni dell'utente (es. dopo un cambio password). */
    @Modifying
    @Query("update RefreshToken t set t.revokeAt = :now "
            + "where t.user = :user and t.revokeAt is null")
    int revokeAllActiveByUser(@Param("user") User user, @Param("now") Instant now);
}
