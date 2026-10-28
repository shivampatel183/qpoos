# Centralized Payment and Settlement Engine — Implementation Plan

## Objective

Build one reusable payment engine for the ERP. It must record money movement once, allocate that movement to one or more financial documents, support partial settlement over time, and integrate with accounting. The same engine must handle today's purchase bills and payable expenses, and later customer invoices, sales bills, credit notes, refunds, and other settlement documents.

This design intentionally treats the existing database as demo data. It does not require preserving or migrating current rows. The implementation may replace the payment scaffold and reset demo data as needed. The module must preserve architectural compatibility through stable document contracts, not through legacy data migration.

## Implementation Status

The first vertical slice is implemented: outgoing supplier payments can allocate to posted purchase bills and payable expenses; one document can receive multiple partial payments, and one payment can cover multiple documents. Draft create/edit/void, journal posting, reversal, a paginated payment list, bill settlement balances, and expense "pay later" are wired into the UI and backend.

The current release handles outgoing vendor payments in the company's existing base-currency behavior. Incoming customer receipts are not yet enabled because sales invoices do not exist. Future invoice/sales modules will need to provide settlement-target validation and receivable control-account mappings before the shared API accepts those document types. Explicit idempotency-key storage, period-close integration, and versioned production schema migrations remain follow-up hardening work.

Core cardinality:

```text
One financial document <- many payment allocations <- many payments
One payment -> many payment allocations -> many financial documents
```

A bill can receive several partial payments over time. A single payment can settle several bills. Both workflows use the same payment and allocation services and the same UI.

## Repository Baseline

- `PaymentEntity` and `PaymentAllocationEntity` plus repositories exist, but no payment API/service/UI workflow exists.
- Allocation currently points only to the generic `TransactionEntity`; purchase bills are separate `PurchaseBillEntity` records.
- Purchase bills post a payable journal through `AccountingPostingService`.
- Expenses are currently recorded as paid immediately and post Dr Expense / Cr Cash or Bank. That workflow must not be charged again. To make an expense eligible for later payment, add a payable mode that posts Dr Expense / Cr Accounts Payable first.
- Journal source types currently include purchase bill and expense. Payment/receipt source support must be added.
- Payment document numbering is already represented by `DocumentNumberType.PAYMENT`.
- Current schema setup uses Hibernate update. For production, adopt a versioned schema migration tool and use it for the new payment model. Since current data is demo data, an initial reset/rebuild is acceptable.

## Central Design: Settlement Documents

### Shared settlement contract

Each module that can be paid or collected against must expose a small adapter to the settlement engine. The engine should not depend on every module's entity internals.

```java
interface SettlementDocumentAdapter {
    SettlementDocument loadForSettlement(UUID companyId, Long documentId, LockMode lockMode);
    SettlementSnapshot getSettlementSnapshot(SettlementDocument document);
    List<JournalLineSpec> buildSettlementLines(SettlementDocument document, BigDecimal amount);
}
```

The exact Java shape may use a registry and typed adapters rather than this literal interface. Contracts should cover:

- Stable document type and ID.
- Company, counterparty, currency, document date, due date, lifecycle status.
- Original amount, settled amount, outstanding amount, and settlement state.
- Whether the document is currently settleable and the control ledger for settlement.
- Accounting line specification for applying money to the document.

Adapters are implemented by purchase bill, payable expense, customer invoice, sales bill, and future credit/refund modules. Modules own their document lifecycle and initial recognition journal; the settlement engine owns settlement events, allocations, payment/receipt journals, and derived settlement totals.

### Shared direction and document type

Use one payment aggregate with an explicit direction:

- `OUTGOING`: money leaves a company (supplier payment, expense payment, customer refund).
- `INCOMING`: money enters a company (customer receipt, sales collection, supplier refund received).

Use a registered `SettlementDocumentType` (initial values `PURCHASE_BILL`, `PAYABLE_EXPENSE`; future `SALES_INVOICE`, `SALES_BILL`, `CREDIT_NOTE`, `REFUND`, etc.). Each type declares expected direction and counterparty role. Do not hard-code a separate payment service for every module.

### Minimal persistence model

#### `payments`

One row per receipt/disbursement. Keep only header/lifecycle/accounting facts here:

```text
id, company_id, financial_year_id, payment_number,
direction, payment_date, currency_code, amount,
payment_method, source_ledger_id, counterparty_type, counterparty_id,
reference_number, notes, status,
original_journal_id, reversal_journal_id,
created_by, created_at, updated_by, updated_at, version
```

`counterparty_type/id` is useful for filtering and display but is not an accounting target. Validate it against all allocations. If the application already has a unified party table, use its FK instead. Amount/currency precision and supported methods follow an explicit company currency policy.

