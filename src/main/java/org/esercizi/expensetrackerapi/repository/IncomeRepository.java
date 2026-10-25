package org.esercizi.expensetrackerapi.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

import org.esercizi.expensetrackerapi.model.income.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {
    Optional<Income> findByIdAndOwnerUsername(long id, String username);

    List<Income> findAllByOwnerUsername(String username);

    @Query("select coalesce(sum(i.amount), 0) from Income i where i.owner.username = :username")
    BigDecimal sumAmountByOwnerUsername(@Param("username") String username);
}
