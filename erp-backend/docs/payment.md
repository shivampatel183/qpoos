# Payment Design

## Purpose

Payments settle supplier bills and expenses. A payment is a financial document and must be recorded separately from the purchase transaction that created the payable balance.

The payment design supports:

- Full and partial payments
- One payment allocated across multiple bills
- Multiple payments against one bill
- Cash, bank, card, cheque, and UPI payments
- Payment cancellation and reversal
- Audit history
- Double-entry accounting through payment vouchers

## Current Transaction Model

The current transaction model contains:

- `totalAmount`
- `paymentStatus`
- `paymentDate`
- `transactionType`

These fields are useful as a temporary summary, but they are not sufficient as the source of truth for payments. A bill can have multiple payments with different dates, methods, references, and amounts.

Payment records should therefore be stored separately. The transaction payment status should be derived from payment allocations.

## Recommended Tables

### `payments`

Stores the payment document header.

| Column | Type | Description |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `company_id` | uuid | Owning company; required |
| `financial_year_id` | bigint | Financial year |
| `payment_number` | varchar(80) | Unique payment number per company and year |
| `payment_date` | date | Date on which payment was made |
| `vendor_id` | bigint | Supplier receiving the payment |
| `payment_method` | varchar(30) | CASH, BANK, UPI, CHEQUE, CARD |
| `source_ledger_id` | bigint | Cash, bank, or card ledger credited by the payment |
| `amount` | numeric(19,4) | Total payment amount |
| `reference_number` | varchar(100) | Cheque number, UTR, UPI reference, or bank reference |
| `status` | varchar(30) | DRAFT, POSTED, CANCELLED, REVERSED |
| `notes` | text | Optional remarks |
| `voucher_id` | bigint | Linked accounting voucher |
| `created_by` | uuid | Creating user |
| `created_at` | timestamp | Creation timestamp |
| `updated_by` | uuid | Last modifying user |
| `updated_at` | timestamp | Last modification timestamp |

Recommended indexes:

- Unique index on `company_id, financial_year_id, payment_number`
- Index on `company_id, payment_date`
- Index on `company_id, vendor_id`
- Index on `company_id, status`
- Unique index on `voucher_id` when a payment has been posted

### `payment_allocations`

Connects a payment to purchase bills or expenses.

| Column | Type | Description |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `company_id` | uuid | Owning company; required |
| `payment_id` | bigint | Payment header reference |
| `transaction_id` | bigint | Purchase bill or expense reference |
| `allocated_amount` | numeric(19,4) | Amount applied to the transaction |
| `created_at` | timestamp | Creation timestamp |

Recommended indexes and constraints:

- Index on `payment_id`
- Index on `transaction_id`
- Index on `company_id, transaction_id`
- `allocated_amount` must be greater than zero
- Payment and transaction must belong to the same company
- The total allocation cannot exceed the payment amount
- The total allocation cannot exceed the transaction outstanding balance

A unique constraint on `payment_id, transaction_id` is recommended if one payment should contain only one allocation row per bill. Otherwise, repeated allocations must be explicitly supported and audited.

## Accounting Voucher

Every posted payment must create or reference one `PAYMENT` voucher.

The payment record stores the business document. The voucher stores the immutable accounting impact.

Typical supplier payment:

```text
Dr Accounts Payable
Cr Bank or Cash
```

Typical direct expense payment when no payable bill exists:

```text
Dr Expense
Cr Bank or Cash
```

Recommended voucher metadata:

| Field | Value |
| --- | --- |
| `voucher_type` | PAYMENT |
| `source_module` | PURCHASE_PAYMENT |
| `source_document_id` | Payment ID |
| `reference_number` | Payment reference |
| `voucher_date` | Payment date |
| `status` | POSTED after successful posting |

The voucher must be balanced before posting. Payment creation and voucher creation should happen in one database transaction.

## Payment Status

Payment status should be calculated from the bill total and its allocations.

