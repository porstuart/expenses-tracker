package com.budget.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.budget.model.Ledger;

@Repository
public interface LedgerDao extends JpaRepository<Ledger, Long>{

    @Query("SELECT l FROM Ledger l WHERE l.ledgerId = :ledgerId AND l.deleted = false")
    Optional<Ledger> findByIdAndDeletedFalse(@Param("ledgerId") Long ledgerId);

    @Query("SELECT l FROM Ledger l WHERE l.personId = :personId AND l.name = :ledgerName AND l.deleted = false")
    Optional<Ledger> findByPersonIdAndNameAndDeletedFalse(@Param("personId") Long personId, @Param("ledgerName") String ledgerName);

    @Query("SELECT l FROM Ledger l WHERE l.personId = :personId AND l.deleted = false ORDER BY l.ledgerId")
    List<Ledger> findAllByPersonIdAndDeletedFalse(@Param("personId") Long personId);

}