Statuses: `DRAFT`, `POSTED`, `REVERSED`; unposted drafts may be `VOIDED`. Do not mutate posted details.

#### `payment_allocations`

One row per payment-to-document application:

```text
id, company_id, payment_id,
document_type, document_id,
allocated_amount, created_at
```

This compact registry avoids adding a new nullable FK column every time a module becomes payable. Because relational FKs cannot validate a `(type,id)` pair, every adapter lookup must verify company/type, and the payment post transaction must lock and validate each target. Add `UNIQUE(payment_id, document_type, document_id)`, positive amount check, and indexes on `(company_id, document_type, document_id)` and `(payment_id)`.

If database-enforced referential integrity across target tables is a firm requirement, introduce a minimal shared `settlement_documents` supertype table (`id, company_id, document_type, source_document_id, currency_code`) with one-to-one module rows. This is stricter but adds joins and lifecycle synchronization. Choose one approach before coding; do not implement both.

#### Avoid balance tables as authority

The source of truth is posted allocations plus their payment lifecycle. Do not store mutable paid/outstanding amounts as independent authority. For performance, a rebuildable per-document settlement summary/projection may be added later, updated in the same transaction or via outbox, and periodically reconciled against allocations.

#### Reversal/audit

Keep payment and allocation rows immutable after posting. Store reversal journal link and reason on payment or in a small `payment_reversals` table if multiple correction events are ever needed. Maintain a generic audit event record for create/update/post/void/reverse and actor/time/reason. No physical delete of posted financial data.

## Behavior and Accounting

### Create draft and allocate

- User selects direction, counterparty, date, method, source ledger, amount, and one or more open documents.
- The same document may receive allocations from many different payments.
- A payment may allocate across several documents, provided direction, company, counterparty, and currency are compatible.
- Draft allocations do not change document balances and do not create journal entries.
- Draft can be edited/voided. Posting re-reads and revalidates balances because another payment may have been posted since draft creation.
- Default first-release rule: payment amount must equal allocation total at posting. If overpayments/advances are required, add an explicit unapplied balance/advance target with its own ledger treatment. Never silently leave an amount unallocated.

### Settlement state

For document amount `T`:

```text
settled = sum(allocations from POSTED payments) - allocations from REVERSED payments
outstanding = T - settled
UNPAID if settled = 0; PARTIALLY_PAID if 0 < settled < T; PAID if settled = T
```

The backend computes amounts; frontend values are display hints only. Due/overdue is separate from lifecycle/settlement state. Credit notes and refunds need adapter-defined sign/direction rules and must not be represented as negative ordinary payments.

### Accounting entries

Document recognition remains owned by the source module. Settlement postings transfer the recognized payable/receivable against cash/bank/clearing:

Outgoing payment against supplier bill:

```text
Dr Accounts Payable
Cr Cash / Bank / Clearing
```

Incoming receipt against customer invoice:

```text
Dr Cash / Bank / Clearing
Cr Accounts Receivable
```

Expense payable settlement uses Accounts Payable (or the configured payable control), not expense again. The current direct-paid expense remains its own immediate payment entry and is not eligible for a second settlement. A later payable expense mode posts Dr Expense / Cr Accounts Payable and then uses the shared engine.

For a multi-document payment, aggregate debits/credits by control ledger and create one balanced journal per payment. Persist allocation detail as the document-level audit trail; journal lines need not duplicate every allocation if the accounting/reporting contract is satisfied by the allocation relation and journal metadata.

Posting and reversal are atomic with journal creation, payment status, and allocation effect. Extend the shared accounting posting layer with `PAYMENT` source/entry kind and original/reversal linkage. Payment code must call the posting abstraction; it must not create journal entries independently. Retries return the prior result and never duplicate the cash movement.

## Backend API

Keep one API regardless of source module:

```text
POST   /api/payments                         # create draft
GET    /api/payments                         # paginated/filterable list
GET    /api/payments/{id}                    # detail + allocations + accounting refs
PUT    /api/payments/{id}                    # draft only
POST   /api/payments/{id}/post               # post atomically; idempotent
POST   /api/payments/{id}/void               # void draft only
POST   /api/payments/{id}/reverse            # reverse posted payment with reason
GET    /api/settlement-documents              # eligible documents by direction/type/party/date
GET    /api/settlement-documents/{type}/{id}  # document balance and settlement history
```

Draft request includes direction, party reference, date, currency, amount, method, source ledger, reference/notes, and allocations (`documentType`, `documentId`, `amount`). It excludes company ID, calculated balances, payment status, journal IDs, and server-calculated totals.

