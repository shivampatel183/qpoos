package com.qpoos.erp.accounting.journal.application;

import com.qpoos.erp.accounting.chartofaccount.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.chartofaccount.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.journal.domain.JournalEntryEntity;
import com.qpoos.erp.accounting.journal.domain.JournalEntryKind;
import com.qpoos.erp.accounting.journal.domain.JournalLineEntity;
import com.qpoos.erp.accounting.journal.domain.JournalSourceType;
import com.qpoos.erp.accounting.journal.domain.JournalStatus;
import com.qpoos.erp.accounting.journal.infrastructure.JournalEntryRepository;
import com.qpoos.erp.common.money.MoneyCalculator;
import com.qpoos.erp.common.number.DocumentNumberService;
import com.qpoos.erp.common.number.DocumentNumberType;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import com.qpoos.erp.purchase.bill.domain.PurchaseBillLineEntity;
import com.qpoos.erp.purchase.bill.infrastructure.PurchaseBillLineRepository;
import com.qpoos.erp.purchase.bill.infrastructure.PurchaseBillRepository;
import com.qpoos.erp.purchase.expense.domain.PaymentStatus;
import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionLineEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionLineType;
import com.qpoos.erp.purchase.expense.domain.TransactionType;
import com.qpoos.erp.purchase.expense.infrastructure.TransactionLineRepository;
import com.qpoos.erp.purchase.expense.infrastructure.TransactionRepository;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
import com.qpoos.erp.inventory.product.domain.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountingPostingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PurchaseBillRepository purchaseBillRepository;
    private final PurchaseBillLineRepository purchaseBillLineRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLineRepository transactionLineRepository;
    private final LedgerRepository ledgerRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalValidator journalValidator;
    private final DocumentNumberService documentNumberService;
    @Transactional
    public JournalEntryEntity postPurchaseBill(Long id) {
        UUID companyId = SecurityUtils.getCompanyId();
        PurchaseBillEntity bill = purchaseBillRepository.findForPosting(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase bill not found"));

        if (bill.getJournalEntry() != null) {
            return bill.getJournalEntry();
        }
        if (bill.getStatus() == PurchaseBillStatus.POSTED) {
            JournalEntryEntity prior = findOriginal(companyId, JournalSourceType.PURCHASE_BILL, bill.getId());
            if (prior == null) {
                throw new IllegalArgumentException(
                        "This purchase bill was posted before journal accounting was enabled; reconcile it before reposting"
                );
            }
            bill.setJournalEntry(prior);
            bill.setPostedVoucherId(prior.getId());
            purchaseBillRepository.save(bill);
            return prior;
        }
        if (bill.getStatus() != PurchaseBillStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase bills can be posted");
        }
        List<PurchaseBillLineEntity> sourceLines =
                purchaseBillLineRepository.findAllByPurchaseBill_IdOrderByLineNoAsc(bill.getId());
        if (bill.getBillDate() == null || sourceLines.isEmpty()) {
            throw new IllegalArgumentException("Purchase bill date and lines are required for posting");
        }

        List<JournalLineEntity> lines = new ArrayList<>();
        BigDecimal taxTotal = ZERO;
        int lineNo = 1;
        for (PurchaseBillLineEntity sourceLine : sourceLines) {
            MoneyCalculator.LineAmounts amounts = calculateAndValidatePurchaseLine(sourceLine);
            LedgerEntity debitAccount = sourceLine.getProduct().getType() == ProductType.INVENTORY
                    ? requireLedger(companyId, "INVENTORY_ASSET")
                    : requireLedger(companyId, "PURCHASE_ACCOUNT");
            BigDecimal baseAmount = MoneyCalculator.money(amounts.gross().subtract(amounts.discount()));
            addDebit(lines, debitAccount, baseAmount, sourceLine.getDescription(), lineNo++);
            taxTotal = taxTotal.add(amounts.taxAmount());
        }

        validatePurchaseTotals(bill);
        if (taxTotal.signum() > 0) {
            addDebit(
                    lines,
                    requireLedger(companyId, "GST_INPUT_CREDIT"),
                    MoneyCalculator.money(taxTotal),
                    "Recoverable input GST",
                    lineNo++
            );
        }
        addRoundOff(lines, companyId, bill.getRoundOff(), lineNo);
        addCredit(
                lines,
                requireLedger(companyId, "SUPPLIER_PAYABLE"),
                MoneyCalculator.money(bill.getTotalAmount()),
                "Supplier payable",
                nextLineNumber(lines)
        );

        JournalEntryEntity journal = createAndValidate(
                companyId,
                bill.getCompany(),
                JournalSourceType.PURCHASE_BILL,
                bill.getId(),
                bill.getBillDate(),
                "Purchase bill " + bill.getBillNumber() + " - " + bill.getVendor().getDisplayName(),
                lines
        );
        bill.setJournalEntry(journal);
        bill.setPostedVoucherId(journal.getId());
        bill.setStatus(PurchaseBillStatus.POSTED);
        bill.setUpdatedBy(SecurityUtils.getUserId());
        purchaseBillRepository.save(bill);
        return journal;
    }

    @Transactional
    public JournalEntryEntity postExpense(Long id) {
        UUID companyId = SecurityUtils.getCompanyId();
        TransactionEntity transaction = transactionRepository.findForPosting(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getJournalEntry() != null) {
            return transaction.getJournalEntry();
        }
        if (transaction.getTransactionType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Only expense transactions can be posted by this workflow");
        }
        if (transaction.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled expenses cannot be posted");
        }
        if (transaction.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID) {
            throw new IllegalArgumentException(
                    "Partially paid expenses cannot be posted until the paid and outstanding amounts are recorded separately"
            );
        }
        if (transaction.getTransactionDate() == null) {
            throw new IllegalArgumentException("Expense date is required for posting");
        }
        if (transaction.getPaymentDate() == null) {
            throw new IllegalArgumentException("Expense payment date is required for posting");
        }
        if (transaction.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Expense must be paid before it can be posted");
        }

        LedgerEntity paymentLedger = transaction.getPaymentLedger();
        if (paymentLedger == null
                || !companyId.equals(paymentLedger.getCompany().getId())
                || !Boolean.TRUE.equals(paymentLedger.getActive())
                || !Boolean.TRUE.equals(paymentLedger.getPostingAllowed())
                || (paymentLedger.getLedgerType() != com.qpoos.erp.accounting.chartofaccount.domain.LedgerType.CASH
                    && paymentLedger.getLedgerType() != com.qpoos.erp.accounting.chartofaccount.domain.LedgerType.BANK)) {
            throw new IllegalArgumentException("Expense must use an active, postable company cash or bank account");
        }

        List<JournalLineEntity> lines = new ArrayList<>();
        List<MoneyCalculator.LineAmounts> sourceAmounts = new ArrayList<>();
        int lineNo = 1;
        for (TransactionLineEntity sourceLine :
                transactionLineRepository.findAllByTransaction_IdAndTransaction_Company_IdOrderByLineNoAsc(
                        transaction.getId(),
                        companyId
                )) {
            MoneyCalculator.LineAmounts amounts = calculateAndValidateExpenseLine(sourceLine);
            sourceAmounts.add(amounts);

            LedgerEntity debitAccount;
            if (sourceLine.getLineType() == TransactionLineType.ACCOUNT) {
                debitAccount = sourceLine.getAccount();
            } else if (sourceLine.getItem() != null
                    && sourceLine.getItem().getType() == ProductType.INVENTORY) {
                debitAccount = requireLedger(companyId, "INVENTORY_ASSET");
            } else {
                debitAccount = requireLedger(companyId, "PURCHASE_ACCOUNT");
            }

            BigDecimal baseAmount = MoneyCalculator.money(amounts.gross().subtract(amounts.discount()));
            addDebit(lines, debitAccount, baseAmount, sourceLine.getDescription(), lineNo++);
        }
        validateExpenseTotals(transaction, sourceAmounts);

        BigDecimal taxTotal = sourceAmounts.stream()
                .map(MoneyCalculator.LineAmounts::taxAmount)
                .reduce(ZERO, BigDecimal::add);
        if (taxTotal.signum() > 0) {
            addDebit(
                    lines,
                    requireLedger(companyId, "GST_INPUT_CREDIT"),
                    MoneyCalculator.money(taxTotal),
                    "Recoverable input GST",
                    lineNo++
            );
        }
        addRoundOff(lines, companyId, transaction.getRoundOff(), lineNo);

        addCredit(
                lines,
                paymentLedger,
                MoneyCalculator.money(transaction.getTotalAmount()),
                "Paid from " + paymentLedger.getName(),
                nextLineNumber(lines)
        );

        String narration = transaction.getReferenceNo() == null || transaction.getReferenceNo().isBlank()
                ? "Expense transaction " + transaction.getId()
                : transaction.getReferenceNo();
        JournalEntryEntity journal = createAndValidate(
                companyId,
                transaction.getCompany(),
                JournalSourceType.EXPENSE,
                transaction.getId(),
                transaction.getTransactionDate(),
                narration,
                lines
        );
        transaction.setJournalEntry(journal);
        transaction.setUpdatedBy(SecurityUtils.getUserId());
        transactionRepository.save(transaction);
        return journal;
    }

    private MoneyCalculator.LineAmounts calculateAndValidatePurchaseLine(PurchaseBillLineEntity line) {
        MoneyCalculator.LineAmounts amounts = MoneyCalculator.calculateLine(
                line.getQuantity(), line.getRate(), line.getDiscount(), line.getTaxRate()
        );
        requireSameMoney("Purchase bill line amount", line.getAmount(), amounts.amount());
        requireSameMoney("Purchase bill line tax", line.getTaxAmount(), amounts.taxAmount());
        return amounts;
    }

    private MoneyCalculator.LineAmounts calculateAndValidateExpenseLine(TransactionLineEntity line) {
        MoneyCalculator.LineAmounts amounts = MoneyCalculator.calculateLine(
                line.getQuantity(), line.getRate(), line.getDiscount(), line.getTaxRate()
        );
        requireSameMoney("Expense line amount", line.getAmount(), amounts.amount());
        requireSameMoney("Expense line tax", line.getTaxAmount(), amounts.taxAmount());
        return amounts;
    }

    private void validatePurchaseTotals(PurchaseBillEntity bill) {
        List<MoneyCalculator.LineAmounts> amounts = purchaseBillLineRepository
                .findAllByPurchaseBill_IdOrderByLineNoAsc(bill.getId()).stream()
                .map(this::calculateAndValidatePurchaseLine)
                .toList();
        MoneyCalculator.DocumentTotals totals = MoneyCalculator.calculateDocument(amounts, bill.getRoundOff());
        requireSameMoney("Purchase bill subtotal", bill.getSubtotal(), totals.subtotal());
        requireSameMoney("Purchase bill discount", bill.getDiscountAmount(), totals.discountAmount());
        requireSameMoney("Purchase bill tax", bill.getTaxAmount(), totals.taxAmount());
        requireSameMoney("Purchase bill total", bill.getTotalAmount(), totals.totalAmount());
    }

    private void validateExpenseTotals(
            TransactionEntity transaction,
            List<MoneyCalculator.LineAmounts> amounts
    ) {
        MoneyCalculator.DocumentTotals totals = MoneyCalculator.calculateDocument(amounts, transaction.getRoundOff());
        requireSameMoney("Expense subtotal", transaction.getSubtotal(), totals.subtotal());
        requireSameMoney("Expense discount", transaction.getDiscountAmount(), totals.discountAmount());
        requireSameMoney("Expense tax", transaction.getTaxAmount(), totals.taxAmount());
        requireSameMoney("Expense total", transaction.getTotalAmount(), totals.totalAmount());
    }

    private void requireSameMoney(String field, BigDecimal actual, BigDecimal expected) {
        if (actual == null || MoneyCalculator.money(actual).compareTo(MoneyCalculator.money(expected)) != 0) {
            throw new IllegalArgumentException(field + " does not match its calculated source lines");
        }
    }

    private JournalEntryEntity createAndValidate(
            UUID companyId,
            com.qpoos.erp.company.domain.CompanyEntity company,
            JournalSourceType sourceType,
            Long sourceId,
            java.time.LocalDate accountingDate,
            String narration,
            List<JournalLineEntity> lines
    ) {
        journalValidator.validate(companyId, lines);
        JournalEntryEntity entry = JournalEntryEntity.builder()
                .company(company)
                .journalNumber(documentNumberService.next(DocumentNumberType.JOURNAL, accountingDate))
                .accountingDate(accountingDate)
                .sourceType(sourceType)
                .sourceDocumentId(sourceId)
                .entryKind(JournalEntryKind.ORIGINAL)
                .narration(narration)
                .status(JournalStatus.POSTED)
                .createdBy(SecurityUtils.getUserId())
                .postedBy(SecurityUtils.getUserId())
                .build();
        lines.forEach(entry::addLine);
        return journalEntryRepository.saveAndFlush(entry);
    }

    private void addRoundOff(
            List<JournalLineEntity> lines,
            UUID companyId,
            BigDecimal roundOff,
            int firstLineNo
    ) {
        if (roundOff == null || roundOff.signum() == 0) {
            return;
        }
        BigDecimal amount = MoneyCalculator.money(roundOff.abs());
        if (roundOff.signum() > 0) {
            addDebit(lines, requireLedger(companyId, "ROUND_OFF_LOSS"), amount, "Document round off", firstLineNo);
        } else {
            addCredit(lines, requireLedger(companyId, "ROUND_OFF_GAIN"), amount, "Document round off", firstLineNo);
        }
    }

    private void addDebit(
            List<JournalLineEntity> lines,
            LedgerEntity ledger,
            BigDecimal amount,
            String description,
            int lineNo
    ) {
        if (amount.signum() <= 0) {
            return;
        }
        lines.add(JournalLineEntity.builder()
                .ledger(ledger)
                .lineNo(lineNo)
                .debitAmount(MoneyCalculator.money(amount))
                .creditAmount(MoneyCalculator.money(ZERO))
                .description(description)
                .build());
    }

    private void addCredit(
            List<JournalLineEntity> lines,
            LedgerEntity ledger,
            BigDecimal amount,
            String description,
            int lineNo
    ) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A posted source document must have a positive total amount");
        }
        lines.add(JournalLineEntity.builder()
                .ledger(ledger)
                .lineNo(lineNo)
                .debitAmount(MoneyCalculator.money(ZERO))
                .creditAmount(MoneyCalculator.money(amount))
                .description(description)
                .build());
    }

    private int nextLineNumber(List<JournalLineEntity> lines) {
        return lines.stream()
                .map(JournalLineEntity::getLineNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private LedgerEntity requireLedger(UUID companyId, String code) {
        LedgerEntity ledger = ledgerRepository.findByCompany_IdAndCodeIgnoreCase(companyId, code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Required accounting mapping is missing: ledger " + code
                ));
        if (!Boolean.TRUE.equals(ledger.getActive()) || !Boolean.TRUE.equals(ledger.getPostingAllowed())) {
            throw new IllegalArgumentException("Required accounting ledger is inactive or non-postable: " + code);
        }
        return ledger;
    }

    private JournalEntryEntity findOriginal(UUID companyId, JournalSourceType sourceType, Long sourceId) {
        return journalEntryRepository
                .findByCompany_IdAndSourceTypeAndSourceDocumentIdAndEntryKind(
                        companyId,
                        sourceType,
                        sourceId,
                        JournalEntryKind.ORIGINAL
                )
                .orElse(null);
    }
}
