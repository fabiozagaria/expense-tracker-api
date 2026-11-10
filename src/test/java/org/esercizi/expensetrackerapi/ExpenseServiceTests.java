package org.esercizi.expensetrackerapi;

import jakarta.persistence.EntityManager;
import org.esercizi.expensetrackerapi.cache.OwnerExpenseCache;
import org.esercizi.expensetrackerapi.cache.OwnerExpensesChanged;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.exceptions.NotFoundExpenseException;
import org.esercizi.expensetrackerapi.mapper.ExpenseMapperImpl;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.model.expense.ExpenseCategory;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.esercizi.expensetrackerapi.services.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTests {
    final ExpenseRepository repository = mock(ExpenseRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final ExpenseService service = new ExpenseService(
            repository, new ExpenseMapperImpl(), em, mock(OwnerExpenseCache.class), events);

    Expense existing() {
        return Expense.builder().id(7L).title("Vecchio").amount(BigDecimal.TEN).description("old")
                .category(ExpenseCategory.other).date(LocalDate.now().minusDays(2)).build();
    }

    @Test
    void createAssignsOwnerFromTheServerSidePrincipalAndNotifiesCache() {
        User owner = User.builder().username("owner1").build();
        var request = new ExpenseCreateRequest("Cena", new BigDecimal("12.50"), null,
                ExpenseCategory.other, LocalDate.now());

        Expense created = service.createExpense(request, owner);

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getTitle()).isEqualTo("Cena");
        verify(em).persist(created);
        verify(events).publishEvent(new OwnerExpensesChanged("owner1"));
    }

    @Test
    void createTrimsDescription() {
        var request = new ExpenseCreateRequest("Cena", BigDecimal.ONE, "  nota  ",
                ExpenseCategory.other, LocalDate.now());
        assertThat(service.createExpense(request, User.builder().username("owner1").build()).getDescription())
                .isEqualTo("nota");
    }

    @Test
    void searchAndListAreScopedToTheOwnerAndReturnResponses() {
        var cache = mock(OwnerExpenseCache.class);
        when(cache.read(anyString(), anyString(), any())).thenAnswer(call ->
                ((java.util.function.Supplier<?>) call.getArgument(2)).get());
        var cached = new ExpenseService(repository, new ExpenseMapperImpl(), em, cache, events);
        Expense expense = existing();
        when(repository.findByOwnerUsername(eq("owner1"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(java.util.List.of(expense)));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(expense)));

        var all = cached.findAllOwner("owner1");
        assertThat(all.items()).extracting("title").containsExactly("Vecchio");
        assertThat(all.truncated()).isFalse();
        var page = cached.search("owner1", "2026-01", "vecc", 0, 10, "newest");
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        verify(cache).read(eq("owner1"), startsWith("search:"), any());
        verify(cache).read(eq("owner1"), eq("all:v2"), any());
    }

    @Test
    void listIsCappedToTheNewestExpensesAndKeepsTheHistoricalOrder() {
        var cache = mock(OwnerExpenseCache.class);
        when(cache.read(anyString(), anyString(), any())).thenAnswer(call ->
                ((java.util.function.Supplier<?>) call.getArgument(2)).get());
        var cached = new ExpenseService(repository, new ExpenseMapperImpl(), em, cache, events);
        Expense newer = existing();
        newer.setId(9L);
        Expense older = existing();
        older.setId(3L);
        // Il repository restituisce le più recenti per prime e segnala che ne esistono altre.
        var pageable = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        when(repository.findByOwnerUsername(eq("owner1"), pageable.capture()))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(
                        java.util.List.of(newer, older), org.springframework.data.domain.PageRequest.of(0, 2), true));

        var result = cached.findAllOwner("owner1");

        assertThat(result.truncated()).isTrue();
        assertThat(result.items()).extracting("id").containsExactly(3L, 9L);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(org.esercizi.expensetrackerapi.dto.CappedList.MAX_SIZE);
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("date: DESC,id: DESC");
    }

    @Test
    void missingOrForeignExpenseIsNotFoundAndNeverTouchesPersistenceOrCache() {
        when(repository.findByIdAndOwnerUsername(7L, "intruder")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdOwner(7, "intruder")).isInstanceOf(NotFoundExpenseException.class);
        assertThatThrownBy(() -> service.deleteById(7, "intruder")).isInstanceOf(NotFoundExpenseException.class);
        assertThatThrownBy(() -> service.putExpenseById(7, new ExpenseUpdateRequest("Nuovo", BigDecimal.ONE, null,
                ExpenseCategory.other, LocalDate.now()), "intruder")).isInstanceOf(NotFoundExpenseException.class);
        assertThatThrownBy(() -> service.patchExpenseById(7, new ExpensePatchRequest("Nuovo", null, null, null, null),
                "intruder")).isInstanceOf(NotFoundExpenseException.class);
        verifyNoInteractions(em, events);
    }

    @Test
    void patchChangesOnlyProvidedFieldsAndEmptyDescriptionClearsIt() {
        Expense expense = existing();
        when(repository.findByIdAndOwnerUsername(7L, "owner1")).thenReturn(Optional.of(expense));

        service.patchExpenseById(7, new ExpensePatchRequest("  Nuovo  ", null, null, null, null), "owner1");
        assertThat(expense.getTitle()).isEqualTo("Nuovo");
        assertThat(expense.getAmount()).isEqualByComparingTo("10");
        assertThat(expense.getDescription()).isEqualTo("old");

        service.patchExpenseById(7, new ExpensePatchRequest(null, null, "", null, null), "owner1");
        assertThat(expense.getDescription()).isEmpty();
        verify(events, times(2)).publishEvent(new OwnerExpensesChanged("owner1"));
    }

    @Test
    void putReplacesEveryFieldAndDeleteRemovesTheEntity() {
        Expense expense = existing();
        when(repository.findByIdAndOwnerUsername(7L, "owner1")).thenReturn(Optional.of(expense));

        service.putExpenseById(7, new ExpenseUpdateRequest("Sostituita", new BigDecimal("99.90"), null,
                ExpenseCategory.gifts, LocalDate.now()), "owner1");
        assertThat(expense.getTitle()).isEqualTo("Sostituita");
        assertThat(expense.getAmount()).isEqualByComparingTo("99.90");
        assertThat(expense.getDescription()).isNull();
        assertThat(expense.getCategory()).isEqualTo(ExpenseCategory.gifts);

        service.deleteById(7, "owner1");
        verify(em).remove(expense);
        verify(em, never()).persist(any());
    }
}
