package com.qpoos.erp.common.term;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentTermRequest(
        @NotBlank
        @Size(max = 40)
        String code,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        @Min(0)
        Integer dueDays,

        Boolean active
) {
}