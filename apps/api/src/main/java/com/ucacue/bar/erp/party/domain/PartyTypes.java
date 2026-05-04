package com.ucacue.bar.erp.party.domain;

public final class PartyTypes {

    private PartyTypes() {}

    public enum PartyType {
        PERSON,
        ORGANIZATION
    }

    public enum IdentifierType {
        CEDULA,
        RUC,
        PASSPORT,
        FOREIGN_ID,
        OTHER
    }

    public enum AddressType {
        MAIN,
        BILLING,
        SHIPPING,
        BRANCH,
        OTHER
    }

    public enum ContactType {
        EMAIL,
        PHONE,
        MOBILE,
        WHATSAPP,
        FAX,
        OTHER
    }

    public enum RoleType {
        CUSTOMER,
        SUPPLIER,
        EMPLOYEE,
        STUDENT,
        GUARDIAN,
        CONTACT,
        OTHER
    }
}
