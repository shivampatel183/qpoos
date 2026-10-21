package com.qpoos.erp.purchase.transaction.application;

import com.qpoos.erp.accounting.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.product.domain.ProductEntity;
import com.qpoos.erp.product.infrastructure.ProductRepository;
import com.qpoos.erp.purchase.transaction.domain.PaymentStatus;
import com.qpoos.erp.purchase.transaction.domain.TransactionEntity;
import com.qpoos.erp.purchase.transaction.domain.TransactionType;
import com.qpoos.erp.purchase.transaction.dto.TransactionLineRequest;
import com.qpoos.erp.purchase.transaction.dto.TransactionLineResponse;
import com.qpoos.erp.purchase.transaction.dto.TransactionListResponse;
import com.qpoos.erp.purchase.transaction.dto.TransactionRequest;
import com.qpoos.erp.purchase.transaction.dto.TransactionResponse;
import com.qpoos.erp.purchase.transaction.infrastructure.TransactionRepository;
import com.qpoos.erp.purchase.transaction.domain.TransactionLineEntity;
import com.qpoos.erp.purchase.transaction.domain.TransactionLineType;
import com.qpoos.erp.purchase.transaction.infrastructure.TransactionLineRepository;
import com.qpoos.erp.vendor.domain.VendorEntity;
import com.qpoos.erp.vendor.infrastructure.VendorRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionLineRepository transactionLineRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final LedgerRepository ledgerRepository;
    private final EntityManager entityManager;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        UUID userId = SecurityUtils.getUserId();

        validateLines(request.lines());

        TransactionEntity transaction = TransactionEntity.builder()
                .company(entityManager.getReference(CompanyEntity.class, companyId))
                .active(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        applyRequest(transaction, request, companyId);

        TransactionEntity saved = transactionRepository.save(transaction);
        saveLines(saved, request.lines(), companyId);
        return toResponse(saved);
    }

    @Transactional
    public List<TransactionListResponse> list() {
        return transactionRepository
                .findAllByCompany_IdAndActiveTrueOrderByTransactionDateDescIdDesc(SecurityUtils.getCompanyId())
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional
    public TransactionResponse get(Long transactionId) {
        return toResponse(getTransaction(SecurityUtils.getCompanyId(), transactionId));
    }

    @Transactional
    public TransactionResponse update(Long transactionId, TransactionRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        UUID userId = SecurityUtils.getUserId();
        TransactionEntity transaction = getTransaction(companyId, transactionId);

        validateLines(request.lines());
        applyRequest(transaction, request, companyId);
        transaction.setUpdatedBy(userId);

        transactionLineRepository.deleteAllByTransactionId(transactionId);
        saveLines(transaction, request.lines(), companyId);
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long transactionId) {
        TransactionEntity transaction = getTransaction(SecurityUtils.getCompanyId(), transactionId);
        transaction.setActive(false);
        transaction.setUpdatedBy(SecurityUtils.getUserId());
        transactionRepository.save(transaction);
    }

    private TransactionEntity getTransaction(UUID companyId, Long transactionId) {
        return transactionRepository.findByIdAndCompany_IdAndActiveTrue(transactionId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    private void applyRequest(
            TransactionEntity transaction,
            TransactionRequest request,
            UUID companyId
    ) {
        transaction.setTransactionDate(request.transactionDate());
        transaction.setTransactionType(request.transactionType());
        transaction.setBillNo(blankToNull(request.billNo()));
        transaction.setVendor(resolveVendor(companyId, request.vendorId()));
        transaction.setDueDate(request.dueDate());
        transaction.setReferenceNo(blankToNull(request.referenceNo()));
        transaction.setSubtotal(amountOrZero(request.subtotal()));
        transaction.setDiscountAmount(amountOrZero(request.discountAmount()));
        transaction.setTaxAmount(amountOrZero(request.taxAmount()));
        transaction.setRoundOff(amountOrZero(request.roundOff()));
        transaction.setTotalAmount(amountOrZero(request.totalAmount()));
        if (request.transactionType() == TransactionType.EXPENSE) {
            transaction.setPaymentDate(
                request.paymentDate() == null ? request.transactionDate() : request.paymentDate()
            );
            transaction.setPaymentStatus(PaymentStatus.PAID);
        } else {
            transaction.setPaymentDate(request.paymentDate());
            transaction.setPaymentStatus(
                request.paymentStatus() == null ? PaymentStatus.UNPAID : request.paymentStatus()
            );
        }
        transaction.setNotes(blankToNull(request.notes()));
        transaction.setAttachments(blankToNull(request.attachments()));
    }

    private VendorEntity resolveVendor(UUID companyId, Long vendorId) {
        if (vendorId == null) {
            return null;
        }
        return vendorRepository.findByIdAndCompany_IdAndActiveTrue(vendorId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private void saveLines(
            TransactionEntity transaction,
            List<TransactionLineRequest> lineRequests,
            UUID companyId
    ) {
        List<TransactionLineEntity> lines = lineRequests.stream()
                .map(request -> toLineEntity(transaction, request, companyId))
                .toList();
        transactionLineRepository.saveAll(lines);
    }

    private TransactionLineEntity toLineEntity(
            TransactionEntity transaction,
            TransactionLineRequest request,
            UUID companyId
    ) {
        TransactionLineEntity line = TransactionLineEntity.builder()
                .transaction(transaction)
                .build();
        line.setLineNo(request.lineNo());
        line.setLineType(request.lineType());
        line.setItem(resolveItem(companyId, request));
        line.setAccount(resolveAccount(companyId, request));
        line.setDescription(blankToNull(request.description()));
        line.setQuantity(amountOrZero(request.quantity()));
        line.setUnit(blankToNull(request.unit()));
        line.setRate(amountOrZero(request.rate()));
        line.setDiscount(amountOrZero(request.discount()));
        line.setTaxRate(amountOrZero(request.taxRate()));
        line.setTaxAmount(amountOrZero(request.taxAmount()));
        line.setAmount(amountOrZero(request.amount()));
        return line;
    }

    private ProductEntity resolveItem(UUID companyId, TransactionLineRequest request) {
        if (request.lineType() != TransactionLineType.ITEM) {
            return null;
        }
        if (request.itemId() == null) {
            throw new IllegalArgumentException("Item is required for ITEM lines");
        }
        if (request.accountId() != null) {
            throw new IllegalArgumentException("Account must be empty for ITEM lines");
        }
        return productRepository.findByIdAndCompany_IdAndActiveTrue(request.itemId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
    }

    private LedgerEntity resolveAccount(UUID companyId, TransactionLineRequest request) {
        if (request.lineType() != TransactionLineType.ACCOUNT) {
            return null;
        }
        if (request.accountId() == null) {
            throw new IllegalArgumentException("Account is required for ACCOUNT lines");
        }
        if (request.itemId() != null) {
            throw new IllegalArgumentException("Item must be empty for ACCOUNT lines");
        }
        LedgerEntity account = ledgerRepository.findByIdAndCompany_Id(request.accountId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!Boolean.TRUE.equals(account.getActive())) {
            throw new IllegalArgumentException("Account not found");
        }
        return account;
    }

    private void validateLines(List<TransactionLineRequest> lines) {
        Set<Integer> lineNumbers = new HashSet<>();
        for (TransactionLineRequest line : lines) {
            if (!lineNumbers.add(line.lineNo())) {
                throw new IllegalArgumentException("Line number must be unique");
            }
        }
    }

    private TransactionResponse toResponse(TransactionEntity transaction) {
        List<TransactionLineResponse> lines = transactionLineRepository
                .findAllByTransaction_IdAndTransaction_Company_IdOrderByLineNoAsc(
                        transaction.getId(),
                        transaction.getCompany().getId()
                )
                .stream()
                .map(this::toLineResponse)
                .toList();

        return new TransactionResponse(
                transaction.getId(),
                transaction.getCompany().getId(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                transaction.getBillNo(),
                transaction.getVendor() == null ? null : transaction.getVendor().getId(),
                transaction.getDueDate(),
                transaction.getReferenceNo(),
                transaction.getSubtotal(),
                transaction.getDiscountAmount(),
                transaction.getTaxAmount(),
                transaction.getRoundOff(),
                transaction.getTotalAmount(),
                transaction.getPaymentDate(),
                transaction.getPaymentStatus(),
                transaction.getNotes(),
                transaction.getAttachments(),
                transaction.getCreatedBy(),
                transaction.getUpdatedBy(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                lines
        );
    }

    private TransactionListResponse toListResponse(TransactionEntity transaction) {
        VendorEntity vendor = transaction.getVendor();
        return new TransactionListResponse(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                transaction.getBillNo(),
                vendor == null ? null : vendor.getId(),
                vendor == null ? null : vendor.getDisplayName(),
                transaction.getDueDate(),
                transaction.getPaymentDate(),
                transaction.getTotalAmount(),
                transaction.getPaymentStatus()
        );
    }

    private TransactionLineResponse toLineResponse(TransactionLineEntity line) {
        return new TransactionLineResponse(
                line.getId(),
                line.getLineNo(),
                line.getLineType(),
                line.getItem() == null ? null : line.getItem().getId(),
                line.getAccount() == null ? null : line.getAccount().getId(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnit(),
                line.getRate(),
                line.getDiscount(),
                line.getTaxRate(),
                line.getTaxAmount(),
                line.getAmount()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
