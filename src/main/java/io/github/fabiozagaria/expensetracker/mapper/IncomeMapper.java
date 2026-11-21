package io.github.fabiozagaria.expensetracker.mapper;

import io.github.fabiozagaria.expensetracker.dto.income.IncomeCreateRequest;
import io.github.fabiozagaria.expensetracker.dto.income.IncomePatchRequest;
import io.github.fabiozagaria.expensetracker.dto.income.IncomeResponse;
import io.github.fabiozagaria.expensetracker.dto.income.IncomeUpdateRequest;
import io.github.fabiozagaria.expensetracker.model.income.Income;
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
public interface IncomeMapper {

    IncomeResponse toResponse(Income income);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "user")
    @Mapping(target = "description", source = "request.description", qualifiedByName = "trim")
    Income toEntity(IncomeCreateRequest request, User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "title", source = "title", qualifiedByName = "trim")
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    void updateFromRequest(IncomeUpdateRequest request, @MappingTarget Income income);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "title", source = "title", qualifiedByName = "trim")
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    void patchFromRequest(IncomePatchRequest request, @MappingTarget Income income);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
