package com.qpoos.erp.purchase.order.api;

import com.qpoos.erp.purchase.order.application.PurchaseOrderService;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderListResponse;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderRequest;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderResponse;
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
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderListResponse>> list() {
        return ResponseEntity.ok(purchaseOrderService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request
    ) {
        return ResponseEntity.ok(purchaseOrderService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.approve(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }
}