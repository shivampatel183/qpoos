# Purchase, Expense, Bill, and Payment Implementation Plan

This document is the implementation contract for separating expenses, purchase orders, purchase bills, and payments into production-ready workflows.

## Implementation Status

Phase 1 foundation and Phase 2 purchase-order workflow are implemented. The purchase-order slice includes company-scoped persistence, item-only lines, server-side totals, automatic numbering, payment terms, delivery address snapshots, list/detail/create/update APIs, draft submission, approval, cancellation, frontend routes, and focused integration tests. Ledger management, voucher posting, receiving, and document-specific bill/payment workflows are intentionally deferred to later phases.

The work is intentionally divided into small phases. Each phase has a narrow scope, a database/API boundary, tests, and an exit criterion. Do not start the next phase until the current phase passes its exit criteria.

## 1. Business Rules

### 1.1 Expense

An expense records a cost charged directly to one or more ledger accounts.

- Expense lines are `ACCOUNT` lines only.
- Item lines are not allowed.
- A vendor is optional unless the business process requires one.
- Direct payment details are required for the initial workflow.
- Direct expense accounting entry:

```text
Dr Expense Account
Cr Cash, Bank, Card, or other payment ledger
```

### 1.2 Purchase Order

A purchase order is an operational commitment to a vendor. It does not create a payable and does not make a payment.

- Vendor is required.
- Item lines are required for inventory purchases.
- Payment status and payment date do not belong on the order.
- Delivery address, delivery term, expected delivery date, and payment term are supported.
- The order has its own lifecycle status.
- No accounting voucher is posted when an order is created.

Recommended statuses:

```text
DRAFT, SENT, APPROVED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED, CLOSED
```

### 1.3 Purchase Bill

A purchase bill records a vendor payable. It may be created independently or from a purchase order.

- Vendor is required.
- Bill number is required and unique per company and financial year.
- Bill date is required.
- Payment term and due date are stored.
- A purchase order reference is optional when the bill is not order-based.
- Payment is not created while editing the bill.
- Payment status is derived from posted payment allocations.
- A bill may contain item lines and, if approved by the accounting rules, account lines.

Typical accounting entry:

```text
Dr Inventory or Expense Account
Dr Input Tax Account
Cr Accounts Payable
```

### 1.4 Payment

A payment is a separate financial document. It settles one or more purchase bills or records a direct expense payment.

- One payment may be allocated to multiple bills.
- One bill may receive multiple payments.
- Partial payments are supported.
- Posted payments are immutable.
- Cancellation requires reversal and an audit trail.
- Payment status on bills is calculated, never trusted from the browser.

See [payment.md](payment.md) for the detailed payment design.

## 2. Current State and Required Direction

The current application uses a shared transaction model and a shared form for all types:

- Frontend: `erp-frontend/src/components/transactions/TransactionFormModal/TransactionFormModal.tsx`
- Frontend service: `erp-frontend/src/services/transactionService.ts`
- Backend entity: `erp-backend/src/main/java/com/qpoos/erp/purchase/transaction/domain/TransactionEntity.java`
- Backend service: `erp-backend/src/main/java/com/qpoos/erp/purchase/transaction/application/TransactionService.java`

The current model is useful as a migration base, but it is too permissive for production:

- The same form allows account and item lines for every document type.
- The same form allows payment status/date for purchase orders and bills.
- The backend accepts client-calculated totals.
- `PAYMENT` is currently a transaction type instead of a separate payment aggregate.
- Frontend links for purchase orders and bills exist, but their routes and workflows are not implemented.

The target architecture keeps reusable document infrastructure but gives each document type its own API contract, validation rules, lifecycle, and UI workflow.

## 3. Target Architecture

### 3.1 Domain boundaries

Use these bounded modules:

```text
common
  money
  financialyear
  number
  term
  address

purchase
  purchase-order
  purchase-bill

expense

payment

accounting
  voucher posting
```

