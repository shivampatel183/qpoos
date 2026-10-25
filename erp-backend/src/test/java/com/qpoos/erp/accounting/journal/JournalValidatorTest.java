package com.qpoos.erp.accounting.journal;

import com.qpoos.erp.accounting.chartofaccount.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.journal.application.JournalValidator;
import com.qpoos.erp.accounting.journal.domain.JournalLineEntity;
import com.qpoos.erp.company.domain.CompanyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalValidatorTest {

    private JournalValidator validator;
    private UUID companyId;
    private LedgerEntity debitLedger;
    private LedgerEntity creditLedger;

    @BeforeEach
    void setUp() {
        validator = new JournalValidator();
        companyId = UUID.randomUUID();
        CompanyEntity company = CompanyEntity.builder().id(companyId).build();
        debitLedger = ledger(1L, company, true, true);
        creditLedger = ledger(2L, company, true, true);
    }

    @Test
    void acceptsBalancedJournal() {
        assertDoesNotThrow(() -> validator.validate(companyId, List.of(
                line(1, debitLedger, "100.00", "0.00"),
                line(2, creditLedger, "0.00", "100.00")
        )));
    }

    @Test
    void rejectsUnbalancedJournal() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "100.00", "0.00"),
                line(2, creditLedger, "0.00", "99.99")
        )));
    }

    @Test
    void rejectsJournalWithLessThanTwoLines() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                companyId,
                List.of(line(1, debitLedger, "100.00", "0.00"))
        ));
    }

    @Test
    void rejectsCrossCompanyAndInactiveAccounts() {
        LedgerEntity otherCompanyLedger = ledger(
                3L,
                CompanyEntity.builder().id(UUID.randomUUID()).build(),
                true,
                true
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "100.00", "0.00"),
                line(2, otherCompanyLedger, "0.00", "100.00")
        )));

        LedgerEntity inactiveLedger = ledger(4L, debitLedger.getCompany(), false, true);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, inactiveLedger, "100.00", "0.00"),
                line(2, creditLedger, "0.00", "100.00")
        )));

        LedgerEntity nonPostableLedger = ledger(5L, debitLedger.getCompany(), true, false);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, nonPostableLedger, "100.00", "0.00"),
                line(2, creditLedger, "0.00", "100.00")
        )));
    }

    @Test
    void rejectsInvalidSidesAndDuplicateLineNumbers() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "-1.00", "0.00"),
                line(2, creditLedger, "0.00", "1.00")
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "1.00", "1.00"),
                line(2, creditLedger, "0.00", "2.00")
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "0.00", "0.00"),
                line(2, creditLedger, "0.00", "1.00")
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(1, debitLedger, "1.00", "0.00"),
                line(1, creditLedger, "0.00", "1.00")
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(companyId, List.of(
                line(0, debitLedger, "1.00", "0.00"),
                line(2, creditLedger, "0.00", "1.00")
        )));
    }

    private LedgerEntity ledger(Long id, CompanyEntity company, boolean active, boolean postingAllowed) {
        return LedgerEntity.builder()
                .id(id)
                .company(company)
                .active(active)
                .postingAllowed(postingAllowed)
                .build();
    }

    private JournalLineEntity line(int lineNo, LedgerEntity ledger, String debit, String credit) {
        return JournalLineEntity.builder()
                .lineNo(lineNo)
                .ledger(ledger)
                .debitAmount(new BigDecimal(debit))
                .creditAmount(new BigDecimal(credit))
                .build();
    }
}
