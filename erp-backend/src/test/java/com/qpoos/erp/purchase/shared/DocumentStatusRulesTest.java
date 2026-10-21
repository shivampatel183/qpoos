package com.qpoos.erp.purchase.shared;

import com.qpoos.erp.purchase.shared.application.DocumentStatusRules;
import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;
import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class DocumentStatusRulesTest {

    @Test
    void allowsPurchaseOrderLifecycleTransitions() {
        assertThatNoException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseOrderTransition(
                        PurchaseOrderStatus.DRAFT,
                        PurchaseOrderStatus.SENT
                )
        );
        assertThatNoException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseOrderTransition(
                        PurchaseOrderStatus.APPROVED,
                        PurchaseOrderStatus.PARTIALLY_RECEIVED
                )
        );
    }

    @Test
    void rejectsEditingClosedOrCancelledPurchaseOrders() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseOrderTransition(
                        PurchaseOrderStatus.CLOSED,
                        PurchaseOrderStatus.DRAFT
                )
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseOrderTransition(
                        PurchaseOrderStatus.CANCELLED,
                        PurchaseOrderStatus.APPROVED
                )
        );
    }

    @Test
    void onlyDraftPurchaseBillsCanBePostedOrCancelled() {
        assertThatNoException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseBillTransition(
                        PurchaseBillStatus.DRAFT,
                        PurchaseBillStatus.POSTED
                )
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
                DocumentStatusRules.requirePurchaseBillTransition(
                        PurchaseBillStatus.POSTED,
                        PurchaseBillStatus.DRAFT
                )
        );
    }
}