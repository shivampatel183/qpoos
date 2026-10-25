package com.qpoos.erp.accounting.journal.infrastructure;

import com.qpoos.erp.accounting.journal.domain.JournalEntryEntity;
import com.qpoos.erp.accounting.journal.domain.JournalEntryKind;
import com.qpoos.erp.accounting.journal.domain.JournalSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Long> {

    Optional<JournalEntryEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    Optional<JournalEntryEntity> findByCompany_IdAndSourceTypeAndSourceDocumentIdAndEntryKind(
            UUID companyId,
            JournalSourceType sourceType,
            Long sourceDocumentId,
            JournalEntryKind entryKind
    );

    List<JournalEntryEntity> findAllByCompany_IdOrderByAccountingDateDescIdDesc(UUID companyId);
}
