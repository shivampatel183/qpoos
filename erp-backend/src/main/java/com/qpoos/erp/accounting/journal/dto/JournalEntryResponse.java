package com.qpoos.erp.accounting.journal.dto;

import com.qpoos.erp.accounting.journal.domain.JournalEntryKind;
import com.qpoos.erp.accounting.journal.domain.JournalSourceType;
import com.qpoos.erp.accounting.journal.domain.JournalStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        Long id,
        UUID companyId,
        String journalNumber,
        LocalDate accountingDate,
        JournalSourceType sourceType,
        Long sourceDocumentId,
        JournalEntryKind entryKind,
        String narration,
        JournalStatus status,
        UUID createdBy,
        UUID postedBy,
        OffsetDateTime createdAt,
        OffsetDateTime postedAt,
        List<JournalLineResponse> lines
) {
}
