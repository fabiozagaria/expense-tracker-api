package org.esercizi.expensetrackerapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String title;

    private BigDecimal amount;

    private ExpenseCategory category;

    private LocalDate date;

    public Expense() {

    }

    public Expense(Integer id, String title, BigDecimal amount, ExpenseCategory category, LocalDate date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
