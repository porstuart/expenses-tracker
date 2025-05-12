package com.budget.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "ledger")
@Entity
public class Ledger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;
    
    @NotBlank(message = "Person id is required.")
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @NotBlank(message = "Ledger name is required.")
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description")
    private String description;

    @NotBlank(message = "Ledger currency is required.")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "color", nullable = false, length = 7)
    private String color = "#000000";

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

}
