package com.budget.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.budget.dao.LedgerDao;
import com.budget.exception.ApiException;
import com.budget.model.Ledger;
import com.budget.service.LedgerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LedgerServiceImpl implements LedgerService {

    private static final String LEDGER_NOT_FOUND = "Ledger not found.";

    @Autowired
    LedgerDao ledgerDao;

    @Override
    public Ledger getLedgerById(Long ledgerId) {
        // Ledger ledger = null;
        Optional<Ledger> ledger = ledgerDao.findByIdAndDeletedFalse(ledgerId);
        if (Objects.isNull(ledger)) {
            log.error("Ledger not found for ledgerId {}", ledgerId);
            throw new ApiException(HttpStatus.NOT_FOUND, LEDGER_NOT_FOUND);
        }

        return ledger.orElseThrow(() -> { 
            log.error("Ledger not found for ledgerId {}", ledgerId); 
            return new ApiException(HttpStatus.NOT_FOUND, LEDGER_NOT_FOUND);
        });
        // return ledger;
    }

    @Override
    public List<Ledger> getAllLedgersByPersonId(Long personId) {
        return ledgerDao.findAllByPersonIdAndDeletedFalse(personId);
    }

    @Transactional
    @Override
    public Ledger saveLedger(Ledger ledgerModel) {
        validateLedger(ledgerModel);
        checkDuplicateLedgerName(ledgerModel.getLedgerId(), ledgerModel.getPersonId(), ledgerModel.getName());
        Ledger savedLedger = ledgerDao.save(ledgerModel);

        return savedLedger;
    }

    @Transactional
    @Override
    public Ledger updateLedger(Ledger ledgerModel) {
        getLedgerById(ledgerModel.getLedgerId());
        validateLedger(ledgerModel);
        checkDuplicateLedgerName(ledgerModel.getLedgerId(), ledgerModel.getPersonId(), ledgerModel.getName());

        return updateLedgerDetails(ledgerModel);
    }

    @Transactional
    public void deleteLedger(Long ledgerId) {
        Ledger ledger = getLedgerById(ledgerId);

        ledger.setDeleted(true);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledgerDao.save(ledger);
    }

    private void validateLedger(Ledger ledger) {
        if (Objects.isNull(ledger) || Objects.isNull(ledger.getPersonId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger or personId cannot be null.");
        }

        if (Objects.isNull(ledger.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger name cannot be null.");
        }

        if (Objects.isNull(ledger.getCurrency())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger currency cannot be null.");
        }
    }

    private void checkDuplicateLedgerName(Long ledgerId, Long personId, String ledgerName) {
        if(hasDuplicateLedgerName(ledgerId, personId, ledgerName)) {
            log.error("Ledger name - {} for personId {} already exists.", ledgerName, personId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger name already exists.");
        }
    }

    private Boolean hasDuplicateLedgerName(Long ledgerId, Long personId, String ledgerName) {
        List<Ledger> ledgerList = getAllLedgersByPersonId(personId);

        return ledgerList.stream()
                .filter(ledger -> !Objects.equals(ledger.getLedgerId(), ledgerId)) // Exclude current ledger if updating
                .map(Ledger::getName)
                .anyMatch(name -> name.trim().equalsIgnoreCase(ledgerName));
    }

    private Ledger updateLedgerDetails(Ledger ledgerModel) {
        ledgerModel.setUpdatedAt(LocalDateTime.now());
        Ledger updatedLedger = ledgerDao.save(ledgerModel);

        return updatedLedger;
    }
    
}
