package com.polatholding.procurementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProcurementsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementsystemApplication.class, args);
    }

}