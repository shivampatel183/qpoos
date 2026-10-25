package com.qpoos.erp.accounting.journal.dto;

import java.math.BigDecimal;

public record JournalLineResponse(
        Long id,
        Integer lineNo,
        Long ledgerId,
        String ledgerCode,
        String ledgerName,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String description
) {
}