Use `Idempotency-Key` for post and reverse commands, stored by company/operation/key with request hash and result. Reusing a key with a different request returns conflict. Keep a unique journal source event as a second defense.

Use stable error codes for wrong direction, cross-company target, wrong party/currency, target not posted/open, stale balance, over-allocation, invalid source ledger, invalid transition, date unavailable, and idempotency conflict.

## UI Design: Both Settlement Directions

The UI is one shared payment experience, entered from either the Payments area or a source document.

### Navigation and pages

```text
Payments
  Payment/Receipt list with direction, status, date, party, amount, method, reference
  Create Payment / Receive Money action
  Payment detail with allocations, journal link, audit history, reverse action

Purchase Bill detail -> Make Payment
Expense payable detail -> Make Payment
Customer Invoice detail -> Receive Payment
Sales Bill detail -> Receive Payment
```

Use clear labels based on direction: “Pay”/“Payment” for outgoing and “Receive”/“Receipt” for incoming. They are the same entity/API underneath.

### Create payment/receipt form

1. Choose direction (or infer and lock it when launched from a document).
2. Choose counterparty (supplier/customer/other party) and show only open compatible documents.
3. Enter date, payment method, source cash/bank/clearing ledger, reference, and total amount.
4. Allocation grid supports adding/removing multiple documents and entering amount per row. Columns: document number/type, date/due date, original total, already settled, outstanding, amount to apply, remaining after draft.
5. Show amount, allocated total, and unallocated remainder continuously. Block save/post when totals violate policy.
6. Save draft or Post. Posting confirmation summarizes total cash movement and document applications.

When launched from a bill/invoice, preselect that document and its current outstanding. Allow the user to add other documents for the same party, making one payment cover several bills. The same bill can be revisited for later partial payments because prior allocations reduce its outstanding value.

### Detail and list UX

- Detail shows immutable payment header, each document allocation, current document status, journal link, created/posted/reversed actors and timestamps, and reverse action when authorized.
- List has server-side pagination, filters for direction/date/party/method/status/document number, and stable sort.
- Source bill/invoice detail shows a settlement panel: original amount, settled, outstanding, settlement state, payment history, and `Pay`/`Receive` action.
- On a stale-balance conflict, show refreshed balances and retain safe draft input so the user can adjust allocations.
- Posted payment fields are read-only. Reverse requires reason and a confirmation summary; it never deletes history.
- Accessible keyboard-operable allocation rows, clear currency formatting, and responsive layout for narrower screens.

### Frontend service/component boundaries

```text
services/paymentService.ts
pages/payments/PaymentListPage
pages/payments/PaymentFormPage
pages/payments/PaymentDetailPage
components/payments/PaymentHeaderForm
components/payments/SettlementDocumentPicker
components/payments/AllocationGrid
components/payments/AllocationTotals
components/payments/SettlementHistory
```

Source modules provide `MakePayment`/`ReceivePayment` entry points and settlement panel; they do not implement their own allocation or posting logic. Shared components reuse selectors/formatting, but backend remains authoritative for eligibility and balances.

## Production Invariants and Optimization

1. Company ID is always derived from the authenticated principal; every target/party/ledger read is company-scoped in SQL.
2. A posted target must belong to same company, direction, counterparty, and currency as the payment.
3. Payment amount and allocations are positive; line rounding/currency scale policy is consistent and tested.
4. Payment allocation sum equals payment amount at post unless an explicit advance/unapplied feature is enabled.
5. Each allocation is unique per payment/document; repeated partial payments are distinct payments, not duplicate lines.
6. Lock payment and all target documents in deterministic `(type,id)` order; re-read posted allocation sums while holding locks before posting, preventing concurrent overpayment.
7. Use optimistic `@Version` for drafts; pessimistic locks only during financial posting/reversal.
8. Posted payment and allocations are immutable; corrections use reversal entries.
9. One original journal and at most one effective reversal per payment; idempotency protects network retries.
10. Do not make external network calls inside the financial transaction. Use an outbox for notifications/integrations if needed.
11. Use aggregate/projection queries for list balances; avoid N+1 reads and loading all allocations into memory.
12. Bound page size, validate filters, use indexes aligned to company/date/status/party/document access patterns, inspect query plans at representative scale.
13. Rebuild and reconcile any cached settlement summary from posted allocation history.
14. Add permission checks for view/create/edit/post/void/reverse and audit all lifecycle changes.

## Phased Delivery

### Phase 1 — Shared contracts and module adapters

- Define direction, settlement document type, party/currency contract, payment lifecycle, and amount/rounding policy.
- Implement adapter registry and read-only adapters for current purchase bills and expenses.
- Mark current immediate-paid expenses as non-settleable; add explicit payable expense design contract for future work.
- Add read-only customer invoice/sales bill adapter contract with examples, even if modules are not implemented yet.
- Add contract tests every adapter must pass.