Shared code may contain vendor lookup, tax calculation, money handling, document numbering, line mapping, attachments, and audit metadata. It must not hide document-specific validation.

### 3.2 Persistence model

Use a shared document header and specific detail tables. Do not duplicate the complete entity model three times.

#### Shared purchase document

```text
purchase_documents
- id
- company_id
- financial_year_id
- document_type: PURCHASE_ORDER | PURCHASE_BILL
- document_number
- document_date
- vendor_id
- currency_code
- subtotal
- discount_amount
- tax_amount
- round_off
- total_amount
- notes
- attachments
- status
- created_by
- created_at
- updated_by
- updated_at
```

#### Shared document lines

```text
purchase_document_lines
- id
- company_id
- document_id
- line_no
- line_type: ITEM | ACCOUNT
- item_id nullable
- account_id nullable
- description
- quantity
- unit
- rate
- discount
- tax_rate
- tax_amount
- amount
```

#### Purchase order details

```text
purchase_order_details
- document_id
- delivery_address_id nullable
- delivery_address_snapshot nullable
- delivery_term
- expected_delivery_date
- payment_term_id or payment_term_code
- status
```

Store an address snapshot on submitted orders so later vendor-address edits do not change historical documents.

#### Purchase bill details

```text
purchase_bill_details
- document_id
- bill_number
- bill_date
- due_date
- payment_term_id or payment_term_code
- purchase_order_id nullable
- status
- posted_voucher_id nullable
```

#### Expenses

Expenses should have an explicit aggregate or a clearly isolated document type. The production contract is:

```text
expenses
- id
- company_id
- expense_date
- vendor_id nullable
- total_amount
- payment_id
- posted_voucher_id
- status
```

Expense lines reference accounts only. If the existing transaction table is retained temporarily, enforce the same rules in the service layer and mark the old fields for migration.

#### Payments

```text
payments
- id
- company_id
- financial_year_id
- payment_number
- payment_date
- vendor_id nullable
- payment_method: CASH | BANK | UPI | CHEQUE | CARD
- source_ledger_id
- amount
- reference_number
- status: DRAFT | POSTED | CANCELLED | REVERSED
- voucher_id nullable
- created_by
- created_at
- updated_by
- updated_at

payment_allocations
- id
- company_id
- payment_id
- purchase_bill_id nullable
- expense_id nullable
- allocated_amount
```

An allocation must reference exactly one payable target. Use a check constraint or service validation to prevent both target columns being populated.

### 3.3 Money and totals

- Store money as `BigDecimal` with a consistent database scale.
- Calculate line amounts on the backend.
- Recalculate document subtotal, discounts, tax, round-off, and total on every create/update.
- Compare client totals only for diagnostics; never use them as authoritative values.
- Reject negative values unless a specific credit-note or adjustment workflow supports them.
- Define and test one rounding policy for line tax, document tax, and round-off.

## 4. API Contract

Use document-specific endpoints. Shared list/report endpoints may aggregate results later.

### 4.1 Expense API

```text
POST   /api/expenses
GET    /api/expenses
GET    /api/expenses/{id}
PUT    /api/expenses/{id}             # only while editable
POST   /api/expenses/{id}/cancel
```

The create request contains expense lines and direct payment information. The backend creates the expense and payment/voucher atomically, or returns no success if any part fails.

### 4.2 Purchase order API

```text
POST   /api/purchase-orders
GET    /api/purchase-orders
GET    /api/purchase-orders/{id}
PUT    /api/purchase-orders/{id}
POST   /api/purchase-orders/{id}/submit
POST   /api/purchase-orders/{id}/approve
POST   /api/purchase-orders/{id}/cancel
POST   /api/purchase-orders/{id}/convert-to-bill
```

Only valid status transitions are accepted. For example, a cancelled order cannot be submitted or converted.

### 4.3 Purchase bill API

