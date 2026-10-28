package com.qpoos.erp.payment.api;

import com.qpoos.erp.payment.application.PaymentService;
import com.qpoos.erp.payment.dto.PaymentRequest;
import com.qpoos.erp.payment.dto.PaymentResponse;
import com.qpoos.erp.payment.dto.PaymentReversalRequest;
import com.qpoos.erp.payment.dto.PaymentListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentRequest request) { return paymentService.create(request); }

    @GetMapping
    public Page<PaymentListResponse> list(@PageableDefault(size = 50, sort = {"paymentDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
                                          @RequestParam(required = false) String search) {
        return paymentService.list(pageable, search);
    }

    @GetMapping("/payable-bills")
    public List<com.qpoos.erp.payment.dto.PayableBillResponse> payableBills(@RequestParam Long vendorId) {
        return paymentService.payableBills(vendorId);
    }

    @GetMapping("/payable-documents")
    public List<com.qpoos.erp.payment.dto.PayableDocumentResponse> payableDocuments(@RequestParam Long vendorId) {
        return paymentService.payableDocuments(vendorId);
    }

    @GetMapping("/settlement-history")
    public List<com.qpoos.erp.payment.dto.SettlementHistoryResponse> settlementHistory(
            @RequestParam String documentType, @RequestParam Long documentId) {
        return paymentService.settlementHistory(documentType, documentId);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) { return paymentService.get(id); }

    @PutMapping("/{id}")
    public PaymentResponse update(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return paymentService.update(id, request);
    }

    @PostMapping("/{id}/post")
    public PaymentResponse post(@PathVariable Long id) { return paymentService.post(id); }

    @PostMapping("/{id}/void")
    public PaymentResponse voidDraft(@PathVariable Long id) { return paymentService.voidDraft(id); }

    @PostMapping("/{id}/reverse")
    public PaymentResponse reverse(@PathVariable Long id, @Valid @RequestBody PaymentReversalRequest request) {
        return paymentService.reverse(id, request);
    }
}
