# Accounting posting engine

## Current scope

`AccountingPostingService` is the shared backend entry point for posting the currently implemented accounting sources:

- Purchase bills posted through `POST /api/purchase-bills/{id}/post`
- Expenses saved through `POST /api/transactions` (save and post in one transaction)

Business services do not create journal lines. The engine loads the source under the authenticated company, recalculates and validates persisted amounts, resolves that company's existing ledgers, validates a balanced entry, and saves the journal and source posting reference in one transaction. Repeated posting requests return the existing journal.

Journal entries and lines are available from `GET /api/accounting/journals` and `GET /api/accounting/journals/{id}`. These reads are company-scoped. The Chart of Accounts balance now presents opening balance plus/minus posted journal activity after the opening-balance date; it does not store a separate mutable balance.

## Initial account policy

The company chart installer creates these ledgers for the first posting implementation:

| Ledger code | Account purpose |
| --- | --- |
| `INVENTORY_ASSET` | Inventory purchases; grouped under Other Current Assets |
| `PURCHASE_ACCOUNT` | Non-inventory purchase lines and default item expenses |
| `GST_INPUT_CREDIT` | Purchase tax treated as recoverable input GST |
| `SUPPLIER_PAYABLE` | Credit side of purchase bills and unpaid expenses |
| Cash and bank ledgers | Credit side of paid expenses, selected when recording the expense |
| `ROUND_OFF_LOSS` / `ROUND_OFF_GAIN` | Document rounding differences |

Tracked product classification is currently based on `ProductType.INVENTORY`. Purchase and expense tax is available only as a document/line total, not split into GST components. The agreed first version therefore posts the stored tax to one GST Receivable ledger and treats it as recoverable. Tax eligibility, component-level tax ledgers, and jurisdiction rules are not represented by the current source documents.

Expenses require a payment date and a company-owned, active, postable Cash or Bank ledger selected from the Chart of Accounts. Each expense line selects a postable Chart of Accounts ledger and records its rate, discount, and tax rate; the line amount is calculated as rate less discount plus tax. Expense creation and journal posting are atomic: if validation or posting fails, neither the expense nor journal is saved. Legacy expenses without a recorded payment ledger display "Not recorded"; they are not assigned a fabricated account or silently rewritten. Partially paid expenses are not supported because the source does not identify paid and outstanding portions.

## Safety and reporting behavior

- Company ID is taken from the authenticated principal, not the request body.
- Posting locks the source row, uses the existing company-scoped number sequence, and relies on unique database constraints for journal number and source-event duplication.
- Journal lines require an active, postable ledger owned by the same company and exactly one positive debit or credit amount.
- Debit and credit totals are compared using `BigDecimal.compareTo`.
- Posted source documents cannot be edited or deleted through the expense transaction service.
- `GET /api/transactions` returns only expenses for the expense experience; Purchase Bills and Purchase Orders retain their dedicated pages and APIs.
- Expense accounting is posted immediately when saved; the legacy post endpoint remains idempotent for compatibility and older unposted records.
- Legacy purchase bills already marked `POSTED` without a journal are not silently reposted; they need reconciliation.
- Supplier payments are posted through `POST /api/payments/{id}/post`, including purchase-bill and payable-expense allocations. The engine does not create entries for purchase orders, sales documents, or returns yet. Incoming customer receipts remain unavailable until sales invoices/receivables exist.
- There is no accounting-period close model. Financial-year configuration is used for journal numbering, but a closed-period check is not yet possible.
- Manual journals, reversal APIs, General Ledger detail, and Trial Balance are later phases.

## Schema and verification

The backend currently relies on Hibernate schema updates (`spring.jpa.hibernate.ddl-auto`, default `update`) and does not have Flyway or another configured migration runner. Journal tables, constraints, indexes, and source references are mapped as JPA entities and are created/updated by the existing Hibernate setup. Environments that disable schema updates must provision these tables before deploying this code.

Run focused accounting tests:

```powershell
cd erp-backend
.\mvnw.cmd '-Dtest=JournalValidatorTest,PurchaseBillEndpointTest,ExpensePostingEndpointTest' test
```

Run the full backend suite:

```powershell
cd erp-backend
.\mvnw.cmd clean test
```

At implementation time, the clean full suite reports one unrelated pre-existing error:
`CompanyServiceTest.switchCompanyUsesConfiguredAccessTokenExpiration` fails with `User not found`.
The remaining backend tests, including the journal posting tests, pass.
