package com.budget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.budget.model.Ledger;
import com.budget.service.LedgerService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

    private static final String TEST_LEDGER_NAME = "Test Ledger";
    private static final String LEDGER_URL = "/v1/ledger";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LedgerService ledgerService;

    @Autowired
    private ObjectMapper objectMapper;

    private Ledger testLedger;
    private List<Ledger> testLedgerList;

    @BeforeEach
    void setup() {
        // Initialize test data
        testLedger = new Ledger();
        testLedger.setLedgerId(1L);
        testLedger.setName(TEST_LEDGER_NAME);
        testLedger.setPersonId(100L);
        testLedger.setDeleted(false);

        Ledger secondLedger = new Ledger();
        secondLedger.setLedgerId(2L);
        secondLedger.setName("Another Ledger");
        secondLedger.setPersonId(100L);
        secondLedger.setDeleted(false);

        testLedgerList = Arrays.asList(testLedger, secondLedger);
    }

    @Test
    void testGetLedgerById() throws Exception {
        when(ledgerService.getLedgerById(1L)).thenReturn(testLedger);

        mockMvc.perform(get(LEDGER_URL + "/1")
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerId").value(1))
                .andExpect(jsonPath("$.name").value(TEST_LEDGER_NAME))
                .andExpect(jsonPath("$.personId").value(100))
                .andExpect(jsonPath("$.deleted").value(false));

        verify(ledgerService, times(1)).getLedgerById(1L);
    }

    @Test
    void testGetAllLedgersByPersonId() throws Exception {
        when(ledgerService.getAllLedgersByPersonId(100L)).thenReturn(testLedgerList);

        mockMvc.perform(get("/v1/ledgers/100")
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ledgerId").value(1))
                .andExpect(jsonPath("$[0].name").value(TEST_LEDGER_NAME))
                .andExpect(jsonPath("$[1].ledgerId").value(2))
                .andExpect(jsonPath("$[1].name").value("Another Ledger"));

        verify(ledgerService, times(1)).getAllLedgersByPersonId(100L);
    }

    @Test
    void testCreateLedger() throws Exception {
        when(ledgerService.saveLedger(any(Ledger.class))).thenReturn(testLedger);

        mockMvc.perform(post(LEDGER_URL)
                .with(csrf())
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testLedger)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ledgerId").value(1))
                .andExpect(jsonPath("$.name").value(TEST_LEDGER_NAME))
                .andExpect(jsonPath("$.personId").value(100))
                .andExpect(jsonPath("$.deleted").value(false));

        verify(ledgerService, times(1)).saveLedger(any(Ledger.class));
    }

    @Test
    void testUpdateLedger() throws Exception {
        when(ledgerService.updateLedger(any(Ledger.class))).thenReturn(testLedger);

        mockMvc.perform(put(LEDGER_URL)
                .with(csrf())
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testLedger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerId").value(1))
                .andExpect(jsonPath("$.name").value(TEST_LEDGER_NAME));

        verify(ledgerService, times(1)).updateLedger(any(Ledger.class));
    }

    @Test
    void testDeleteLedger() throws Exception {
        doNothing().when(ledgerService).deleteLedger(1L);

        mockMvc.perform(put(LEDGER_URL + "/1/delete")
                .with(csrf())
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(ledgerService, times(1)).deleteLedger(1L);
    }

    @Test
    void testCreateLedgerWithInvalidData() throws Exception {
        // Create an invalid ledger with null name
        Ledger invalidLedger = new Ledger();
        invalidLedger.setPersonId(100L);
        invalidLedger.setDeleted(false);
        // Name is null which should trigger validation error

        mockMvc.perform(post(LEDGER_URL)
                .with(csrf())
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLedger)))
                .andExpect(status().isBadRequest());

        // Service should not be called for invalid data
        verify(ledgerService, times(0)).saveLedger(any(Ledger.class));
    }

    @Test
    void testUpdateLedgerWithInvalidData() throws Exception {
        // Create an invalid ledger with null name
        Ledger invalidLedger = new Ledger();
        invalidLedger.setLedgerId(1L);
        invalidLedger.setPersonId(100L);
        invalidLedger.setDeleted(false);
        // Name is null which should trigger validation error

        mockMvc.perform(put(LEDGER_URL)
                .with(csrf())
                .with(user("testUser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLedger)))
                .andExpect(status().isBadRequest());

        // Service should not be called for invalid data
        verify(ledgerService, times(0)).updateLedger(any(Ledger.class));
    }
}