```text
POST   /api/purchase-bills
GET    /api/purchase-bills
GET    /api/purchase-bills/{id}
PUT    /api/purchase-bills/{id}       # only while draft
POST   /api/purchase-bills/{id}/post
POST   /api/purchase-bills/{id}/cancel
```

Posting a bill creates the payable voucher exactly once. Retrying the request must not create a second voucher.

### 4.4 Payment API

```text
POST   /api/payments
GET    /api/payments
GET    /api/payments/{id}
POST   /api/payments/{id}/post
POST   /api/payments/{id}/cancel
POST   /api/payments/{id}/reverse
```

The payment request includes allocations. Validate company ownership, vendor ownership, outstanding balances, source ledger type, open accounting period, and total allocation before posting.

### 4.5 Response requirements

Every detail response should include:

- `id`
- document number
- document type
- lifecycle status
- vendor summary
- dates and terms
- calculated totals
- outstanding amount where applicable
- audit metadata
- lines
- related document references
- allowed actions, when practical

Returning allowed actions makes the frontend less dependent on duplicated status logic.

## 5. Frontend Architecture

### 5.1 Shared components

Create reusable components for:

```text
components/purchases/
  VendorSelector
  AccountSelector
  ItemSelector
  DocumentHeaderFields
  DocumentLinesEditor
  TotalsSummary
  PaymentTermSelector
  DeliveryFields
  DocumentStatusBadge
  DocumentActionMenu
```

Reuse controls and calculations, not the entire business form.

### 5.2 Feature pages

```text
pages/expenses/
  ExpenseListPage
  ExpenseFormPage
  ExpenseDetailPage

pages/purchase-orders/
  PurchaseOrderListPage
  PurchaseOrderFormPage
  PurchaseOrderDetailPage

pages/purchase-bills/
  PurchaseBillListPage
  PurchaseBillFormPage
  PurchaseBillDetailPage

pages/payments/
  PaymentListPage
  PaymentFormPage
  PaymentDetailPage
```

### 5.3 Form rules

#### Expense form

- Account selector only
- Payment details visible and required
- No quantity/item fields unless a future expense policy explicitly supports them
- Show source ledger and payment method
- Confirm the accounting effect before saving

#### Purchase order form

- Item selector and quantities
- Delivery address
- Delivery term
- Expected delivery date
- Payment term
- No payment status/date/method fields
- Save as draft, then submit/approve through explicit actions

#### Purchase bill form

- Item/account lines according to backend policy
- Vendor and bill number
- Bill date and payment term
- Due date calculated from the term, with permission-based override if required
- Optional purchase-order selection
- No payment entry fields
- Separate `Pay` action after the bill is posted

### 5.4 API services

Replace the single unrestricted `transactionService` with:

```text
expenseService
purchaseOrderService
purchaseBillService
paymentService
```

The shared API client remains the only place that handles HTTP, authentication, error parsing, and abort signals.

## 6. Phased Implementation

### Phase 0: Contract and safety baseline

Goal: establish behavior before changing production data.

Tasks:

- Confirm the business rules in this document with accounting stakeholders.
- Inventory current transaction records by type.
- Identify whether any existing records use `PAYMENT` as a transaction.
- Add backend tests for company scoping and existing transaction behavior.
- Add a feature flag for new purchase workflows.
- Decide the migration strategy for existing `transactions` rows.
- Freeze new fields on the generic form except for bug fixes.

Exit criteria:

- Existing tests pass.
- A record inventory is exported or reproducible.
- No migration can delete existing transaction data.
- The new endpoints can be disabled without affecting the existing application.

### Phase 1: Shared domain primitives

Goal: make shared concepts reliable before adding workflows.

Tasks:

- Add document status enums and transition rules.
- Add payment term representation.
- Add delivery address snapshot representation.
- Add document number generation scoped by company and financial year.
- Add money calculation and rounding utilities.
- Add audit fields and company-scoped repository helpers.
- Add backend tests for totals, rounding, numbering, and status transitions.

