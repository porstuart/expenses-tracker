package com.budget.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.budget.model.Ledger;
import com.budget.service.LedgerService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class LedgerController {

    private final LedgerService ledgerService;

    @Autowired
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("v1/ledger/{ledgerId}")
    public ResponseEntity<Ledger> getLedgerById(@PathVariable Long ledgerId) {
        log.info("Retrieving ledger for ledgerId {}", ledgerId);    
        Ledger ledgerResponse = ledgerService.getLedgerById(ledgerId);
        log.info("Successfully retrieved ledger for ledgerId {}", ledgerId);

        return ResponseEntity.ok(ledgerResponse);
    }

    @GetMapping("v1/ledgers/{personId}")
    public ResponseEntity<List<Ledger>> getAllLedgersByPersonId(@PathVariable Long personId) {
        log.info("Retrieving ledgers for personId {}", personId);
        List<Ledger> ledgerList = ledgerService.getAllLedgersByPersonId(personId);
        log.info("Successfully retrieved ledgers for personId {}", personId);

        return ResponseEntity.ok(ledgerList);
    }

    @PostMapping("/v1/ledger")
    public ResponseEntity<Ledger> createLedger(@Valid @RequestBody Ledger ledgerModel) {
        log.info("Creating ledger for personId {}", ledgerModel.getPersonId());
        Ledger ledgerResponse = ledgerService.saveLedger(ledgerModel);
        log.info("Successfully created ledger {} for personId {}", ledgerModel.getName(), ledgerModel.getPersonId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerResponse);
    }

    @PutMapping("/v1/ledger")
    public ResponseEntity<Ledger> updateLedger(@Valid @RequestBody Ledger ledgerModel) {
        log.info("Updating ledger for personId {}", ledgerModel.getPersonId());
        Ledger ledgerResponse = ledgerService.updateLedger(ledgerModel);
        log.info("Successfully updated ledger {} for personId {}", ledgerModel.getName(), ledgerModel.getPersonId());

        return ResponseEntity.ok(ledgerResponse);
    }

    @PutMapping("v1/ledger/{ledgerId}/delete")
    public ResponseEntity<Void> deleteLedger(@PathVariable Long ledgerId) {
        log.info("Deleting ledger for ledgerId {}", ledgerId);
        ledgerService.deleteLedger(ledgerId);
        log.info("Successfully deleted ledger for ledgerId {}", ledgerId);

        return ResponseEntity.noContent().build();
    }

}
