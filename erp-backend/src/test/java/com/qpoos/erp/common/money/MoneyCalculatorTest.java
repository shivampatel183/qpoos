package com.qpoos.erp.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MoneyCalculatorTest {

    @Test
    void calculatesLineAndDocumentTotalsWithHalfUpRounding() {
        MoneyCalculator.LineAmounts first = MoneyCalculator.calculateLine(
                new BigDecimal("2"),
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("18")
        );
        MoneyCalculator.LineAmounts second = MoneyCalculator.calculateLine(
                new BigDecimal("1"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("5")
        );

        MoneyCalculator.DocumentTotals totals = MoneyCalculator.calculateDocument(
                List.of(first, second),
                new BigDecimal("0.01")
        );

        assertThat(first.gross()).isEqualByComparingTo("200.00");
        assertThat(first.discount()).isEqualByComparingTo("10.00");
        assertThat(first.taxAmount()).isEqualByComparingTo("34.20");
        assertThat(first.amount()).isEqualByComparingTo("224.20");
        assertThat(totals.subtotal()).isEqualByComparingTo("250.00");
        assertThat(totals.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(totals.taxAmount()).isEqualByComparingTo("36.70");
        assertThat(totals.totalAmount()).isEqualByComparingTo("276.71");
    }

    @Test
    void capsDiscountAtGrossAmount() {
        MoneyCalculator.LineAmounts amounts = MoneyCalculator.calculateLine(
                BigDecimal.ONE,
                new BigDecimal("10"),
                new BigDecimal("15"),
                new BigDecimal("18")
        );

        assertThat(amounts.discount()).isEqualByComparingTo("10.00");
        assertThat(amounts.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(amounts.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNegativeMoneyInputs() {
        assertThatIllegalArgumentException().isThrownBy(() -> MoneyCalculator.calculateLine(
                BigDecimal.ONE,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
    }
}