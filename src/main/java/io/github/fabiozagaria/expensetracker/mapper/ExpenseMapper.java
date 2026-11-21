package io.github.fabiozagaria.expensetracker.mapper;

import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseCreateRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpensePatchRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseResponse;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseUpdateRequest;
import io.github.fabiozagaria.expensetracker.model.expense.Expense;
import io.github.fabiozagaria.expensetracker.model.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ExpenseMapper {

    ExpenseResponse toResponse(Expense expense);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "user")
    @Mapping(target = "description", source = "request.description", qualifiedByName = "trim")
    Expense toEntity(ExpenseCreateRequest request, User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    void updateFromRequest(ExpenseUpdateRequest request, @MappingTarget Expense expense);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "title", source = "title", qualifiedByName = "trim")
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    void patchFromRequest(ExpensePatchRequest request, @MappingTarget Expense expense);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
