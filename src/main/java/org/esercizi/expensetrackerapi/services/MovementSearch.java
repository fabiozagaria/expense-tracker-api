package org.esercizi.expensetrackerapi.services;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public final class MovementSearch {
    private MovementSearch() {}
    public static PageRequest page(int page, int size, String order) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page must be nonnegative; size must be 1–100");
        if (!order.equals("newest") && !order.equals("oldest")) throw new IllegalArgumentException("Invalid order");
        Sort.Direction direction = order.equals("newest") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, "date", "id"));
    }
    public static <T> Specification<T> owned(String username, String month, String query) {
        if (query.length() > 100) throw new IllegalArgumentException("Search too long");
        final YearMonth period;
        try { period = month.isBlank() ? null : YearMonth.parse(month); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("Month must be yyyy-MM"); }
        if (period != null && (period.getYear() < 1 || period.getYear() > 9999)) throw new IllegalArgumentException("Invalid year");
        String pattern = "%" + query.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, criteria, cb) -> {
            var predicate = cb.equal(root.get("owner").get("username"), username);
            if (period != null) predicate = cb.and(predicate,
                    cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), period.atDay(1)),
                    cb.lessThan(root.<LocalDate>get("date"), period.plusMonths(1).atDay(1)));
            if (!query.isBlank()) {
                var match = cb.or(cb.like(cb.lower(root.get("title")), pattern, '!'),
                        cb.like(cb.lower(root.get("description")), pattern, '!'));
                try { match = cb.or(match, cb.equal(root.get("amount"), new java.math.BigDecimal(query.trim().replace(',', '.')))); }
                catch (NumberFormatException ignored) { /* Text search remains available. */ }
                LocalDate date = null;
                try { date = LocalDate.parse(query.trim()); }
                catch (java.time.DateTimeException ignored) {
                    try { date = LocalDate.parse(query.trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)); }
                    catch (java.time.DateTimeException alsoIgnored) { /* Not a date search. */ }
                }
                if (date != null) match = cb.or(match, cb.equal(root.get("date"), date));
                predicate = cb.and(predicate, match);
            }
            return predicate;
        };
    }
}
