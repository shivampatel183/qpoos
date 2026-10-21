package com.qpoos.erp.accounting.accounttype.dto;

import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.domain.StatementType;

public record AccountTypeResponse(
        Long id,
        String code,
        String name,
        NormalBalance normalBalance,
        StatementType statementType,
        Integer displayOrder,
        Boolean systemDefined
) {
}
