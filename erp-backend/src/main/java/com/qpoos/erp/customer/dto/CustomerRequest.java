package com.qpoos.erp.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerRequest(
        @Size(max = 180)
        String companyName,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(max = 180)
        String displayName,

        @Email
        @Size(max = 180)
        String email,

        @Size(max = 20)
        String mobileNo,

        @Size(max = 255)
        String streetAddress1,

        @Size(max = 255)
        String streetAddress2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String country,

        @Size(max = 12)
        String pinCode,

        @Size(max = 4000)
        String notes,

        @Size(max = 180)
        String accountHolderName,

        @Size(max = 50)
        String accountNumber,

        @Pattern(
                regexp = "^$|^[A-Za-z]{4}0[A-Za-z0-9]{6}$",
                message = "must be a valid IFSC code"
        )
        String ifscCode,

        @Pattern(
                regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
                message = "must be a valid GST number"
        )
        String gstNo,

        @DecimalMin(value = "0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal openingBalance
) {
}
