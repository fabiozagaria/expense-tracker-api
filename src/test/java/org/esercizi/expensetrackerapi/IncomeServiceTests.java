package org.esercizi.expensetrackerapi;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.esercizi.expensetrackerapi.services.*;
import org.esercizi.expensetrackerapi.repository.IncomeRepository;
import org.esercizi.expensetrackerapi.model.income.*;
import org.esercizi.expensetrackerapi.dto.income.*;
import org.esercizi.expensetrackerapi.exceptions.NotFoundIncomeException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class IncomeServiceTests {
    final IncomeRepository repository = mock(IncomeRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final IncomeService service = new IncomeService(repository, em);
    @Test void missingOrForeignIncomeCannotBeReadChangedOrDeleted() {
        when(repository.findByIdAndOwnerUsername(3L, "owner")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByIdOwner(3, "owner")).isInstanceOf(NotFoundIncomeException.class);
        assertThatThrownBy(() -> service.deleteById(3, "owner")).isInstanceOf(NotFoundIncomeException.class);
        assertThatThrownBy(() -> service.patchIncomeById(3, new IncomePatchRequest("New title", null, null, null, null), "owner")).isInstanceOf(NotFoundIncomeException.class);
        verifyNoInteractions(em);
    }
    @Test void putReplacesAllFieldsAndPatchPreservesOmittedFields() {
        Income income = new Income(3L, "Old title", BigDecimal.TEN, "old", IncomeCategory.salary, LocalDate.now(), null);
        when(repository.findByIdAndOwnerUsername(3L, "owner")).thenReturn(Optional.of(income));
        service.putIncomeById(3, new IncomeUpdateRequest("New title", new BigDecimal("42.50"), null, IncomeCategory.other, LocalDate.now().minusDays(1)), "owner");
        assertThat(income.getTitle()).isEqualTo("New title");
        assertThat(income.getDescription()).isNull();
        service.patchIncomeById(3, new IncomePatchRequest(null, null, "", null, null), "owner");
        assertThat(income.getAmount()).isEqualByComparingTo("42.50");
        assertThat(income.getDescription()).isEmpty();
        service.deleteById(3, "owner"); verify(em).remove(income);
    }
    @Test void paginationIsBoundedAndStable() {
        assertThatThrownBy(() -> MovementSearch.page(-1, 10, "newest")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovementSearch.page(0, 101, "newest")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovementSearch.page(0, 10, "amount")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovementSearch.owned("owner", "2026-13", "")).isInstanceOf(IllegalArgumentException.class);
        assertThat(MovementSearch.page(0, 10, "newest").getSort().toString()).isEqualTo("date: DESC,id: DESC");
    }
}
