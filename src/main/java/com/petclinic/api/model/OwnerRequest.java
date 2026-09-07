package com.petclinic.api.model;

public record OwnerRequest(
        String firstName,
        String lastName,
        String address,
        String city,
        String telephone
) {
    public OwnerRequest withFirstName(String value) {
        return new OwnerRequest(value, lastName, address, city, telephone);
    }

    public OwnerRequest withLastName(String value) {
        return new OwnerRequest(firstName, value, address, city, telephone);
    }

    public OwnerRequest withAddress(String value) {
        return new OwnerRequest(firstName, lastName, value, city, telephone);
    }

    public OwnerRequest withCity(String value) {
        return new OwnerRequest(firstName, lastName, address, value, telephone);
    }

    public OwnerRequest withTelephone(String value) {
        return new OwnerRequest(firstName, lastName, address, city, value);
    }
}
