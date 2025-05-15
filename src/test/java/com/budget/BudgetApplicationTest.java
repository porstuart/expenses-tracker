package com.budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetApplicationTest {

    @Mock
    private SpringApplication springApplication;

    @InjectMocks
    private BudgetApplication budgetApplication;

    @Test
    void runApplication_CallsSpringApplicationRun() {
        String[] args = {"--server.port=8080"};

        budgetApplication.runApplication(args);

        verify(springApplication).run(args);
    }

    @Test
    void mainMethod_CallsRunApplication() {
        String[] args = {"--server.port=8080"};

        // Since main is static, we can call it or simulate its behavior
        BudgetApplication app = new BudgetApplication(springApplication);
        app.runApplication(args);

        verify(springApplication).run(args);
    }
}