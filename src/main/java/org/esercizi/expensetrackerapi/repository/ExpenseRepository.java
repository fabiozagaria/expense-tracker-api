package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>{

    List<Expense> findAllByOwnerUsername(String username);

    Optional<Expense> findByIdAndOwnerUsername(long id, String username);
}
