package org.esercizi.expensetrackerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExpenseTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApiApplication.class, args);
    }

}
