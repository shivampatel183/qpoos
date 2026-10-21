package com.qpoos.erp.purchase.shared.application;

import com.qpoos.erp.purchase.shared.domain.ExpenseStatus;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;

public final class DocumentStatusRules {

    private DocumentStatusRules() {
    }

    public static void requirePurchaseOrderTransition(
            PurchaseOrderStatus current,
            PurchaseOrderStatus next
    ) {
        boolean allowed = switch (current) {
            case DRAFT -> next == PurchaseOrderStatus.SENT
                    || next == PurchaseOrderStatus.CANCELLED;
            case SENT -> next == PurchaseOrderStatus.APPROVED
                    || next == PurchaseOrderStatus.CANCELLED;
            case APPROVED -> next == PurchaseOrderStatus.PARTIALLY_RECEIVED
                    || next == PurchaseOrderStatus.RECEIVED
                    || next == PurchaseOrderStatus.CANCELLED;
            case PARTIALLY_RECEIVED -> next == PurchaseOrderStatus.RECEIVED
                    || next == PurchaseOrderStatus.CLOSED;
            case RECEIVED -> next == PurchaseOrderStatus.CLOSED;
            case CANCELLED, CLOSED -> false;
        };
        requireAllowed(allowed, current.name(), next.name());
    }

    public static void requirePurchaseBillTransition(
            PurchaseBillStatus current,
            PurchaseBillStatus next
    ) {
        boolean allowed = current == PurchaseBillStatus.DRAFT
                && (next == PurchaseBillStatus.POSTED || next == PurchaseBillStatus.CANCELLED);
        requireAllowed(allowed, current.name(), next.name());
    }

    public static void requireExpenseTransition(ExpenseStatus current, ExpenseStatus next) {
        boolean allowed = current == ExpenseStatus.DRAFT
                && (next == ExpenseStatus.PAID || next == ExpenseStatus.CANCELLED);
        requireAllowed(allowed, current.name(), next.name());
    }

    private static void requireAllowed(boolean allowed, String current, String next) {
        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid document status transition: " + current + " -> " + next
            );
        }
    }
}