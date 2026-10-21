package com.qpoos.erp.purchase.bill.application;

import com.qpoos.erp.common.money.MoneyCalculator;
import com.qpoos.erp.common.number.DocumentNumberService;
import com.qpoos.erp.common.number.DocumentNumberType;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.common.term.PaymentTermEntity;
import com.qpoos.erp.common.term.PaymentTermRepository;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.product.domain.ProductEntity;
import com.qpoos.erp.product.infrastructure.ProductRepository;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillLineEntity;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillLineRequest;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillLineResponse;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillListResponse;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillRequest;
import com.qpoos.erp.purchase.bill.dto.PurchaseBillResponse;
import com.qpoos.erp.purchase.bill.infrastructure.PurchaseBillLineRepository;
import com.qpoos.erp.purchase.bill.infrastructure.PurchaseBillRepository;
import com.qpoos.erp.purchase.order.domain.PurchaseOrderEntity;
import com.qpoos.erp.purchase.order.infrastructure.PurchaseOrderRepository;
import com.qpoos.erp.purchase.shared.application.DocumentStatusRules;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
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
public class PurchaseBillService {

    private final PurchaseBillRepository purchaseBillRepository;
    private final PurchaseBillLineRepository lineRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final PaymentTermRepository paymentTermRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DocumentNumberService documentNumberService;
    private final EntityManager entityManager;

    @Transactional
    public PurchaseBillResponse create(PurchaseBillRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        validateLineNumbers(request.lines());
        PurchaseBillEntity bill = PurchaseBillEntity.builder()
                .company(entityManager.getReference(CompanyEntity.class, companyId))
                .billNumber(documentNumberService.next(DocumentNumberType.PURCHASE_BILL))
                .status(PurchaseBillStatus.DRAFT)
                .createdBy(SecurityUtils.getUserId())
                .updatedBy(SecurityUtils.getUserId())
                .build();
        applyRequest(bill, request, companyId);
        PurchaseBillEntity saved = purchaseBillRepository.save(bill);
        saveLines(saved, request.lines(), companyId);
        return toResponse(saved);
    }

    @Transactional
    public List<PurchaseBillListResponse> list() {
        return purchaseBillRepository.findAllByCompany_IdOrderByBillDateDescIdDesc(SecurityUtils.getCompanyId())
                .stream()
                .map(bill -> new PurchaseBillListResponse(
                        bill.getId(), bill.getBillNumber(), bill.getBillDate(),
                        bill.getVendor().getId(), bill.getVendor().getDisplayName(),
                        bill.getStatus(), bill.getTotalAmount(), bill.getDueDate()
                ))
                .toList();
    }

    @Transactional
    public PurchaseBillResponse get(Long id) {
        return toResponse(getBill(SecurityUtils.getCompanyId(), id));
    }

    @Transactional
    public PurchaseBillResponse update(Long id, PurchaseBillRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        PurchaseBillEntity bill = getBill(companyId, id);
        if (bill.getStatus() != PurchaseBillStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase bills can be edited");
        }
        validateLineNumbers(request.lines());
        applyRequest(bill, request, companyId);
        lineRepository.deleteAllByPurchaseBillId(id);
        saveLines(bill, request.lines(), companyId);
        bill.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(purchaseBillRepository.save(bill));
    }

    @Transactional
    public PurchaseBillResponse post(Long id) {
        PurchaseBillEntity bill = getBill(SecurityUtils.getCompanyId(), id);
        if (bill.getStatus() == PurchaseBillStatus.POSTED) {
            if (bill.getPostedVoucherId() == null) {
                bill.setPostedVoucherId(bill.getId());
                bill.setUpdatedBy(SecurityUtils.getUserId());
                purchaseBillRepository.save(bill);
            }
            return toResponse(bill);
        }
        DocumentStatusRules.requirePurchaseBillTransition(bill.getStatus(), PurchaseBillStatus.POSTED);
        bill.setStatus(PurchaseBillStatus.POSTED);
        bill.setPostedVoucherId(bill.getId());
        bill.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(purchaseBillRepository.save(bill));
    }

    @Transactional
    public PurchaseBillResponse cancel(Long id) {
        PurchaseBillEntity bill = getBill(SecurityUtils.getCompanyId(), id);
        DocumentStatusRules.requirePurchaseBillTransition(bill.getStatus(), PurchaseBillStatus.CANCELLED);
        bill.setStatus(PurchaseBillStatus.CANCELLED);
        bill.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(purchaseBillRepository.save(bill));
    }