**Exit:** each current/future module integrates through the same contract; payment core has no direct dependency on bill/expense entity implementation details.

### Phase 2 — Minimal persistence and settlement queries

- Replace demo payment scaffold with compact payment header/allocation model and chosen target reference strategy.
- Add database constraints/indexes, payment number generation, audit and idempotency storage, versioned migrations.
- Build company-scoped eligible-document and settlement-history queries with pagination/aggregation.
- Expose settled/outstanding/state in purchase bill detail and list.

**Exit:** balances are derived correctly and query count/performance stays bounded on seeded large data.

### Phase 3 — Shared draft and allocation workflow

- Implement create, list, detail, draft edit, and void APIs.
- Validate parties, direction, document type, currency, line uniqueness, positive amounts, and allocation sum.
- Build payment list/form/detail UI and source-document launch actions for bills and payable expenses.
- Add partial payment and many-document allocation UX.

**Exit:** one document accepts multiple draft/posted payment allocations over time; one draft can target multiple documents; drafts do not affect balances.

### Phase 4 — Atomic accounting post

- Add generic settlement posting to accounting engine and `PAYMENT` source.
- Lock source payment and eligible targets in deterministic order; validate current outstanding under lock.
- Aggregate journal lines by control ledger; validate balanced entry; save journal and mark payment posted atomically.
- Add posting idempotency and outbox only where post-commit side effects exist.
- Integrate current bill adapter; add payable expense adapter once its accrual workflow is implemented.

**Exit:** partial, exact, and multi-document payment/receipt work; race tests prove no target is over-settled; journal failure rolls back all effects.

### Phase 5 — Reversal and full UI lifecycle

- Implement reasoned reversal with opposite journal and restored document outstanding.
- Add payment detail audit, journal navigation, reverse permission, confirmation flow, and conflict states.
- Add reconciliation query/report for payment amount vs cash journal and allocation totals vs payable/receivable journal lines.

**Exit:** reversed payment history is preserved, balances restore exactly, and reconciliation flags injected mismatch cases.

### Phase 6 — Future module onboarding and hardening

- Add sales invoice/sales bill adapters when their recognition journals and receivable controls exist.
- Add customer receipts through the same `INCOMING` API/UI; no new payment entities or allocation engine.
- Add refund/credit note direction/sign rules as explicit adapters and workflows.
- Add load testing, metrics/alerts, backup/restore, period-date rules, feature controls, and operational runbook.

**Exit:** new modules onboard by implementing and passing adapter contract tests; no payment core rewrite is needed.

## Verification Plan

### Core unit rules

- Allocation arithmetic, currency scale, rounding, and equality/remaining rules.
- Direction/type compatibility and adapter registry behavior.
- Settlement-state calculation with several posted payments, partial applications, and reversals.
- Journal aggregation/balance for multiple target documents.
- Idempotency request hash and status transitions.

### API/integration rules

- Create/edit/void draft and verify no settlement/journal impact.
- Two or more partial payments against one bill; assert outstanding and states after each.
- One payment allocated across multiple bills; assert one cash movement/journal and correct per-bill totals.
- Incoming customer receipt contract behaves symmetrically once invoice adapter exists.
- Reject wrong company, party, direction, currency, draft/cancelled target, inactive ledger, invalid amount, and stale outstanding.
- Concurrent payments compete for the same outstanding balance; no over-settlement.
- Retry same post/reverse idempotency key; ensure one journal; changed payload conflicts.
- Simulated journal error rolls back payment state and all allocations.
- Reverse payment and verify immutable record plus exact outstanding restoration.

### UI acceptance flows

1. Open a bill, start payment, make a partial payment, return to bill, see reduced outstanding.
2. Start another payment from the same bill, pay remaining amount, see `PAID` and both payments in history.
3. Start payment from Payments, select several bills for one supplier, post once, and verify each bill's balance/history.
4. Open a payable expense and settle it using the same flow after payable expense mode is available.
5. Open a customer invoice and receive partial then final payments using same form in incoming direction.
6. Encounter a stale balance, refresh values, adjust, and post safely.
7. Reverse a posted payment with reason; verify original and reversal remain visible and balances restore.

## Definition of Done

The centralized payment engine is production ready when one payment can allocate to many supported documents, one document can be settled by many payments, all module types go through the shared adapter/API, journal impacts are balanced/atomic/idempotent/reversible, concurrency cannot over-settle, UI supports both entry directions and source-document launch, permission/audit/reconciliation exist, and performance is measured at expected volume.
