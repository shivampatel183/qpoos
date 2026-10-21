package com.qpoos.erp.purchase.order.application;

import com.qpoos.erp.common.address.DeliveryAddressSnapshot;
import com.qpoos.erp.common.money.MoneyCalculator;
import com.qpoos.erp.common.number.DocumentNumberService;
import com.qpoos.erp.common.number.DocumentNumberType;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.common.term.PaymentTermEntity;
import com.qpoos.erp.common.term.PaymentTermRepository;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.product.domain.ProductEntity;
import com.qpoos.erp.product.infrastructure.ProductRepository;
import com.qpoos.erp.purchase.order.domain.PurchaseOrderEntity;
import com.qpoos.erp.purchase.order.domain.PurchaseOrderLineEntity;
import com.qpoos.erp.purchase.order.dto.DeliveryAddressRequest;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderLineRequest;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderLineResponse;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderListResponse;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderRequest;
import com.qpoos.erp.purchase.order.dto.PurchaseOrderResponse;
import com.qpoos.erp.purchase.order.infrastructure.PurchaseOrderLineRepository;
import com.qpoos.erp.purchase.order.infrastructure.PurchaseOrderRepository;
import com.qpoos.erp.purchase.shared.application.DocumentStatusRules;
import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;
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
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final PaymentTermRepository paymentTermRepository;
    private final DocumentNumberService documentNumberService;
    private final EntityManager entityManager;

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        validateLineNumbers(request.lines());
        PurchaseOrderEntity order = PurchaseOrderEntity.builder()
                .company(entityManager.getReference(CompanyEntity.class, companyId))
                .orderNumber(documentNumberService.next(DocumentNumberType.PURCHASE_ORDER))
                .status(PurchaseOrderStatus.DRAFT)
                .createdBy(SecurityUtils.getUserId())
                .updatedBy(SecurityUtils.getUserId())
                .build();
        applyRequest(order, request, companyId);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(order);
        saveLines(saved, request.lines(), companyId);
        return toResponse(saved);
    }

    @Transactional
    public List<PurchaseOrderListResponse> list() {
        return purchaseOrderRepository.findAllByCompany_IdOrderByOrderDateDescIdDesc(SecurityUtils.getCompanyId())
                .stream()
                .map(order -> new PurchaseOrderListResponse(
                        order.getId(), order.getOrderNumber(), order.getOrderDate(),
                        order.getVendor().getId(), order.getVendor().getDisplayName(),
                        order.getStatus(), order.getTotalAmount(), order.getExpectedDeliveryDate()
                ))
                .toList();
    }

    @Transactional
    public PurchaseOrderResponse get(Long id) {
        return toResponse(getOrder(SecurityUtils.getCompanyId(), id));
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        PurchaseOrderEntity order = getOrder(companyId, id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase orders can be edited");
        }
        validateLineNumbers(request.lines());
        applyRequest(order, request, companyId);
        lineRepository.deleteAllByPurchaseOrderId(id);
        saveLines(order, request.lines(), companyId);
        order.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(purchaseOrderRepository.save(order));
    }

    @Transactional
    public PurchaseOrderResponse submit(Long id) {
        return transition(id, PurchaseOrderStatus.SENT);
    }

    @Transactional
    public PurchaseOrderResponse approve(Long id) {
        return transition(id, PurchaseOrderStatus.APPROVED);
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        return transition(id, PurchaseOrderStatus.CANCELLED);
    }

    private PurchaseOrderResponse transition(Long id, PurchaseOrderStatus next) {
        PurchaseOrderEntity order = getOrder(SecurityUtils.getCompanyId(), id);
        DocumentStatusRules.requirePurchaseOrderTransition(order.getStatus(), next);
        order.setStatus(next);
        order.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(purchaseOrderRepository.save(order));
    }

    private void applyRequest(PurchaseOrderEntity order, PurchaseOrderRequest request, UUID companyId) {
        order.setOrderDate(request.orderDate());
        order.setVendor(vendorRepository.findByIdAndCompany_IdAndActiveTrue(request.vendorId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found")));
        order.setPaymentTerm(resolvePaymentTerm(request.paymentTermId(), companyId));
        order.setDeliveryTerm(blankToNull(request.deliveryTerm()));
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setDeliveryAddress(toAddress(request.deliveryAddress()));
        order.setRoundOff(MoneyCalculator.money(request.roundOff() == null ? BigDecimal.ZERO : request.roundOff()));
        order.setNotes(blankToNull(request.notes()));
        order.setAttachments(blankToNull(request.attachments()));

        List<MoneyCalculator.LineAmounts> amounts = request.lines().stream()
                .map(line -> MoneyCalculator.calculateLine(line.quantity(), line.rate(), line.discount(), line.taxRate()))
                .toList();
        MoneyCalculator.DocumentTotals totals = MoneyCalculator.calculateDocument(amounts, order.getRoundOff());
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discountAmount());
        order.setTaxAmount(totals.taxAmount());
        order.setTotalAmount(totals.totalAmount());
    }

    private void saveLines(PurchaseOrderEntity order, List<PurchaseOrderLineRequest> requests, UUID companyId) {
        List<PurchaseOrderLineEntity> lines = requests.stream()
                .map(request -> toLine(order, request, companyId))
                .toList();
        lineRepository.saveAll(lines);
    }

    private PurchaseOrderLineEntity toLine(PurchaseOrderEntity order, PurchaseOrderLineRequest request, UUID companyId) {
        ProductEntity product = productRepository.findByIdAndCompany_IdAndActiveTrue(request.productId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        MoneyCalculator.LineAmounts amounts = MoneyCalculator.calculateLine(
                request.quantity(), request.rate(), request.discount(), request.taxRate()
        );
        return PurchaseOrderLineEntity.builder()
                .purchaseOrder(order)
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

    private PurchaseOrderEntity getOrder(UUID companyId, Long id) {
        return purchaseOrderRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
    }

    private PaymentTermEntity resolvePaymentTerm(Long id, UUID companyId) {
        if (id == null) return null;
        return paymentTermRepository.findByIdAndCompany_Id(id, companyId)
                .filter(PaymentTermEntity::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Payment term not found"));
    }

    private DeliveryAddressSnapshot toAddress(DeliveryAddressRequest request) {
        if (request == null) return null;
        return DeliveryAddressSnapshot.builder()
                .line1(blankToNull(request.line1())).line2(blankToNull(request.line2()))
                .city(blankToNull(request.city())).state(blankToNull(request.state()))
                .country(blankToNull(request.country())).postalCode(blankToNull(request.postalCode()))
                .build();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrderEntity order) {
        DeliveryAddressSnapshot address = order.getDeliveryAddress();
        DeliveryAddressRequest addressResponse = address == null ? null : new DeliveryAddressRequest(
                address.getLine1(), address.getLine2(), address.getCity(), address.getState(),
                address.getCountry(), address.getPostalCode()
        );
        List<PurchaseOrderLineResponse> lines = lineRepository
            .findAllByPurchaseOrder_IdOrderByLineNoAsc(order.getId()).stream()
                .map(line -> new PurchaseOrderLineResponse(
                        line.getId(), line.getLineNo(), line.getProduct().getId(), line.getProduct().getName(),
                        line.getDescription(), line.getQuantity(), line.getUnit(), line.getRate(),
                        line.getDiscount(), line.getTaxRate(), line.getTaxAmount(), line.getAmount()
                )).toList();
        return new PurchaseOrderResponse(
                order.getId(), order.getOrderNumber(), order.getOrderDate(), order.getVendor().getId(),
                order.getVendor().getDisplayName(), order.getPaymentTerm() == null ? null : order.getPaymentTerm().getId(),
                order.getPaymentTerm() == null ? null : order.getPaymentTerm().getCode(), order.getDeliveryTerm(),
                order.getExpectedDeliveryDate(), addressResponse, order.getStatus(), order.getSubtotal(),
                order.getDiscountAmount(), order.getTaxAmount(), order.getRoundOff(), order.getTotalAmount(),
                order.getNotes(), order.getAttachments(), order.getCreatedAt(), order.getUpdatedAt(), lines
        );
    }

    private void validateLineNumbers(List<PurchaseOrderLineRequest> lines) {
        Set<Integer> numbers = new HashSet<>();
        for (PurchaseOrderLineRequest line : lines) {
            if (!numbers.add(line.lineNo())) throw new IllegalArgumentException("Line number must be unique");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}