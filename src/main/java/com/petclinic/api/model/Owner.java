package com.petclinic.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Owner(
        Integer id,
        String firstName,
        String lastName,
        String address,
        String city,
        String telephone
) {
    public OwnerRequest asRequest() {
        return new OwnerRequest(firstName, lastName, address, city, telephone);
    }
}
