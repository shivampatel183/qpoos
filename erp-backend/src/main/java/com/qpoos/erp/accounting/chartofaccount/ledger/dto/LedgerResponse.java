package com.qpoos.erp.accounting.chartofaccount.ledger.dto;

import com.qpoos.erp.accounting.chartofaccount.domain.LedgerType;
import com.qpoos.erp.accounting.chartofaccount.domain.NormalBalance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerResponse(
        Long id,
        UUID companyId,
        Long accountGroupId,
        String accountGroupCode,
        String accountGroupName,
        Long accountTypeId,
        String accountTypeCode,
        String accountTypeName,
        String code,
        String name,
        String description,
        BigDecimal openingBalance,
        LocalDate openingBalanceAsOfDate,
        BigDecimal balance,
        LedgerType ledgerType,
        NormalBalance normalBalance,
        Boolean active,
        Boolean postingAllowed,
        Boolean systemDefined
) {
}
