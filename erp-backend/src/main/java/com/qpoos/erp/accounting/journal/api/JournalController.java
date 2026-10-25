package com.qpoos.erp.accounting.journal.api;

import com.qpoos.erp.accounting.journal.application.JournalQueryService;
import com.qpoos.erp.accounting.journal.dto.JournalEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/journals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class JournalController {

    private final JournalQueryService journalQueryService;

    @GetMapping
    public ResponseEntity<List<JournalEntryResponse>> list() {
        return ResponseEntity.ok(journalQueryService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(journalQueryService.get(id));
    }
}
