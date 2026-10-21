package com.qpoos.erp.accounting.accountcatalog.api;

import com.qpoos.erp.accounting.accountcatalog.application.AccountTypeDefinitionService;
import com.qpoos.erp.accounting.accountcatalog.dto.AccountTypeDefinitionRequest;
import com.qpoos.erp.accounting.accountcatalog.dto.AccountTypeDefinitionResponse;
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
@RequestMapping("/api/accounting/account-types")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AccountTypeDefinitionController {

    private final AccountTypeDefinitionService definitionService;

    @GetMapping
    public ResponseEntity<List<AccountTypeDefinitionResponse>> list() {
        return ResponseEntity.ok(definitionService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountTypeDefinitionResponse> create(
            @Valid @RequestBody AccountTypeDefinitionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.create(request));
    }

    @PutMapping("/{definitionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountTypeDefinitionResponse> update(
            @PathVariable Long definitionId,
            @Valid @RequestBody AccountTypeDefinitionRequest request
    ) {
        return ResponseEntity.ok(definitionService.update(definitionId, request));
    }

    @DeleteMapping("/{definitionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable Long definitionId) {
        definitionService.deactivate(definitionId);
        return ResponseEntity.ok(new MessageResponse("Account type deactivated successfully."));
    }
}
