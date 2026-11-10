package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.EmailVerificationToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String hashToken);

    EmailVerificationToken findByUserUsername(String name);

    Optional<EmailVerificationToken> findFirstByUserOrderByCreateAtDesc(User user);

    void deleteByUser(User user);

    @Modifying
    @Query("delete from EmailVerificationToken t where t.expireAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
