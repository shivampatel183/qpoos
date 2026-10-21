package com.qpoos.erp.accounting.ledger.api;

import com.qpoos.erp.accounting.ledger.application.LedgerService;
import com.qpoos.erp.accounting.ledger.dto.LedgerRequest;
import com.qpoos.erp.accounting.ledger.dto.LedgerResponse;
import com.qpoos.erp.auth.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/ledgers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<LedgerResponse> create(
            @Valid @RequestBody LedgerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledgerService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LedgerResponse>> list() {
        return ResponseEntity.ok(ledgerService.list());
    }

    @GetMapping("/{ledgerId}")
    public ResponseEntity<LedgerResponse> get(
            @PathVariable Long ledgerId
    ) {
        return ResponseEntity.ok(ledgerService.get(ledgerId));
    }

    @PutMapping("/{ledgerId}")
    public ResponseEntity<LedgerResponse> update(
            @PathVariable Long ledgerId,
            @Valid @RequestBody LedgerRequest request
    ) {
        return ResponseEntity.ok(ledgerService.update(ledgerId, request));
    }

    @DeleteMapping("/{ledgerId}")
    public ResponseEntity<MessageResponse> deactivate(
            @PathVariable Long ledgerId
    ) {
        ledgerService.deactivate(ledgerId);
        return ResponseEntity.ok(new MessageResponse("Ledger deactivated successfully."));
    }
}
