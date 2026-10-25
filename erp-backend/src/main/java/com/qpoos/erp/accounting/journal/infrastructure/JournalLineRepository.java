package com.qpoos.erp.accounting.journal.infrastructure;

import com.qpoos.erp.accounting.journal.domain.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLineEntity, Long> {

    @Query("""
            select coalesce(sum(line.debitAmount), 0) as debitTotal,
                   coalesce(sum(line.creditAmount), 0) as creditTotal
            from JournalLineEntity line
            where line.ledger.id = :ledgerId
              and line.journalEntry.company.id = :companyId
              and line.journalEntry.status = com.qpoos.erp.accounting.journal.domain.JournalStatus.POSTED
              and (:openingDate is null or line.journalEntry.accountingDate > :openingDate)
            """)
    Optional<LedgerMovement> summarizePostedActivity(
            @Param("ledgerId") Long ledgerId,
            @Param("companyId") UUID companyId,
            @Param("openingDate") LocalDate openingDate
    );
}
