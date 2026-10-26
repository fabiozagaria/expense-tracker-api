package org.esercizi.expensetrackerapi.dto;
import java.util.List;
import org.springframework.data.domain.Page;
public record MovementPage<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    public static <T> MovementPage<T> from(Page<T> page) {
        return new MovementPage<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
