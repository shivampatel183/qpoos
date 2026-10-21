package com.qpoos.erp.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String logo,

        Boolean passwordOn,

        @Size(max = 100)
        String password,

        String financialYearStart,

        String financialYearEnd,

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String country,

        @Size(max = 10)
        String pincode,

        @Size(max = 20)
        String phoneNumber,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 255)
        String website
) {
}
