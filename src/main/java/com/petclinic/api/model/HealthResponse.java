package com.petclinic.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HealthResponse(
        String status,
        List<String> groups
) {
}
