package com.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BudgetApplication {

    private final SpringApplication springApplication;

    public BudgetApplication() {
        this.springApplication = new SpringApplication(BudgetApplication.class);
    }

    public BudgetApplication(SpringApplication springApplication) {
        this.springApplication = springApplication;
    }

    public void runApplication(String[] args) {
        springApplication.run(args);
    }

    public static void main(String[] args) {
        BudgetApplication app = new BudgetApplication();
        app.runApplication(args);
    }
}