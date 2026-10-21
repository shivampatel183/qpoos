package com.qpoos.erp.common.term;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment-terms")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentTermController {

    private final PaymentTermService paymentTermService;

    @PostMapping
    public ResponseEntity<PaymentTermResponse> create(@Valid @RequestBody PaymentTermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentTermService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentTermResponse>> list() {
        return ResponseEntity.ok(paymentTermService.list());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentTermResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentTermRequest request
    ) {
        return ResponseEntity.ok(paymentTermService.update(id, request));
    }
}