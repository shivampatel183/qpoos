package com.qpoos.erp.accounting.journal.application;

import com.qpoos.erp.accounting.journal.domain.JournalEntryEntity;
import com.qpoos.erp.accounting.journal.dto.JournalEntryResponse;
import com.qpoos.erp.accounting.journal.infrastructure.JournalEntryRepository;
import com.qpoos.erp.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalQueryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public JournalEntryResponse get(Long id) {
        return responseMapper.toResponse(getEntry(id));
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> list() {
        return journalEntryRepository
                .findAllByCompany_IdOrderByAccountingDateDescIdDesc(SecurityUtils.getCompanyId())
                .stream()
                .map(responseMapper::toResponse)
                .toList();
    }

    private JournalEntryEntity getEntry(Long id) {
        return journalEntryRepository.findByIdAndCompany_Id(id, SecurityUtils.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }
}