Exit criteria:

- Duplicate document numbers are rejected.
- Cross-company references are rejected.
- Invalid status transitions are rejected.
- Calculation tests cover zero, tax, discount, round-off, and decimal quantities.

### Phase 2: Purchase order workflow

Status: implemented. Goal: create a complete non-financial purchase order.

Tasks:

- Add purchase order tables/entities and repositories.
- Implement create, list, detail, update, submit, approve, cancel, and conversion eligibility.
- Enforce vendor, delivery, term, and line rules.
- Add frontend routes and service.
- Build order list, form, detail, and action menu.
- Show status and expected delivery information.
- Do not create payments or vouchers.

Exit criteria:

- A purchase order can be drafted and submitted from the UI.
- Payment fields are absent from the order API and form.
- No payable or accounting voucher is created.
- Cancelled orders cannot be edited or converted.
- Backend integration tests cover every status transition.

### Phase 3: Purchase bill workflow

Goal: create a payable bill without mixing payment creation into bill editing.

Tasks:

- Add purchase bill tables/entities and repositories.
- Add bill number uniqueness per company and financial year.
- Add bill date, payment term, due date, and purchase order reference.
- Implement draft, post, and cancel operations.
- Recalculate totals on the backend.
- Create the payable voucher once during posting.
- Add frontend bill routes, list, form, detail, and post action.
- Add purchase order selection and line-copy behavior.

Exit criteria:

- A bill can be created from an order and independently.
- Posting creates exactly one payable voucher.
- Retrying posting is idempotent.
- Draft bills can be edited; posted bills cannot be edited directly.
- No payment is created by bill creation or posting.

### Phase 4: Payment and allocation workflow

Goal: settle bills and direct expenses with auditable payments.

Tasks:

- Add payment and allocation tables/entities.
- Add payment method and payment status enums.
- Add payment repositories with company-scoped queries.
- Validate allocations against outstanding balances.
- Validate vendor, source ledger, payment method, and open period.
- Post payment voucher atomically.
- Calculate bill outstanding amount and payment status from posted allocations.
- Implement payment cancellation and reversal.
- Add payment list, detail, and allocation UI.

Exit criteria:

- Full, partial, and multi-bill payments work.
- Over-allocation is rejected.
- A posted payment cannot be edited or physically deleted.
- Reversal creates an opposite voucher and preserves the original payment.
- Bill statuses update correctly after posting and reversing payments.

### Phase 5: Direct expense workflow

Goal: make direct expense payment safe and accounting-correct.

Tasks:

- Add expense-specific API and DTOs.
- Enforce account-only lines.
- Require payment method and source ledger.
- Create expense and direct payment in one transaction.
- Post the direct expense voucher.
- Add expense form, list, detail, and cancellation flow.
- Remove payment status/date editing from the generic transaction form.

Exit criteria:

- Item lines are rejected by the backend.
- Missing source ledger or payment method is rejected.
- A failed voucher post rolls back the expense and payment.
- Expense cancellation creates the required accounting reversal.

### Phase 6: Migration and compatibility

Goal: move existing data without losing history.

Tasks:

- Create an explicit migration script or migration service.
- Map old transaction types to new document types.
- Preserve original IDs in a legacy-reference column.
- Mark ambiguous records for manual review instead of guessing.
- Backfill document numbers where missing.
- Reconcile totals against source rows.
- Reconcile old payment summaries against payment allocations.
- Keep read-only compatibility endpoints temporarily if needed.

Exit criteria:

- Source and target record counts reconcile.
- Source and target monetary totals reconcile.
- All ambiguous records are reported.
- A rollback or restore procedure has been tested on a copy of production data.

### Phase 7: Reporting, permissions, and production hardening

Goal: make the workflows operationally complete.

Tasks:

