package com.budget.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.budget.model.Ledger;

@Service
public interface LedgerService {

    public Ledger getLedgerById(Long ledgerId);
    
    public List<Ledger> getAllLedgersByPersonId(Long personId);

    public Ledger saveLedger(Ledger ledgerModel);

    public Ledger updateLedger(Ledger ledgerModel);

    public void deleteLedger(Long ledgerId);

}
