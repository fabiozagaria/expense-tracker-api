package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long>{
}
