package com.qpoos.erp.company.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String logo,
        boolean passwordOn,
        String financialYearStart,
        String financialYearEnd,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String pincode,
        String phoneNumber,
        String email,
        String website,
        Boolean isActive,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
