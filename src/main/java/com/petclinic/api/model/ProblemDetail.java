package com.petclinic.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemDetail(
        String type,
        String title,
        Integer status,
        String detail,
        String timestamp,
        List<ValidationMessage> schemaValidationErrors
) {
}
