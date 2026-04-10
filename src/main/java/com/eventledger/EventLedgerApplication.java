package com.eventledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EventLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventLedgerApplication.class, args);
    }
}
