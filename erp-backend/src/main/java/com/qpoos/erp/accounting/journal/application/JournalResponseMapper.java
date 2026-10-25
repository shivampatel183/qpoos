package com.qpoos.erp.accounting.journal.application;

import com.qpoos.erp.accounting.journal.domain.JournalEntryEntity;
import com.qpoos.erp.accounting.journal.dto.JournalEntryResponse;
import com.qpoos.erp.accounting.journal.dto.JournalLineResponse;
import org.springframework.stereotype.Component;

@Component
public class JournalResponseMapper {

    public JournalEntryResponse toResponse(JournalEntryEntity entry) {
        return new JournalEntryResponse(
                entry.getId(),
                entry.getCompany().getId(),
                entry.getJournalNumber(),
                entry.getAccountingDate(),
                entry.getSourceType(),
                entry.getSourceDocumentId(),
                entry.getEntryKind(),
                entry.getNarration(),
                entry.getStatus(),
                entry.getCreatedBy(),
                entry.getPostedBy(),
                entry.getCreatedAt(),
                entry.getPostedAt(),
                entry.getLines().stream()
                        .map(line -> new JournalLineResponse(
                                line.getId(),
                                line.getLineNo(),
                                line.getLedger().getId(),
                                line.getLedger().getCode(),
                                line.getLedger().getName(),
                                line.getDebitAmount(),
                                line.getCreditAmount(),
                                line.getDescription()
                        ))
                        .toList()
        );
    }
}
