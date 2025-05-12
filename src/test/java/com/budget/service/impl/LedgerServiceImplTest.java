package com.budget.service.impl;

import com.budget.dao.LedgerDao;
import com.budget.exception.ApiException;
import com.budget.model.Ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    private static final String USD_CURRENCY = "USD";

    @Mock
    private LedgerDao ledgerDao;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        ledger = new Ledger();
        ledger.setLedgerId(1L);
        ledger.setPersonId(100L);
        ledger.setName("Personal Budget");
        ledger.setCurrency(USD_CURRENCY);
        ledger.setDeleted(false);
    }

    @Test
    void getLedgerById_Success() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ledger));

        Ledger result = ledgerService.getLedgerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLedgerId());
        verify(ledgerDao).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getLedgerById_NotFound() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.getLedgerById(1L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Ledger not found.", exception.getMessage());
        verify(ledgerDao).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getAllLedgersByPersonId_Success() {
        List<Ledger> ledgerList = Collections.singletonList(ledger);
        when(ledgerDao.findAllByPersonIdAndDeletedFalse(100L)).thenReturn(ledgerList);

        List<Ledger> result = ledgerService.getAllLedgersByPersonId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getPersonId());
        verify(ledgerDao).findAllByPersonIdAndDeletedFalse(100L);
    }

    @Test
    void getAllLedgersByPersonId_Empty() {
        when(ledgerDao.findAllByPersonIdAndDeletedFalse(200L)).thenReturn(Collections.emptyList());

        List<Ledger> result = ledgerService.getAllLedgersByPersonId(200L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ledgerDao).findAllByPersonIdAndDeletedFalse(200L);
    }

    @Test
    void saveLedger_Success() {
        when(ledgerDao.save(ledger)).thenReturn(ledger);

        Ledger result = ledgerService.saveLedger(ledger);

        assertNotNull(result);
        assertEquals(1L, result.getLedgerId());
        verify(ledgerDao).save(ledger);
    }

    @Test
    void saveLedger_NullLedger() {
        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.saveLedger(null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Ledger or personId cannot be null.", exception.getMessage());
    }

    @Test
    void saveLedger_NullPersonId() {
        Ledger invalidLedger = new Ledger();
        invalidLedger.setName("Test");
        invalidLedger.setCurrency(USD_CURRENCY);

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.saveLedger(invalidLedger));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Ledger or personId cannot be null.", exception.getMessage());
    }

    @Test
    void saveLedger_NullName() {
        Ledger invalidLedger = new Ledger();
        invalidLedger.setPersonId(100L);
        invalidLedger.setCurrency(USD_CURRENCY);

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.saveLedger(invalidLedger));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Ledger name cannot be null.", exception.getMessage());
    }

    @Test
    void saveLedger_NullCurrency() {
        Ledger invalidLedger = new Ledger();
        invalidLedger.setPersonId(100L);
        invalidLedger.setName("Test");
        invalidLedger.setCurrency(null);

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.saveLedger(invalidLedger));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Ledger currency cannot be null.", exception.getMessage());

        verifyNoInteractions(ledgerDao);
    }

    @Test
    void saveLedger_DuplicateName() {
        Ledger existingLedger = new Ledger();
        existingLedger.setLedgerId(2L);
        existingLedger.setPersonId(100L);
        existingLedger.setName("Personal Budget");
        existingLedger.setCurrency(USD_CURRENCY);
        existingLedger.setDeleted(false);

        when(ledgerDao.findAllByPersonIdAndDeletedFalse(100L)).thenReturn(List.of(existingLedger));

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.saveLedger(ledger));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Ledger name already exists.", exception.getMessage());
        verify(ledgerDao).findAllByPersonIdAndDeletedFalse(100L);
        verifyNoMoreInteractions(ledgerDao);
    }

    @Test
    void updateLedger_Success() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ledger));
        when(ledgerDao.save(ledger)).thenReturn(ledger);

        Ledger result = ledgerService.updateLedger(ledger);

        assertNotNull(result);
        assertEquals(1L, result.getLedgerId());
        verify(ledgerDao).findByIdAndDeletedFalse(1L);
        verify(ledgerDao).save(ledger);
    }

    @Test
    void updateLedger_NotFound() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.updateLedger(ledger));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Ledger not found.", exception.getMessage());
    }

    @Test
    void deleteLedger_Success() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ledger));
        when(ledgerDao.save(ledger)).thenReturn(ledger);

        ledgerService.deleteLedger(1L);

        assertTrue(ledger.getDeleted());
        verify(ledgerDao).findByIdAndDeletedFalse(1L);
        verify(ledgerDao).save(ledger);
    }

    @Test
    void deleteLedger_NotFound() {
        when(ledgerDao.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.deleteLedger(1L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Ledger not found.", exception.getMessage());
    }
}