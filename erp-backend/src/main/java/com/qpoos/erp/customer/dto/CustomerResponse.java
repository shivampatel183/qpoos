package com.qpoos.erp.customer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        Long id,
        UUID companyId,
        String companyName,
        String displayName,
        String gstNo,
        String firstName,
        String lastName,
        String email,
        String mobileNo,
        String streetAddress1,
        String streetAddress2,
        String city,
        String state,
        String country,
        String pinCode,
        String notes,
        String accountHolderName,
        String accountNumber,
        String ifscCode,
        BigDecimal openingBalance,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
