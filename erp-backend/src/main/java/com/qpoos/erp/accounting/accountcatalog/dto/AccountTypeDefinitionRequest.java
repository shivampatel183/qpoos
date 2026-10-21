package com.qpoos.erp.accounting.accountcatalog.dto;

import com.qpoos.erp.accounting.domain.LedgerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountTypeDefinitionRequest(
        @NotBlank
        @Size(max = 30)
        String accountGroupCode,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        LedgerType defaultLedgerType,

        Integer displayOrder
) {
}
