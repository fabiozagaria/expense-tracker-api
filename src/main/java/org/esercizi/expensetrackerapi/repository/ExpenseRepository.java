package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>{

    List<Expense> findAllByOwnerUsername(String username);

    Optional<Expense> findByIdAndOwnerUsername(long id, String username);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.owner.username = :username")
    BigDecimal sumAmountByOwnerUsername(@Param("username") String username);
}
