package com.petclinic.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PetclinicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetclinicApiApplication.class, args);
    }
}
