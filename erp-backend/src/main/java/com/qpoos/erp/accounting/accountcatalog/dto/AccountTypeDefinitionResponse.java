package com.qpoos.erp.accounting.accountcatalog.dto;

import com.qpoos.erp.accounting.domain.LedgerType;
import com.qpoos.erp.accounting.domain.NormalBalance;

public record AccountTypeDefinitionResponse(
        Long id,
        String code,
        String name,
        Long accountGroupId,
        String accountGroupCode,
        String accountGroupName,
        NormalBalance normalBalance,
        LedgerType defaultLedgerType,
        Integer displayOrder,
        Boolean active,
        Boolean systemDefined
) {
}
