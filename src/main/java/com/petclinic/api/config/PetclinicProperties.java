package com.petclinic.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petclinic")
public record PetclinicProperties(String baseUrl) {
}
