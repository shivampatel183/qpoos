package com.qpoos.erp.purchase.bill.api;

import com.qpoos.erp.purchase.bill.application.PurchaseBillService;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillListResponse;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillRequest;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillResponse;
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
@RequestMapping("/api/purchase-bills")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PurchaseBillController {

    private final PurchaseBillService purchaseBillService;

    @PostMapping
    public ResponseEntity<PurchaseBillResponse> create(@Valid @RequestBody PurchaseBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseBillService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseBillListResponse>> list() {
        return ResponseEntity.ok(purchaseBillService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseBillResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseBillService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseBillResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseBillRequest request
    ) {
        return ResponseEntity.ok(purchaseBillService.update(id, request));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<PurchaseBillResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseBillService.post(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseBillResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseBillService.cancel(id));
    }
}