- Add permissions for create, submit, approve, post, pay, cancel, and reverse.
- Add audit events for every lifecycle transition.
- Add purchase, payable, outstanding, and payment reports.
- Add filters by company, vendor, date, status, term, and financial year.
- Add observability for failed posting, duplicate requests, and reconciliation errors.
- Add idempotency keys to posting and payment commands.
- Add rate limits and request-size limits where appropriate.
- Document backup, restore, and period-close behavior.

Exit criteria:

- Unauthorized lifecycle actions are rejected.
- Every posted accounting document has a traceable source document.
- Reports reconcile with ledger balances.
- Operational alerts exist for failed accounting operations.

## 7. Backend Invariants

These rules must be enforced in backend code and, where supported, in the database.

- Every document belongs to exactly one company.
- Every referenced vendor, item, account, order, bill, and ledger belongs to the active company.
- A purchase order never has a payment allocation.
- A bill cannot be paid beyond its outstanding balance.
- A payment cannot allocate more than its amount.
- An allocation references exactly one payable target.
- Posted documents are immutable except through explicit reversal workflows.
- Voucher posting is idempotent.
- Totals are calculated on the server.
- A cancelled document cannot receive a new allocation.
- A payment allocation cannot be moved silently after posting.
- Physical deletion is not allowed for posted financial documents.
- All lifecycle actions are auditable.

## 8. Testing Strategy

### Backend unit tests

- Money and tax calculations
- Payment term due-date calculation
- Status transition rules
- Allocation calculations
- Company-scoped reference validation
- Idempotency behavior

### Backend integration tests

- Create/update each document type
- Invalid line types
- Invalid payment fields
- Purchase order conversion
- Bill posting and duplicate-post prevention
- Full and partial payments
- Multi-bill allocation
- Payment reversal
- Cross-company access rejection
- Transaction rollback when voucher posting fails

### Frontend tests

- Correct fields per document type
- Required-field validation
- Expense account-only lines
- Purchase order has no payment controls
- Purchase bill has no payment controls before posting
- Payment allocation totals
- Status action visibility
- API error rendering
- Loading, retry, and cancellation behavior

### End-to-end scenarios

1. Create an expense and confirm the direct payment voucher.
2. Create, submit, and approve a purchase order.
3. Convert the order to a bill and post it.
4. Make a partial payment and confirm `PARTIALLY_PAID`.
5. Make the remaining payment and confirm `PAID`.
6. Reverse the payment and confirm the outstanding balance is restored.
7. Attempt cross-company access and confirm it is rejected.
8. Retry bill posting and confirm no duplicate voucher exists.

## 9. Deployment and Migration Safety

- Use additive schema changes first.
- Deploy new tables and nullable columns before deploying code that requires them.
- Backfill in batches and record migration progress.
- Do not drop old columns until reconciliation and a retention period are complete.
- Run migrations against a production backup copy first.
- Enable new workflows behind a feature flag.
- Release backend contracts before enabling frontend actions.
- Monitor error rates, posting failures, and reconciliation totals after each release.
- Keep a documented rollback procedure for each phase.

## 10. Definition of Production Ready

The implementation is production ready only when:

- Expense, purchase order, purchase bill, and payment have separate contracts.
- Document-specific validation exists on the backend.
- Frontend forms expose only valid fields for the selected document.
- Server-side totals and accounting entries are authoritative.
- Payment status comes from posted allocations.
- Posted financial documents are immutable and reversible.
- Company and permission boundaries are tested.
- Idempotency prevents duplicate vouchers and payments.
- Migration reconciliation has passed.
- Audit, reporting, monitoring, backup, and rollback procedures are documented.

## 11. Immediate Next Step

Start with Phase 0 and Phase 1 only. Do not build the new purchase-order or bill screens against the current unrestricted `transactionService`. First lock the contracts, status model, calculations, numbering, and migration strategy. Then implement the purchase order workflow as the first document-specific vertical slice.
