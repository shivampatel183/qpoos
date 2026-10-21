package com.qpoos.erp.purchase.transaction.api;

import com.qpoos.erp.auth.dto.MessageResponse;
import com.qpoos.erp.purchase.transaction.application.TransactionService;
import com.qpoos.erp.purchase.transaction.dto.TransactionListResponse;
import com.qpoos.erp.purchase.transaction.dto.TransactionRequest;
import com.qpoos.erp.purchase.transaction.dto.TransactionResponse;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TransactionListResponse>> list() {
        return ResponseEntity.ok(transactionService.list());
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> get(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(transactionService.get(transactionId));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.update(transactionId, request));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long transactionId
    ) {
        transactionService.delete(transactionId);
        return ResponseEntity.ok(new MessageResponse("Transaction deleted successfully."));
    }
}
