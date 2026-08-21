package org.esercizi.expensetrackerapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 15)
    private String title;

    @Positive
    private BigDecimal amount;

    @Size(max = 30)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    @NotNull
    @PastOrPresent
    private LocalDate date;

    public Expense() {

    }

    public Expense(Long id, String title, BigDecimal amount, String description, ExpenseCategory category, LocalDate date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
