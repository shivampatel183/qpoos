package com.qpoos.erp.common.number;

public enum DocumentNumberType {
    EXPENSE("EXP"),
    PURCHASE_ORDER("PO"),
    PURCHASE_BILL("BILL"),
    PAYMENT("PAY");

    private final String prefix;

    DocumentNumberType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}