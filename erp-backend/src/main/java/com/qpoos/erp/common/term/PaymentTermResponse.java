package com.qpoos.erp.common.term;

public record PaymentTermResponse(
        Long id,
        String code,
        String name,
        Integer dueDays,
        Boolean active
) {
}