package com.petclinic.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/* The schema declares message plus an optional additionalProperties container, so both are here.
The application never fills that container: it returns field, defaultMessage and rejectedValue at
the top level instead, which is more useful for a client than a nameless map. The implementation
therefore looks closer to the truth than the schema, and the contract needs clarification before
the tests can rely on those fields, see BUG-8. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidationMessage(String message, Map<String, Object> additionalProperties) {
}
