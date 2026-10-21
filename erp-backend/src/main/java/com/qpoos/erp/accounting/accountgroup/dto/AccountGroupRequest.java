package com.qpoos.erp.accounting.accountgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountGroupRequest(
        @NotBlank
        @Size(max = 30)
        String accountTypeCode,

        Long parentGroupId,

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        Integer displayOrder
) {
}
