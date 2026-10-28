package com.qpoos.erp.payment.application;

import com.qpoos.erp.accounting.chartofaccount.domain.LedgerType;
import com.qpoos.erp.accounting.chartofaccount.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.chartofaccount.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.journal.application.AccountingPostingService;
import com.qpoos.erp.accounting.journal.domain.JournalEntryEntity;
import com.qpoos.erp.common.number.DocumentNumberService;
import com.qpoos.erp.common.number.DocumentNumberType;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.payment.domain.PaymentAllocationEntity;
import com.qpoos.erp.payment.domain.PaymentEntity;
import com.qpoos.erp.payment.domain.PaymentStatus;
import com.qpoos.erp.payment.dto.PaymentAllocationRequest;
import com.qpoos.erp.payment.dto.PaymentRequest;
import com.qpoos.erp.payment.dto.PaymentReversalRequest;
import com.qpoos.erp.payment.dto.PaymentResponse;
import com.qpoos.erp.payment.dto.PaymentListResponse;
import com.qpoos.erp.payment.dto.PayableBillResponse;
import com.qpoos.erp.payment.dto.PayableDocumentResponse;
import com.qpoos.erp.payment.dto.SettlementHistoryResponse;
import com.qpoos.erp.payment.infrastructure.PaymentAllocationRepository;
import com.qpoos.erp.payment.infrastructure.PaymentRepository;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import com.qpoos.erp.purchase.bill.infrastructure.PurchaseBillRepository;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
import com.qpoos.erp.purchase.vendor.domain.VendorEntity;
import com.qpoos.erp.purchase.vendor.infrastructure.VendorRepository;
import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionType;
import com.qpoos.erp.purchase.expense.infrastructure.TransactionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final PurchaseBillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final DocumentNumberService documentNumberService;
    private final AccountingPostingService accountingPostingService;
    private final EntityManager entityManager;

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        UUID userId = SecurityUtils.getUserId();
        PaymentEntity payment = PaymentEntity.builder()
                .company(entityManager.getReference(CompanyEntity.class, companyId))
                .paymentNumber(documentNumberService.next(DocumentNumberType.PAYMENT, request.paymentDate()))
                .paymentDate(request.paymentDate())
                .vendor(resolveVendor(request.vendorId(), companyId))
                .paymentMethod(request.paymentMethod())
                .sourceLedger(resolveLedger(request.sourceLedgerId(), companyId))
                .amount(money(request.amount()))
                .referenceNumber(blankToNull(request.referenceNumber()))
                .notes(blankToNull(request.notes()))
                .status(PaymentStatus.DRAFT)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        replaceAllocations(payment, request.allocations(), companyId);
        return toResponse(paymentRepository.saveAndFlush(payment), companyId);
    }

    @Transactional(readOnly = true)
    public Page<PaymentListResponse> list(Pageable pageable, String search) {
        UUID companyId = SecurityUtils.getCompanyId();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return paymentRepository.findPaymentSummaries(companyId, normalizedSearch, pageable);
    }

    @Transactional(readOnly = true)
    public List<PayableBillResponse> payableBills(Long vendorId) {
        UUID companyId = SecurityUtils.getCompanyId();
        if (vendorId == null) return List.of();
        Map<Long, BigDecimal> settled = allocationRepository.sumPostedByDocument(companyId, "PURCHASE_BILL").stream()
                .collect(java.util.stream.Collectors.toMap(row -> ((Number) row[0]).longValue(),
                        row -> (BigDecimal) row[1]));
        return billRepository.findAllByCompany_IdAndVendor_IdAndStatusOrderByDueDateAscIdAsc(
                        companyId, vendorId, PurchaseBillStatus.POSTED).stream()
                .map(bill -> {
                    BigDecimal paid = settled.getOrDefault(bill.getId(), BigDecimal.ZERO);
                    BigDecimal outstanding = money(bill.getTotalAmount()).subtract(paid).max(BigDecimal.ZERO);
                    return new PayableBillResponse(bill.getId(), bill.getBillNumber(), bill.getBillDate(), bill.getDueDate(),
                            bill.getVendor().getId(), bill.getVendor().getDisplayName(), bill.getTotalAmount(), paid, outstanding);
                }).filter(bill -> bill.outstandingAmount().signum() > 0).toList();
    }

    @Transactional(readOnly = true)
    public List<PayableDocumentResponse> payableDocuments(Long vendorId) {
        UUID companyId = SecurityUtils.getCompanyId();
        if (vendorId == null) return List.of();
        Map<Long, BigDecimal> paidBills = totalsByDocument(companyId, "PURCHASE_BILL");
        Map<Long, BigDecimal> paidExpenses = totalsByDocument(companyId, "EXPENSE");
        List<PayableDocumentResponse> result = new java.util.ArrayList<>();
        billRepository.findAllByCompany_IdAndVendor_IdAndStatusOrderByDueDateAscIdAsc(
                        companyId, vendorId, PurchaseBillStatus.POSTED).forEach(bill -> {
                    BigDecimal settled = paidBills.getOrDefault(bill.getId(), BigDecimal.ZERO);
                    BigDecimal outstanding = money(bill.getTotalAmount()).subtract(settled).max(BigDecimal.ZERO);
                    if (outstanding.signum() > 0) result.add(new PayableDocumentResponse("PURCHASE_BILL", bill.getId(),
                            bill.getBillNumber(), bill.getBillDate(), bill.getDueDate(), vendorId,
                            bill.getVendor().getDisplayName(), bill.getTotalAmount(), settled, outstanding));
                });
        transactionRepository.findAllByCompany_IdAndVendor_IdAndTransactionTypeAndPaymentStatusInAndActiveTrueOrderByDueDateAscIdAsc(
                companyId, vendorId, TransactionType.EXPENSE,
                List.of(com.qpoos.erp.purchase.expense.domain.PaymentStatus.UNPAID,
                        com.qpoos.erp.purchase.expense.domain.PaymentStatus.PARTIALLY_PAID)
        ).forEach(expense -> {
            if (expense.getJournalEntry() == null) return;
            BigDecimal settled = paidExpenses.getOrDefault(expense.getId(), BigDecimal.ZERO);
            BigDecimal outstanding = money(expense.getTotalAmount()).subtract(settled).max(BigDecimal.ZERO);
            if (outstanding.signum() > 0) result.add(new PayableDocumentResponse("EXPENSE", expense.getId(),
                    expense.getBillNo() == null ? "EXP-" + expense.getId() : expense.getBillNo(),
                    expense.getTransactionDate(), expense.getDueDate(), vendorId,
                    expense.getVendor().getDisplayName(), expense.getTotalAmount(), settled, outstanding));
        });
        return result.stream().sorted(java.util.Comparator.comparing(PayableDocumentResponse::dueDate,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))).toList();
    }

    private Map<Long, BigDecimal> totalsByDocument(UUID companyId, String type) {
        return allocationRepository.sumPostedByDocument(companyId, type).stream()
                .collect(java.util.stream.Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> (BigDecimal) row[1]));
    }

    @Transactional(readOnly = true)
    public List<SettlementHistoryResponse> settlementHistory(String documentType, Long documentId) {
        UUID companyId = SecurityUtils.getCompanyId();
        String type = documentType == null ? "" : documentType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("PURCHASE_BILL", "EXPENSE").contains(type)) throw new IllegalArgumentException("Unsupported settlement document type");
        return allocationRepository.findAllByDocumentTypeAndDocumentIdAndCompany_Id(type, documentId, companyId).stream()
                .filter(a -> a.getPayment().getStatus() == PaymentStatus.POSTED || a.getPayment().getStatus() == PaymentStatus.REVERSED)
                .sorted(java.util.Comparator.comparing((PaymentAllocationEntity a) -> a.getPayment().getPaymentDate()).thenComparing(a -> a.getPayment().getId()))
                .map(a -> new SettlementHistoryResponse(a.getPayment().getId(), a.getPayment().getPaymentNumber(),
                        a.getPayment().getPaymentDate(), a.getPayment().getPaymentMethod(), a.getPayment().getAmount(),
                        a.getAllocatedAmount(), a.getPayment().getStatus(), a.getPayment().getReferenceNumber(),
                        a.getPayment().getVoucherId(), a.getPayment().getReversalVoucherId())).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        UUID companyId = SecurityUtils.getCompanyId();
        return toResponse(paymentRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found")), companyId);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        PaymentEntity payment = paymentRepository.findForPosting(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.DRAFT) throw new IllegalArgumentException("Only draft payments can be edited");
        payment.setPaymentDate(request.paymentDate());
        payment.setVendor(resolveVendor(request.vendorId(), companyId));
        payment.setPaymentMethod(request.paymentMethod());
        payment.setSourceLedger(resolveLedger(request.sourceLedgerId(), companyId));
        payment.setAmount(money(request.amount()));
        payment.setReferenceNumber(blankToNull(request.referenceNumber()));
        payment.setNotes(blankToNull(request.notes()));
        payment.setUpdatedBy(SecurityUtils.getUserId());
        payment.getAllocations().clear();
        replaceAllocations(payment, request.allocations(), companyId);
        return toResponse(paymentRepository.saveAndFlush(payment), companyId);
    }

    @Transactional
    public PaymentResponse post(Long id) {
        JournalEntryEntity journal = accountingPostingService.postPayment(id);
        PaymentEntity payment = paymentRepository.findByIdAndCompany_Id(id, SecurityUtils.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setVoucherId(journal.getId());
        return toResponse(payment, SecurityUtils.getCompanyId());
    }

    @Transactional
    public PaymentResponse voidDraft(Long id) {
        UUID companyId = SecurityUtils.getCompanyId();
        PaymentEntity payment = paymentRepository.findForPosting(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.DRAFT) throw new IllegalArgumentException("Only draft payments can be voided");
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(paymentRepository.save(payment), companyId);
    }

    @Transactional
    public PaymentResponse reverse(Long id, PaymentReversalRequest request) {
        accountingPostingService.reversePayment(id, request.reversalDate(), request.reason());
        UUID companyId = SecurityUtils.getCompanyId();
        return toResponse(paymentRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found")), companyId);
    }

    private void replaceAllocations(PaymentEntity payment, List<PaymentAllocationRequest> requests, UUID companyId) {
        Set<String> seen = new HashSet<>();
        for (PaymentAllocationRequest request : requests) {
            String documentType = request.documentType().trim().toUpperCase(java.util.Locale.ROOT);
            if (!seen.add(documentType + ":" + request.documentId())) throw new IllegalArgumentException("A document can only appear once in a payment");
            BigDecimal allocated = money(request.allocatedAmount());
            BigDecimal totalAmount;
            BigDecimal paid;
            if ("PURCHASE_BILL".equals(documentType)) {
                PurchaseBillEntity bill = billRepository.findByIdAndCompany_Id(request.documentId(), companyId)
                        .orElseThrow(() -> new IllegalArgumentException("Purchase bill not found"));
                if (bill.getStatus() != PurchaseBillStatus.POSTED || bill.getJournalEntry() == null
                        || !bill.getVendor().getId().equals(payment.getVendor().getId())) {
                    throw new IllegalArgumentException("Allocations must target posted bills for the selected vendor");
                }
                totalAmount = bill.getTotalAmount();
            } else if ("EXPENSE".equals(documentType)) {
                TransactionEntity expense = transactionRepository.findByIdAndCompany_IdAndActiveTrue(request.documentId(), companyId)
                        .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
                if (expense.getTransactionType() != TransactionType.EXPENSE || expense.getJournalEntry() == null
                        || expense.getVendor() == null || !expense.getVendor().getId().equals(payment.getVendor().getId())
                        || (expense.getPaymentStatus() != com.qpoos.erp.purchase.expense.domain.PaymentStatus.UNPAID
                            && expense.getPaymentStatus() != com.qpoos.erp.purchase.expense.domain.PaymentStatus.PARTIALLY_PAID)) {
                    throw new IllegalArgumentException("Allocation must target an unpaid payable expense for the selected vendor");
                }
                totalAmount = expense.getTotalAmount();
            } else {
                throw new IllegalArgumentException("Unsupported settlement document type");
            }
            paid = allocationRepository.sumPostedForDocument(companyId, documentType, request.documentId());
            if (paid.add(allocated).compareTo(money(totalAmount)) > 0) throw new IllegalArgumentException("Allocation exceeds document outstanding balance");
            payment.getAllocations().add(PaymentAllocationEntity.builder()
                    .company(entityManager.getReference(CompanyEntity.class, companyId))
                    .payment(payment)
                    .documentType(documentType)
                    .documentId(request.documentId())
                    .allocatedAmount(allocated)
                    .build());
        }
    }

    private VendorEntity resolveVendor(Long id, UUID companyId) {
        return vendorRepository.findByIdAndCompany_IdAndActiveTrue(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private LedgerEntity resolveLedger(Long id, UUID companyId) {
        LedgerEntity ledger = ledgerRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Source ledger not found"));
        if (!Boolean.TRUE.equals(ledger.getActive()) || !Boolean.TRUE.equals(ledger.getPostingAllowed())
                || (ledger.getLedgerType() != LedgerType.CASH && ledger.getLedgerType() != LedgerType.BANK)) {
            throw new IllegalArgumentException("Source ledger must be an active, postable cash or bank ledger");
        }
        return ledger;
    }

    private PaymentResponse toResponse(PaymentEntity payment, UUID companyId) {
        List<PaymentResponse.Allocation> allocations = allocationRepository
                .findAllByPayment_IdAndCompany_Id(payment.getId(), companyId).stream().map(allocation -> {
                    String number = null;
                    BigDecimal outstanding = null;
                    if ("PURCHASE_BILL".equals(allocation.getDocumentType())) {
                        PurchaseBillEntity bill = billRepository.findByIdAndCompany_Id(allocation.getDocumentId(), companyId).orElse(null);
                        if (bill != null) {
                            number = bill.getBillNumber();
                            BigDecimal paid = allocationRepository.sumPostedForDocument(companyId, "PURCHASE_BILL", bill.getId());
                            outstanding = money(bill.getTotalAmount()).subtract(paid);
                        }
                    } else if ("EXPENSE".equals(allocation.getDocumentType())) {
                        TransactionEntity expense = transactionRepository.findByIdAndCompany_IdAndActiveTrue(allocation.getDocumentId(), companyId).orElse(null);
                        if (expense != null) {
                            number = expense.getBillNo() == null ? "EXP-" + expense.getId() : expense.getBillNo();
                            BigDecimal paid = allocationRepository.sumPostedForDocument(companyId, "EXPENSE", expense.getId());
                            outstanding = money(expense.getTotalAmount()).subtract(paid);
                        }
                    }
                    return new PaymentResponse.Allocation(allocation.getId(), allocation.getDocumentType(),
                            allocation.getDocumentId(), number, allocation.getAllocatedAmount(), outstanding);
                }).toList();
        return new PaymentResponse(payment.getId(), payment.getPaymentNumber(), payment.getPaymentDate(),
                payment.getVendor().getId(), payment.getVendor().getDisplayName(), payment.getPaymentMethod(),
                payment.getSourceLedger() == null ? null : payment.getSourceLedger().getId(),
                payment.getSourceLedger() == null ? null : payment.getSourceLedger().getName(), payment.getAmount(),
                payment.getReferenceNumber(), payment.getNotes(), payment.getStatus(), payment.getVoucherId(),
                payment.getReversalVoucherId(), payment.getReversalReason(), payment.getCreatedAt(), allocations);
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2);
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