```text
allocated amount = 0
    -> UNPAID

allocated amount > 0 and less than bill total
    -> PARTIALLY_PAID

allocated amount = bill total
    -> PAID
```

`OVERDUE` applies when:

```text
due date < current date
and allocated amount < bill total
and the bill is not cancelled
```

`CANCELLED` should represent a cancelled bill or an explicitly cancelled business document. It should not be used to hide a posted payment. A posted payment should be marked `CANCELLED` or `REVERSED` in the payment table and should have a corresponding accounting reversal.

The existing `paymentStatus` and `paymentDate` fields on the transaction can remain as denormalized summary fields during migration, but they should not be the authoritative payment history.

## Business Workflow

```text
Create payment draft
-> Select vendor
-> Select payment date and payment method
-> Select cash or bank ledger
-> Enter amount and reference number
-> Allocate amount to one or more bills
-> Validate allocations
-> Save payment
-> Create and post PAYMENT voucher
-> Recalculate bill outstanding balance and status
```

## Validation Rules

- Payment amount must be greater than zero.
- Payment date is required.
- Vendor is required for supplier payments.
- Source ledger is required and must be a cash, bank, or card ledger.
- Payment method must match the source ledger where applicable.
- At least one allocation is required for a bill settlement payment.
- Total allocation must not exceed the payment amount.
- Allocation must not exceed the selected bill's outstanding balance.
- Allocated bills must belong to the selected vendor.
- Payment and all allocated bills must belong to the active company.
- Payment date must fall inside an open accounting period.
- Posted payments cannot be edited directly.
- Cancelled or posted payments must not be physically deleted.
- A posted payment must be reversed with a reversal voucher.
- Cheque, UPI, and bank payments should require a reference number when configured.

## Examples

### Partial payment

```text
Bill total:       25,000.00
Previous paid:    10,000.00
Outstanding:      15,000.00
New payment:      15,000.00
```

Posting:

```text
Dr Accounts Payable     15,000.00
Cr HDFC Bank            15,000.00
```

The bill status becomes `PAID`.

### One payment for multiple bills

```text
Payment P-001:      10,000.00
Bill B-1001:         6,000.00
Bill B-1002:         4,000.00
```

Allocations:

```text
P-001 -> B-1001 -> 6,000.00
P-001 -> B-1002 -> 4,000.00
```

### Advance payment

If a payment is made before a bill exists, do not create a fake bill. Store the payment with no bill allocation or allocate it to a vendor advance ledger. When the bill is later created, the advance can be allocated to that bill.

Accounting entry:

```text
Dr Vendor Advance
Cr Bank or Cash
```

## Cancellation and Reversal

Posted accounting documents should be immutable.

To undo a posted payment:

1. Mark the original payment as `REVERSED`.
2. Create a reversal voucher with the opposite debit and credit lines.
3. Reverse or remove the payment allocations through an audited action.
4. Recalculate the affected bills' outstanding amounts and statuses.
5. Preserve the original payment and reversal references.

Do not delete the payment row or alter the original posted voucher.

## Reporting Queries

Outstanding balance for a transaction:

```text
transaction total
- sum of posted payment allocations
= outstanding balance
```

Supplier payment report should support filters for:

- Company
- Vendor
- Payment date range
- Payment method
- Source ledger
- Payment status
- Financial year
- Reference number

## Recommended Implementation Order

1. Add payment and payment allocation domain models.
2. Add payment status and payment method enums.
3. Add repositories with company-scoped queries.
4. Add payment creation and allocation validation.
5. Add payment voucher posting.
6. Add payment list, detail, cancel, and reverse operations.
7. Recalculate transaction payment summaries from posted allocations.
8. Add audit events and payment reports.
9. Migrate or retire the old transaction-level payment fields.

## Design Principle

Purchase transactions record what the company owes. Payments record how and when the company settles that obligation. Vouchers remain the accounting source of truth, while payment allocations connect business documents to accounting entries.
