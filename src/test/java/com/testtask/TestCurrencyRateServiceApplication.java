package com.testtask;

import org.springframework.boot.SpringApplication;

public class TestCurrencyRateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CurrencyRateServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
