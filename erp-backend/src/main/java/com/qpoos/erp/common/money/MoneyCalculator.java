package com.qpoos.erp.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class MoneyCalculator {

    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 3;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyCalculator() {
    }

    public static LineAmounts calculateLine(
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal discount,
            BigDecimal taxRate
    ) {
        BigDecimal safeQuantity = nonNegative(quantity, "quantity");
        BigDecimal safeRate = nonNegative(rate, "rate");
        BigDecimal safeDiscount = nonNegative(discount, "discount");
        BigDecimal safeTaxRate = nonNegative(taxRate, "tax rate");

        BigDecimal gross = money(safeQuantity.multiply(safeRate));
        BigDecimal appliedDiscount = money(safeDiscount.min(gross));
        BigDecimal taxableAmount = gross.subtract(appliedDiscount);
        BigDecimal taxAmount = money(taxableAmount.multiply(safeTaxRate)
                .divide(BigDecimal.valueOf(100), 6, ROUNDING_MODE));

        return new LineAmounts(
                gross,
                appliedDiscount,
                taxAmount,
                money(taxableAmount.add(taxAmount))
        );
    }

    public static DocumentTotals calculateDocument(
            List<LineAmounts> lines,
            BigDecimal roundOff
    ) {
        BigDecimal subtotal = lines.stream()
                .map(LineAmounts::gross)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = lines.stream()
                .map(LineAmounts::discount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = lines.stream()
                .map(LineAmounts::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal safeRoundOff = roundOff == null
                ? BigDecimal.ZERO
                : roundOff.setScale(MONEY_SCALE, ROUNDING_MODE);

        return new DocumentTotals(
                money(subtotal),
                money(discount),
                money(tax),
                safeRoundOff,
                money(subtotal.subtract(discount).add(tax).add(safeRoundOff))
        );
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return normalized;
    }

    public record LineAmounts(
            BigDecimal gross,
            BigDecimal discount,
            BigDecimal taxAmount,
            BigDecimal amount
    ) {
    }

    public record DocumentTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal roundOff,
            BigDecimal totalAmount
    ) {
    }
}