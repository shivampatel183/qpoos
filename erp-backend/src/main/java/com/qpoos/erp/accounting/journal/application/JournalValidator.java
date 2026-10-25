package com.qpoos.erp.accounting.journal.application;

import com.qpoos.erp.accounting.journal.domain.JournalLineEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JournalValidator {

    public void validate(UUID companyId, List<JournalLineEntity> lines) {
        if (companyId == null) {
            throw new IllegalArgumentException("Journal company is required");
        }
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("A journal must contain at least two lines");
        }

        Set<Integer> lineNumbers = new HashSet<>();
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (JournalLineEntity line : lines) {
            if (line == null || line.getLineNo() == null || line.getLineNo() <= 0
                    || !lineNumbers.add(line.getLineNo())) {
                throw new IllegalArgumentException("Journal line numbers must be unique positive values");
            }
            if (line.getLedger() == null || line.getLedger().getCompany() == null
                    || !companyId.equals(line.getLedger().getCompany().getId())) {
                throw new IllegalArgumentException("Every journal account must belong to the journal company");
            }
            if (!Boolean.TRUE.equals(line.getLedger().getActive())
                    || !Boolean.TRUE.equals(line.getLedger().getPostingAllowed())) {
                throw new IllegalArgumentException("Journal account is inactive or does not allow posting");
            }

            BigDecimal debit = line.getDebitAmount();
            BigDecimal credit = line.getCreditAmount();
            if (debit == null || credit == null || debit.signum() < 0 || credit.signum() < 0
                    || (debit.signum() > 0) == (credit.signum() > 0)) {
                throw new IllegalArgumentException(
                        "Each journal line must have exactly one positive, nonnegative debit or credit amount"
                );
            }
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException("Journal is not balanced: total debits must equal total credits");
        }
    }
}
