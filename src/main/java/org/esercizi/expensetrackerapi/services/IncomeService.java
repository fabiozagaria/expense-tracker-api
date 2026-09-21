package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.income.IncomeCreateRequest;
import org.esercizi.expensetrackerapi.dto.income.IncomeResponse;
import org.esercizi.expensetrackerapi.model.income.Income;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {
    private final IncomeRepository incomeRepository;
    private final EntityManager entityManager;

    public List<Income> findAllOwner(String username) {
        return incomeRepository.findAllByOwnerUsername(username);
    }

    @Transactional
    public Income createIncome(IncomeCreateRequest request, User user) {
        Income income = new Income(
                null,
                request.title(),
                request.amount(),
                request.description() == null ? null : request.description().trim(),
                request.category(),
                request.date(),
                user
        );
        entityManager.persist(income);
        return income;
    }

    public IncomeResponse toIncomeResponse(Income income) {
        return new IncomeResponse(
                income.getId(),
                income.getTitle(),
                income.getAmount(),
                income.getDescription(),
                income.getCategory(),
                income.getDate()
        );
    }
}
