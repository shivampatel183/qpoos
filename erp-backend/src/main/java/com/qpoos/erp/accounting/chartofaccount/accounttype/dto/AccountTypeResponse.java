package com.qpoos.erp.accounting.chartofaccount.accounttype.dto;

import com.qpoos.erp.accounting.chartofaccount.domain.NormalBalance;
import com.qpoos.erp.accounting.chartofaccount.domain.StatementType;

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
