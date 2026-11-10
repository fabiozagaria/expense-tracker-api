package org.esercizi.expensetrackerapi.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense>{

    /** Slice: Spring legge una riga in più del limite e così sa se esistono altre spese, senza una COUNT. */
    Slice<Expense> findByOwnerUsername(String username, Pageable pageable);

    Optional<Expense> findByIdAndOwnerUsername(long id, String username);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.owner.username = :username")
    BigDecimal sumAmountByOwnerUsername(@Param("username") String username);
}