    private void applyRequest(PurchaseBillEntity bill, PurchaseBillRequest request, UUID companyId) {
        bill.setBillDate(request.billDate());
        bill.setVendor(vendorRepository.findByIdAndCompany_IdAndActiveTrue(request.vendorId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found")));
        bill.setPaymentTerm(resolvePaymentTerm(request.paymentTermId(), companyId));
        bill.setDueDate(request.dueDate());
        bill.setPurchaseOrder(resolvePurchaseOrder(request.purchaseOrderId(), companyId));
        bill.setRoundOff(MoneyCalculator.money(request.roundOff() == null ? BigDecimal.ZERO : request.roundOff()));
        bill.setNotes(blankToNull(request.notes()));
        bill.setAttachments(blankToNull(request.attachments()));

        List<MoneyCalculator.LineAmounts> amounts = request.lines().stream()
                .map(line -> MoneyCalculator.calculateLine(line.quantity(), line.rate(), line.discount(), line.taxRate()))
                .toList();
        MoneyCalculator.DocumentTotals totals = MoneyCalculator.calculateDocument(amounts, bill.getRoundOff());
        bill.setSubtotal(totals.subtotal());
        bill.setDiscountAmount(totals.discountAmount());
        bill.setTaxAmount(totals.taxAmount());
        bill.setTotalAmount(totals.totalAmount());
    }

    private void saveLines(PurchaseBillEntity bill, List<PurchaseBillLineRequest> requests, UUID companyId) {
        List<PurchaseBillLineEntity> lines = requests.stream()
                .map(request -> toLine(bill, request, companyId))
                .toList();
        lineRepository.saveAll(lines);
    }

    private PurchaseBillLineEntity toLine(PurchaseBillEntity bill, PurchaseBillLineRequest request, UUID companyId) {
        ProductEntity product = productRepository.findByIdAndCompany_IdAndActiveTrue(request.productId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        MoneyCalculator.LineAmounts amounts = MoneyCalculator.calculateLine(
                request.quantity(), request.rate(), request.discount(), request.taxRate()
        );
        return PurchaseBillLineEntity.builder()
                .purchaseBill(bill)
                .lineNo(request.lineNo())
                .product(product)
                .description(blankToNull(request.description()))
                .quantity(request.quantity())
                .unit(blankToNull(request.unit()))
                .rate(MoneyCalculator.money(request.rate()))
                .discount(amounts.discount())
                .taxRate(MoneyCalculator.money(request.taxRate() == null ? BigDecimal.ZERO : request.taxRate()))
                .taxAmount(amounts.taxAmount())
                .amount(amounts.amount())
                .build();
    }

    private PurchaseBillEntity getBill(UUID companyId, Long id) {
        return purchaseBillRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase bill not found"));
    }

    private PaymentTermEntity resolvePaymentTerm(Long id, UUID companyId) {
        if (id == null) return null;
        return paymentTermRepository.findByIdAndCompany_Id(id, companyId)
                .filter(PaymentTermEntity::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Payment term not found"));
    }

    private PurchaseOrderEntity resolvePurchaseOrder(Long id, UUID companyId) {
        if (id == null) return null;
        return purchaseOrderRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
    }

    private PurchaseBillResponse toResponse(PurchaseBillEntity bill) {
        List<PurchaseBillLineResponse> lines = lineRepository
                .findAllByPurchaseBill_IdOrderByLineNoAsc(bill.getId()).stream()
                .map(line -> new PurchaseBillLineResponse(
                        line.getId(), line.getLineNo(), line.getProduct().getId(), line.getProduct().getName(),
                        line.getDescription(), line.getQuantity(), line.getUnit(), line.getRate(),
                        line.getDiscount(), line.getTaxRate(), line.getTaxAmount(), line.getAmount()
                )).toList();
        return new PurchaseBillResponse(
                bill.getId(), bill.getBillNumber(), bill.getBillDate(), bill.getVendor().getId(),
                bill.getVendor().getDisplayName(), bill.getPaymentTerm() == null ? null : bill.getPaymentTerm().getId(),
                bill.getPaymentTerm() == null ? null : bill.getPaymentTerm().getCode(), bill.getDueDate(),
                bill.getPurchaseOrder() == null ? null : bill.getPurchaseOrder().getId(), bill.getStatus(),
                bill.getSubtotal(), bill.getDiscountAmount(), bill.getTaxAmount(), bill.getRoundOff(),
                bill.getTotalAmount(), bill.getPostedVoucherId(), bill.getNotes(), bill.getAttachments(),
                bill.getCreatedAt(), bill.getUpdatedAt(), lines
        );
    }

    private void validateLineNumbers(List<PurchaseBillLineRequest> lines) {
        Set<Integer> numbers = new HashSet<>();
        for (PurchaseBillLineRequest line : lines) {
            if (!numbers.add(line.lineNo())) throw new IllegalArgumentException("Line number must be unique");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
