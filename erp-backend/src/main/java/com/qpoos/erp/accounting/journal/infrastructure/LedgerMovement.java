package com.qpoos.erp.accounting.journal.infrastructure;

import java.math.BigDecimal;

public interface LedgerMovement {
    BigDecimal getDebitTotal();
    BigDecimal getCreditTotal();
}
