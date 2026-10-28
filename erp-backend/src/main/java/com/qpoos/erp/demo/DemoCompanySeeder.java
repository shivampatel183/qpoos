package com.qpoos.erp.demo;

import java.time.LocalDate;
import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import com.qpoos.erp.accounting.chartofaccount.setup.AccountingBootstrapService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.customerhub.customer.domain.CustomerEntity;
import com.qpoos.erp.inventory.product.domain.ProductEntity;
import com.qpoos.erp.inventory.product.domain.ProductType;
import com.qpoos.erp.purchase.vendor.domain.VendorEntity;
import com.qpoos.erp.purchase.order.domain.PurchaseOrderEntity;
import com.qpoos.erp.purchase.order.domain.PurchaseOrderLineEntity;
import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillLineEntity;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionLineEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionLineType;
import com.qpoos.erp.purchase.expense.domain.TransactionType;
import com.qpoos.erp.purchase.expense.domain.PaymentStatus;
import com.qpoos.erp.accounting.chartofaccount.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.chartofaccount.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.payment.domain.PaymentEntity;
import com.qpoos.erp.payment.domain.PaymentMethod;

import lombok.RequiredArgsConstructor;

/** Creates a login-ready sample company for local development databases. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed-company", havingValue = "true")
public class DemoCompanySeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@qpoos.local";
    public static final String DEMO_PASSWORD = "Demo123!";
    private static final String DEMO_COMPANY_NAME = "Qpoos Demo Company";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountingBootstrapService accountingBootstrapService;
    private final LedgerRepository ledgerRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserEntity user = userRepository.findByEmailIgnoreCase(DEMO_EMAIL).orElseGet(() ->
                userRepository.save(UserEntity.builder()
                        .email(DEMO_EMAIL)
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .role("user")
                        .isActive(true)
                        .emailVerified(true)
                        .emailVerifiedAt(java.time.OffsetDateTime.now())
                        .build()));

        CompanyEntity company = companyRepository.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId()).stream()
                .filter(existing -> DEMO_COMPANY_NAME.equals(existing.getName())).findFirst().orElse(null);
        if (company != null) {
            accountingBootstrapService.ensureDefaultChartForCompany(company);
            seedSampleRecords(company, user.getId());
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate financialYearStart = today.getMonthValue() >= 4
                ? LocalDate.of(today.getYear(), 4, 1)
                : LocalDate.of(today.getYear() - 1, 4, 1);
        company = companyRepository.save(CompanyEntity.builder()
                .userId(user.getId())
                .name(DEMO_COMPANY_NAME)
                .financialYearStart(financialYearStart.toString())
                .financialYearEnd(financialYearStart.plusYears(1).minusDays(1).toString())
                .addressLine1("123 Demo Street")
                .city("Bengaluru")
                .state("Karnataka")
                .country("India")
                .pincode("560001")
                .phoneNumber("+91-0000000000")
                .email(DEMO_EMAIL)
                .website("https://example.com")
                .isActive(true)
                .build());

        accountingBootstrapService.ensureDefaultChartForCompany(company);
        seedSampleRecords(company, user.getId());
    }

    private void seedSampleRecords(CompanyEntity company, java.util.UUID userId) {
        Long existing = entityManager.createQuery("select count(v) from VendorEntity v where v.company.id = :companyId", Long.class)
                .setParameter("companyId", company.getId()).getSingleResult();
        if (existing > 0) return;

        LocalDate today = LocalDate.now();
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        BigDecimal total = new BigDecimal("600.00");
        BigDecimal rate = new BigDecimal("120.00");
        BigDecimal qty = new BigDecimal("5.000");
        VendorEntity vendor = entityManager.merge(VendorEntity.builder().company(company)
                .companyName("Demo Office Supplies Pvt Ltd").displayName("Demo Office Supplies")
                .firstName("Asha").lastName("Sharma").email("vendor@example.com")
                .mobileNo("+91-9876500001").streetAddress1("45 Market Road").city("Bengaluru")
                .state("Karnataka").country("India").pinCode("560001").active(true).build());
        entityManager.persist(CustomerEntity.builder().company(company).companyName("Demo Retail Customer Ltd")
                .displayName("Demo Retail Customer").firstName("Rahul").lastName("Mehta")
                .email("customer@example.com").mobileNo("+91-9876500002").streetAddress1("88 Commercial Street")
                .city("Bengaluru").state("Karnataka").country("India").pinCode("560002").active(true).build());
        ProductEntity product = entityManager.merge(ProductEntity.builder().company(company)
                .name("A4 Copier Paper (Demo)").description("Sample office paper for purchase workflows")
                .type(ProductType.INVENTORY).sku("DEMO-PAPER-001").trackInventory(true)
                .quantityOnHand(new BigDecimal("25.000")).openingQuantity(new BigDecimal("25.000"))
                .openingValue(new BigDecimal("3000.00")).salePrice(new BigDecimal("150.00"))
                .purchaseCost(rate).active(true).build());

        PurchaseOrderEntity order = PurchaseOrderEntity.builder().company(company).orderNumber("PO-DEMO-0001")
                .orderDate(today.minusDays(7)).vendor(vendor).status(PurchaseOrderStatus.DRAFT)
                .subtotal(total).discountAmount(zero).taxAmount(zero).roundOff(zero).totalAmount(total)
                .notes("Demo draft purchase order").createdBy(userId).updatedBy(userId).build();
        entityManager.persist(order);
        entityManager.persist(PurchaseOrderLineEntity.builder().purchaseOrder(order).lineNo(1).product(product)
                .description("A4 copier paper").quantity(qty).unit("ream").rate(rate).discount(zero)
                .taxRate(zero).taxAmount(zero).amount(total).build());

        PurchaseBillEntity bill = PurchaseBillEntity.builder().company(company).billNumber("BILL-DEMO-0001")
                .billDate(today.minusDays(5)).vendor(vendor).dueDate(today.plusDays(25)).purchaseOrder(order)
                .status(PurchaseBillStatus.DRAFT).subtotal(total).discountAmount(zero).taxAmount(zero)
                .roundOff(zero).totalAmount(total).notes("Demo draft purchase bill").build();
        entityManager.persist(bill);
        entityManager.persist(PurchaseBillLineEntity.builder().purchaseBill(bill).lineNo(1).product(product)
                .description("A4 copier paper").quantity(qty).unit("ream").rate(rate).discount(zero)
                .taxRate(zero).taxAmount(zero).amount(total).build());

        LedgerEntity rent = ledgerRepository.findByCompany_IdAndCodeIgnoreCase(company.getId(), "RENT_EXPENSE")
                .orElseThrow(() -> new IllegalStateException("Default rent ledger was not created"));
        LedgerEntity cash = ledgerRepository.findByCompany_IdAndCodeIgnoreCase(company.getId(), "CASH_IN_HAND")
                .orElseThrow(() -> new IllegalStateException("Default cash ledger was not created"));
        BigDecimal expenseAmount = new BigDecimal("500.00");
        TransactionEntity expense = TransactionEntity.builder().company(company).transactionDate(today.minusDays(3))
                .transactionType(TransactionType.EXPENSE).billNo("EXP-DEMO-0001").vendor(vendor)
                .dueDate(today.plusDays(27)).referenceNo("DEMO-RENT-001").subtotal(expenseAmount)
                .discountAmount(zero).taxAmount(zero).roundOff(zero).totalAmount(expenseAmount)
                .paymentDate(today.minusDays(3)).paymentLedger(cash).paymentStatus(PaymentStatus.PAID)
                .active(true).notes("Demo office rent expense").createdBy(userId).updatedBy(userId).build();
        entityManager.persist(expense);
        entityManager.persist(TransactionLineEntity.builder().transaction(expense).lineNo(1)
                .lineType(TransactionLineType.ACCOUNT).account(rent).description("Office rent")
                .quantity(BigDecimal.ONE).unit("month").rate(expenseAmount).discount(zero).taxRate(zero)
                .taxAmount(zero).amount(expenseAmount).build());

        entityManager.persist(PaymentEntity.builder().company(company).paymentNumber("PAY-DEMO-0001")
                .paymentDate(today.minusDays(2)).vendor(vendor).paymentMethod(PaymentMethod.BANK)
                .sourceLedger(cash).amount(new BigDecimal("100.00")).referenceNumber("DEMO-PAY-001")
                .status(com.qpoos.erp.payment.domain.PaymentStatus.DRAFT).notes("Demo draft supplier payment")
                .createdBy(userId).updatedBy(userId).build());
        entityManager.flush();
    }
}
