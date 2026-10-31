package org.esercizi.expensetrackerapi.services;

import org.esercizi.expensetrackerapi.dto.MovementPage;
import org.esercizi.expensetrackerapi.dto.income.IncomeUpdateRequest;
import org.esercizi.expensetrackerapi.dto.income.IncomePatchRequest;
import org.esercizi.expensetrackerapi.exceptions.NotFoundIncomeException;
import org.esercizi.expensetrackerapi.mapper.IncomeMapper;

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
    private final IncomeMapper incomeMapper;

    @Transactional(readOnly = true)
    public MovementPage<IncomeResponse> search(String username, String month, String query, int page, int size, String order) {
        var result = incomeRepository.findAll(MovementSearch.<Income>owned(username, month, query), MovementSearch.page(page, size, order));
        return MovementPage.from(result.map(this::toIncomeResponse));
    }

    public List<Income> findAllOwner(String username) {
        return incomeRepository.findAllByOwnerUsername(username);
    }

    @Transactional
    public Income createIncome(IncomeCreateRequest request, User user) {
        Income income = incomeMapper.toEntity(request, user);
        entityManager.persist(income);
        return income;
    }

    public Income findByIdOwner(long id, String username) {
        return incomeRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundIncomeException("Income not found"));
    }

    @Transactional
    public Income putIncomeById(long id, IncomeUpdateRequest request, String username) {
        Income income = findByIdOwner(id, username);
        incomeMapper.updateFromRequest(request, income);
        return income;
    }

    @Transactional
    public Income patchIncomeById(long id, IncomePatchRequest request, String username) {
        Income income = findByIdOwner(id, username);
        incomeMapper.patchFromRequest(request, income);
        return income;
    }

    @Transactional
    public Income deleteById(long id, String username) {
        Income income = findByIdOwner(id, username);
        entityManager.remove(income);
        return income;
    }

    public IncomeResponse toIncomeResponse(Income income) {
        return incomeMapper.toResponse(income);
    }
}
