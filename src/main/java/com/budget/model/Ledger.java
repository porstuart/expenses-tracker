package com.budget.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "ledger")
@Entity
public class Ledger implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;
    
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "color", nullable = false, length = 7)
    private String color = "#000000";

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

}
