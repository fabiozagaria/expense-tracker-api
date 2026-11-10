package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.PasswordResetToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserOrderByCreateAtDesc(User user);

    void deleteByUser(User user);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expireAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /**
     * Consumo atomico: la condizione "non usato e non scaduto" vive nella UPDATE.
     * Con due richieste concorrenti sullo stesso link, una sola ottiene 1.
     */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now "
            + "where t.id = :id and t.usedAt is null and t.expireAt > :now")
    int consumeIfActive(@Param("id") Long id, @Param("now") Instant now);
}
