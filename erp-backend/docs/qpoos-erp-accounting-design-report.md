# QPOOS ERP Accounting Design Report

Version: 1.0  
Target module: Accounting and Finance  
Backend stack: Spring Boot 3.5, Java 17, Spring Data JPA, PostgreSQL, Bean Validation, Spring Security, OpenAPI  
Document purpose: Complete functional and technical blueprint for the QPOOS ERP accounting engine, chart of accounts, posting system, database model, APIs, reports, business rules, and implementation roadmap.

## 1. Executive Summary

QPOOS ERP requires an accounting engine that can support Indian business operations, statutory reporting, auditability, multi-company usage, and future modules such as sales, purchase, inventory, payroll, manufacturing, GST, TDS, TCS, and banking.

The accounting design must be built around a stable double-entry core:

- Every financial impact is stored as a voucher.
- Every voucher contains two or more voucher lines.
- Total debit must always equal total credit.
- Ledger balances are derived from posted voucher lines.
- Reports are generated from immutable accounting entries, not from transaction screens directly.

This report defines the complete component map for the first production-ready accounting release.

## 2. Scope

### 2.1 Included Components

- Company accounting profile
- Financial years and accounting periods
- Chart of account types
- Account group hierarchy
- Ledger master
- Opening balances
- Voucher types
- Voucher header and voucher lines
- Posting engine
- Numbering series
- Tax codes
- Currencies
- Cost centers
- Projects
- Dimensions
- Audit trail
- Ledger reports
- Day book
- Cash book
- Bank book
- Trial balance
- Profit and loss statement
- Balance sheet
- Cash flow statement
- GST reports
- API design
- Validation matrix
- Business rules catalogue
- Spring Boot package design
- Implementation roadmap

### 2.2 Excluded From Volume 1

These areas should integrate with accounting later but are not fully designed in this first report:

- Full inventory valuation engine
- Payroll calculation engine
- Manufacturing costing engine
- Fixed asset depreciation automation
- Bank reconciliation automation
- E-invoice and e-way bill integration
- Advanced budgeting and forecasting

## 3. Accounting Foundation

### 3.1 Accounting Equation

The accounting system follows:

```text
Assets = Liabilities + Equity
```

Revenue and expenses affect equity through profit or loss:

```text
Profit = Income - Expenses
Closing Equity = Opening Equity + Profit - Drawings + Capital Introduced
```

### 3.2 Double Entry

Every voucher must have at least one debit and at least one credit. For each voucher:

```text
Sum(debit amount) = Sum(credit amount)
```

The system must reject unbalanced vouchers before posting.

### 3.3 Normal Balance

| Account type | Normal balance | Debit increases | Credit increases |
| --- | --- | --- | --- |
| Asset | Debit | Yes | No |
| Expense | Debit | Yes | No |
| Liability | Credit | No | Yes |
| Equity | Credit | No | Yes |
| Income | Credit | No | Yes |

### 3.4 Accounting Basis

QPOOS ERP should support accrual accounting by default. Cash-basis reporting can be added later as a report view, but the core ledger should remain accrual-based.

### 3.5 Indian Accounting Requirements

The accounting engine should support:

- GST input and output ledgers
- CGST, SGST, IGST, and Cess classification
- TDS payable and TDS receivable ledgers
- TCS payable and TCS receivable ledgers
- PF payable, ESI payable, and salary payable
- Customer and supplier control accounts
- Bank and cash ledgers
- Financial year April to March by default
- Audit log for voucher creation, approval, cancellation, and reversal

## 4. High-Level Accounting Architecture

```text
Company
|-- Financial Year
|   |-- Accounting Period
|
|-- Chart of Accounts
|   |-- Account Type
|   |-- Account Group
|   |-- Ledger
|   |-- Opening Balance
|
|-- Voucher Management
|   |-- Voucher Type
|   |-- Numbering Series
|   |-- Voucher Header
|   |-- Voucher Line
|
|-- Posting Engine
|   |-- Validation
|   |-- Balancing
|   |-- Tax Split
|   |-- Ledger Impact
|   |-- Audit Event
|
|-- Reporting Engine
|   |-- Ledger Statement
|   |-- Day Book
|   |-- Trial Balance
|   |-- Profit and Loss
|   |-- Balance Sheet
|   |-- Cash Flow
|   |-- GST Reports
```

## 5. Component Design

### 5.1 Company

Purpose: Owns all accounting data and provides the legal/business context for reports.

Responsibilities:

- Store legal name, trade name, GSTIN, PAN, address, currency, and time zone.
- Control default financial year setup.
- Isolate accounting data between companies.
- Provide company context for every accounting API.

Business rules:

- A company must be active before postings are allowed.
- GST reports require valid GSTIN and state code.
- A company cannot be deleted after vouchers exist.
- Company data must be scoped by authenticated user permissions.

Database relationships:

- One company has many financial years.
- One company has many account groups.
- One company has many ledgers.
- One company has many vouchers.

### 5.2 Financial Year

Purpose: Defines the statutory accounting year.

Responsibilities:

- Store start date and end date.
- Track open, locked, and closed status.
- Prevent postings outside open years.
- Support carry-forward of closing balances.

Business rules:

- Date ranges must not overlap for the same company.
- A financial year can be closed only when all periods are locked.
- A closed financial year cannot accept new postings.
- Reopening a year must require elevated permission and audit reason.

### 5.3 Accounting Period

Purpose: Provides monthly or quarterly lock control within a financial year.

Responsibilities:

- Store period start and end date.
- Allow month-end locks.
- Support statutory return periods.

Business rules:

- A locked period cannot accept create, update, cancel, or reverse operations.
- Periods must be continuous inside their financial year.
- GST return generation should reference period ranges.

### 5.4 Account Type

Purpose: Defines the top-level accounting classification.

Default account types:

- Asset
- Liability
- Equity
- Income
- Expense

Responsibilities:

- Define normal balance.
- Define financial statement mapping.
- Control whether balances appear in balance sheet or profit and loss.

Business rules:

- Account types are system-controlled.
- Account type code must be unique.
- Normal balance cannot be changed after ledger posting exists.

### 5.5 Account Group

Purpose: Groups ledgers into a reporting hierarchy.

Responsibilities:

- Support recursive parent-child hierarchy.
- Provide report roll-up.
- Control whether child ledgers are allowed.
- Store display order and account numbering prefix.

Business rules:

- A group belongs to exactly one account type.
- A group may have a parent group under the same account type.
- A group cannot be its own parent.
- Circular hierarchy is not allowed.
- Posting is never allowed directly to a group.
- If a group has ledgers, group deletion is blocked.

Example hierarchy:

```text
Assets
|-- Current Assets
|   |-- Cash and Cash Equivalents
|   |   |-- Cash in Hand
|   |   |-- Petty Cash
|   |-- Bank Accounts
|   |   |-- SBI Current Account
|   |   |-- HDFC Current Account
|   |-- Accounts Receivable
|   |-- Inventory
|   |-- GST Receivable
|-- Non-Current Assets
|   |-- Fixed Assets
|   |-- Capital Work in Progress
```

### 5.6 Ledger

Purpose: Posting-level account used in vouchers and reports.

Responsibilities:

- Store account code, ledger name, group, normal balance, and control flags.
- Accept debit and credit entries through voucher lines.
- Support customer, supplier, bank, cash, tax, salary, and generic ledgers.

Business rules:

- Every ledger belongs to exactly one account group.
- Ledger code must be unique inside company.
- Ledger name must be unique inside account group unless business decides otherwise.
- Inactive ledgers cannot be used in new vouchers.
- Control ledgers can only be posted by allowed source modules.
- Manual posting to GST control ledgers should require permission.
- A ledger cannot be deleted after voucher lines exist.

### 5.7 Ledger Opening Balance

Purpose: Stores initial balance at the beginning of a financial year.

Responsibilities:

- Capture opening debit or credit balance.
- Support migration from existing accounts.
- Feed opening balance into ledger, trial balance, and balance sheet reports.

Business rules:

- Opening balance is editable only before first posting for that ledger in the financial year.
- Opening balances must maintain overall trial balance equality.
- Asset and expense ledgers usually open with debit balances.
- Liability, equity, and income ledgers usually open with credit balances.

### 5.8 Voucher Type

Purpose: Defines transaction category and posting behavior.

Default voucher types:

- Sales Invoice
- Purchase Invoice
- Payment
- Receipt
- Journal
- Contra
- Debit Note
- Credit Note
- Stock Journal
- Manufacturing Journal
- Opening Balance
- Adjustment

Responsibilities:

- Store code, name, category, numbering series, approval requirement, and reversal behavior.
- Control permitted source modules.
- Control whether tax lines are required or optional.

Business rules:

- Voucher type code must be unique.
- System voucher types cannot be deleted.
- Voucher type determines number generation.
- Voucher type determines report classification.

### 5.9 Voucher Header

Purpose: Stores transaction-level accounting document.

Responsibilities:

- Store voucher number, date, company, financial year, voucher type, status, narration, reference number, source module, and source document ID.
- Track created by, posted by, approved by, cancelled by, and timestamps.

Status lifecycle:

```text
Draft -> Submitted -> Approved -> Posted -> Reversed
Draft -> Cancelled
Posted -> Reversed
```

Business rules:

- Posted vouchers cannot be edited.
- Posted vouchers can only be reversed using reversal vouchers.
- Voucher date must fall inside an open financial year and unlocked period.
- Voucher number must be unique per company, voucher type, and financial year.
- A voucher must have at least two lines before posting.

### 5.10 Voucher Line

Purpose: Stores debit or credit entry against a ledger.

Responsibilities:

- Store ledger, debit amount, credit amount, narration, tax code, cost center, project, dimension, party, and line order.
- Feed ledger balances and reports.

Business rules:

- A line cannot have both debit and credit amounts.
- A line cannot have both amounts as zero.
- Debit and credit values must be positive.
- Ledger must be active and posting-enabled.
- Cost center may be mandatory for selected expense ledgers.
- Project may be mandatory for project accounting ledgers.

### 5.11 Posting Engine

Purpose: Converts validated business documents into immutable ledger impact.

Responsibilities:

- Validate voucher header and lines.
- Resolve financial year and period.
- Validate ledger permissions.
- Validate debit-credit balance.
- Generate voucher number.
- Persist voucher atomically.
- Mark voucher as posted.
- Write audit event.
- Publish domain event for downstream reports or cache refresh.

Posting flow:

```text
Receive request
-> Validate company access
-> Resolve financial year
-> Resolve accounting period
-> Validate voucher type
-> Validate ledgers
-> Validate dimensions
-> Validate tax codes
-> Validate debit equals credit
-> Generate voucher number
-> Save voucher header
-> Save voucher lines
-> Mark posted
-> Write audit log
-> Return voucher response
```

### 5.12 Reporting Engine

Purpose: Generates accounting reports from vouchers and opening balances.

Responsibilities:

- Build report queries.
- Apply date, company, financial year, ledger, group, cost center, and project filters.
- Include opening, period movement, and closing balances.
- Export to JSON, Excel, and PDF in later versions.

Business rules:

- Reports should read from posted vouchers only by default.
- Draft and cancelled vouchers must be excluded.
- Reversed vouchers should be shown based on report options.
- Ledger reports must include opening balance.
- Trial balance must always balance for a valid date range.

## 6. Chart of Accounts

### 6.1 Account Numbering

Recommended numbering:

| Range | Account type |
| --- | --- |
| 1000-1999 | Assets |
| 2000-2999 | Liabilities |
| 3000-3999 | Equity |
| 4000-4999 | Income |
| 5000-5999 | Cost of goods sold |
| 6000-7999 | Expenses |
| 8000-8999 | Other income |
| 9000-9999 | Contra, suspense, and control |

### 6.2 Core Account Groups

Assets:

- Current Assets
- Cash and Cash Equivalents
- Bank Accounts
- Accounts Receivable
- Inventory
- GST Receivable
- Advances and Deposits
- Fixed Assets
- Capital Work in Progress

Liabilities:

- Current Liabilities
- Accounts Payable
- GST Payable
- Statutory Payables
- Loans and Borrowings
- Salary Payable
- Customer Advances

Equity:

- Capital Account
- Drawings
- Reserves and Surplus
- Retained Earnings
- Current Year Profit or Loss

Income:

- Sales
- Domestic Sales
- Export Sales
- Job Work Income
- Service Income
- Interest Income
- Discount Received
- Rental Income
- Miscellaneous Income

Expenses:

- Purchases
- Direct Expenses
- Employee Benefits
- Rent and Utilities
- Freight and Logistics
- Repairs and Maintenance
- Office Expenses
- Selling and Distribution
- Finance Costs
- Depreciation

### 6.3 Ledger Documentation Template

Every predefined ledger should be documented with this structure:

- Ledger name
- Ledger code
- Purpose
- Business usage
- Parent group
- Account type
- Normal balance
- Posting rules
- Restrictions
- Financial statement mapping
- Example journal entries
- Sample report impact
- Database mapping
- Default behavior
- Validation rules

### 6.4 Sample Ledger Details

#### Cash in Hand

Purpose: Tracks physical cash held by the company.

Business usage:

- Cash receipts from customers.
- Cash payments for small expenses.
- Cash deposits to bank.

Parent group: Cash and Cash Equivalents  
Account type: Asset  
Normal balance: Debit  
Financial statement: Balance Sheet, Current Assets

Posting rules:

- Debit when cash is received.
- Credit when cash is paid or deposited.
- Manual entries allowed only to authorized users.

Restrictions:

- Cannot have negative balance unless company explicitly allows overdraft cash handling.
- Should not be used for bank transactions.

Example entries:

```text
Cash receipt from customer:
Dr Cash in Hand
Cr Accounts Receivable

Cash rent payment:
Dr Rent Expense
Cr Cash in Hand
```

Validation rules:

- Cash ledger must be marked as cash account.
- Cash book report includes this ledger.
- Cost center is optional unless expense side requires it.

#### SBI Current Account

Purpose: Tracks transactions for a specific SBI bank account.

Parent group: Bank Accounts  
Account type: Asset  
Normal balance: Debit  
Financial statement: Balance Sheet, Current Assets

Posting rules:

- Debit for deposits and receipts.
- Credit for withdrawals and payments.
- Bank ledger should support cheque number, UTR, transaction date, and reconciliation status in later phases.

Example entries:

```text
Customer receipt by bank:
Dr SBI Current Account
Cr Accounts Receivable

Supplier payment by bank:
Dr Accounts Payable
Cr SBI Current Account
```

#### Accounts Receivable

Purpose: Customer control account for credit sales.

Parent group: Accounts Receivable  
Account type: Asset  
Normal balance: Debit  
Financial statement: Balance Sheet, Current Assets

Posting rules:

- Debit on sales invoice.
- Credit on customer receipt, credit note, or write-off.
- Manual posting should be restricted if customer sub-ledger is active.

Example entries:

```text
Sales invoice:
Dr Accounts Receivable
Cr Domestic Sales
Cr Output GST Payable
```

#### Accounts Payable

Purpose: Supplier control account for credit purchases.

Parent group: Accounts Payable  
Account type: Liability  
Normal balance: Credit  
Financial statement: Balance Sheet, Current Liabilities

Posting rules:

- Credit on purchase invoice.
- Debit on supplier payment, debit note, or write-off.

Example entries:

```text
Purchase invoice:
Dr Purchase
Dr Input GST Receivable
Cr Accounts Payable
```

#### Input GST Receivable

Purpose: Tracks GST input tax credit receivable from purchases.

Parent group: GST Receivable  
Account type: Asset  
Normal balance: Debit  
Financial statement: Balance Sheet, Current Assets

Posting rules:

- Debit on eligible purchase invoice.
- Credit on GST set-off or reversal.

Restrictions:

- Manual adjustment requires tax permission.
- GST rate and tax code must be present on source line.

#### Output GST Payable

Purpose: Tracks GST collected on sales and payable to government.

Parent group: GST Payable  
Account type: Liability  
Normal balance: Credit  
Financial statement: Balance Sheet, Current Liabilities

Posting rules:

- Credit on taxable sales.
- Debit on GST payment, credit note, or set-off.

#### Domestic Sales

Purpose: Records sales revenue within India.

Parent group: Domestic Sales  
Account type: Income  
Normal balance: Credit  
Financial statement: Profit and Loss

Posting rules:

- Credit on sales invoice.
- Debit on sales return or credit note.

Restrictions:

- Taxable sales should require GST tax code unless exempt or non-GST.

#### Purchase

Purpose: Records purchase of goods or materials.

Parent group: Purchases  
Account type: Expense or COGS  
Normal balance: Debit  
Financial statement: Profit and Loss

Posting rules:

- Debit on purchase invoice.
- Credit on purchase return or debit note.

#### Salary Expense

Purpose: Records employee salary expense.

Parent group: Employee Benefits  
Account type: Expense  
Normal balance: Debit  
Financial statement: Profit and Loss

Posting rules:

- Debit on payroll posting.
- Credit salary payable and statutory payable accounts.

#### Capital Account

Purpose: Tracks owner or shareholder capital.

Parent group: Capital Account  
Account type: Equity  
Normal balance: Credit  
Financial statement: Balance Sheet, Equity

Posting rules:

- Credit when owner introduces capital.
- Debit for withdrawal or capital reduction.

## 7. Recommended PostgreSQL Database Design

### 7.1 Naming Conventions

- Table names: plural snake_case.
- Primary key: `id`.
- Foreign keys: `{entity}_id`.
- Timestamps: `created_at`, `updated_at`, `posted_at`.
- Soft state fields: `status`, `active`.
- Monetary columns: `numeric(19,4)`.
- Codes: uppercase stable identifiers.

### 7.2 Tables

#### account_types

Purpose: Stores system account classifications.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| code | varchar(30) | Unique, required |
| name | varchar(100) | Required |
| normal_balance | varchar(10) | DEBIT or CREDIT |
| statement_type | varchar(30) | BALANCE_SHEET or PROFIT_LOSS |
| display_order | int | Required |
| system_defined | boolean | Default true |

Indexes:

- Unique index on `code`.

#### account_groups

Purpose: Stores recursive account group hierarchy.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| account_type_id | bigint | FK account_types |
| parent_group_id | bigint | Self FK nullable |
| code | varchar(50) | Unique per company |
| name | varchar(150) | Required |
| path | varchar(500) | Optional hierarchy path |
| level | int | Derived |
| display_order | int | Required |
| active | boolean | Default true |
| system_defined | boolean | Default false |

Indexes:

- Unique index on `company_id, code`.
- Index on `company_id, account_type_id`.
- Index on `parent_group_id`.

#### ledgers

Purpose: Stores posting-level accounts.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| account_group_id | bigint | FK account_groups |
| code | varchar(50) | Unique per company |
| name | varchar(180) | Required |
| ledger_type | varchar(50) | GENERAL, CASH, BANK, CUSTOMER, SUPPLIER, TAX, CONTROL |
| normal_balance | varchar(10) | DEBIT or CREDIT |
| posting_allowed | boolean | Default true |
| manual_posting_allowed | boolean | Default true |
| control_account | boolean | Default false |
| active | boolean | Default true |
| system_defined | boolean | Default false |

Indexes:

- Unique index on `company_id, code`.
- Index on `company_id, account_group_id`.
- Index on `ledger_type`.

#### ledger_opening_balances

Purpose: Stores opening balance by ledger and financial year.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| financial_year_id | bigint | FK financial_years |
| ledger_id | bigint | FK ledgers |
| debit_amount | numeric(19,4) | Default 0 |
| credit_amount | numeric(19,4) | Default 0 |
| locked | boolean | Default false |

Indexes:

- Unique index on `financial_year_id, ledger_id`.

#### voucher_types

Purpose: Stores voucher type configuration.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies, nullable for global types |
| code | varchar(50) | Unique per company |
| name | varchar(120) | Required |
| category | varchar(50) | SALES, PURCHASE, PAYMENT, RECEIPT, JOURNAL, CONTRA |
| requires_approval | boolean | Default false |
| active | boolean | Default true |
| system_defined | boolean | Default false |

#### vouchers

Purpose: Stores accounting document header.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| financial_year_id | bigint | FK financial_years |
| accounting_period_id | bigint | FK accounting_periods |
| voucher_type_id | bigint | FK voucher_types |
| voucher_number | varchar(80) | Unique per company, year, type |
| voucher_date | date | Required |
| status | varchar(30) | DRAFT, SUBMITTED, APPROVED, POSTED, CANCELLED, REVERSED |
| narration | text | Optional |
| reference_number | varchar(100) | Optional |
| source_module | varchar(50) | Optional |
| source_document_id | varchar(100) | Optional |
| total_debit | numeric(19,4) | Required |
| total_credit | numeric(19,4) | Required |
| created_by | bigint | FK users |
| posted_by | bigint | FK users nullable |
| posted_at | timestamp | Nullable |

Indexes:

- Unique index on `company_id, financial_year_id, voucher_type_id, voucher_number`.
- Index on `company_id, voucher_date`.
- Index on `status`.
- Index on `source_module, source_document_id`.

#### voucher_lines

Purpose: Stores accounting impact lines.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| voucher_id | bigint | FK vouchers |
| company_id | bigint | FK companies |
| ledger_id | bigint | FK ledgers |
| line_number | int | Required |
| debit_amount | numeric(19,4) | Default 0 |
| credit_amount | numeric(19,4) | Default 0 |
| narration | text | Optional |
| tax_code_id | bigint | FK tax_codes nullable |
| cost_center_id | bigint | FK cost_centers nullable |
| project_id | bigint | FK projects nullable |
| dimension_id | bigint | FK dimensions nullable |

Indexes:

- Index on `voucher_id`.
- Index on `company_id, ledger_id`.
- Index on `company_id, ledger_id, voucher_id`.

#### tax_codes

Purpose: Stores GST and other tax code definitions.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies nullable |
| code | varchar(50) | Unique |
| name | varchar(120) | Required |
| tax_type | varchar(30) | GST, TDS, TCS |
| rate | numeric(9,4) | Required |
| input_ledger_id | bigint | FK ledgers nullable |
| output_ledger_id | bigint | FK ledgers nullable |
| active | boolean | Default true |

#### financial_years

Purpose: Stores financial year boundaries and status.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| name | varchar(50) | Example FY 2026-27 |
| start_date | date | Required |
| end_date | date | Required |
| status | varchar(30) | OPEN, LOCKED, CLOSED |

#### accounting_periods

Purpose: Stores monthly or quarterly accounting lock periods.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| financial_year_id | bigint | FK financial_years |
| name | varchar(50) | Required |
| start_date | date | Required |
| end_date | date | Required |
| status | varchar(30) | OPEN, LOCKED, CLOSED |

#### numbering_series

Purpose: Generates voucher numbers.

Key columns:

| Column | Type | Rules |
| --- | --- | --- |
| id | bigint | Primary key |
| company_id | bigint | FK companies |
| voucher_type_id | bigint | FK voucher_types |
| financial_year_id | bigint | FK financial_years |
| prefix | varchar(40) | Example SI/FY26/ |
| next_number | bigint | Required |
| padding | int | Example 5 |
| suffix | varchar(40) | Optional |

Concurrency rule: Voucher number generation must lock the numbering row in the same transaction.

#### cost_centers

Purpose: Tracks department, branch, or unit-level expense and income analysis.

#### projects

Purpose: Tracks project-wise accounting and profitability.

#### dimensions

Purpose: Generic analytics dimension for future branch, product line, region, or channel reporting.

#### audit_logs

Purpose: Stores accounting-sensitive audit events.

Events:

- ACCOUNT_GROUP_CREATED
- LEDGER_CREATED
- OPENING_BALANCE_CHANGED
- VOUCHER_CREATED
- VOUCHER_APPROVED
- VOUCHER_POSTED
- VOUCHER_REVERSED
- FINANCIAL_YEAR_CLOSED

## 8. Posting Rules by Voucher

### 8.1 Sales Invoice

Business flow:

```text
Create sales invoice
-> Validate customer and tax
-> Calculate taxable value and GST
-> Post accounts receivable, sales, and output tax
-> Update reports
```

Typical posting:

```text
Dr Accounts Receivable
Cr Domestic Sales
Cr Output CGST Payable
Cr Output SGST Payable
```

Rules:

- Customer or receivable ledger is mandatory.
- Sales ledger is mandatory.
- GST tax code is mandatory for taxable supply.
- Voucher date must be inside open period.
- Invoice total must equal accounting total.

### 8.2 Purchase Invoice

Typical posting:

```text
Dr Purchase
Dr Input CGST Receivable
Dr Input SGST Receivable
Cr Accounts Payable
```

Rules:

- Supplier or payable ledger is mandatory.
- Input GST eligibility must be captured.
- Reverse charge mechanism should be supported in a later phase.

### 8.3 Payment

Typical supplier payment:

```text
Dr Accounts Payable
Cr Bank
```

Typical expense payment:

```text
Dr Expense
Cr Cash or Bank
```

Rules:

- Payment must credit one cash or bank ledger.
- Expense ledger requires cost center if configured.
- Bank payment should support payment reference.

### 8.4 Receipt

Typical customer receipt:

```text
Dr Bank
Cr Accounts Receivable
```

Rules:

- Receipt must debit one cash or bank ledger.
- Customer receipt should support invoice allocation later.

### 8.5 Journal

Purpose: General adjustment voucher.

Rules:

- Requires narration.
- May require approval if control ledgers are involved.
- Cannot post to locked period.

### 8.6 Contra

Purpose: Cash-to-bank, bank-to-cash, or bank-to-bank transfer.

Examples:

```text
Dr Bank
Cr Cash

Dr HDFC Bank
Cr SBI Bank
```

Rules:

- Both sides must be cash or bank ledgers.
- Contra vouchers should appear in cash book and bank book.

### 8.7 Debit Note

Purpose: Records purchase return or debit to supplier.

Typical posting:

```text
Dr Accounts Payable
Cr Purchase Return
Cr Input GST Receivable
```

### 8.8 Credit Note

Purpose: Records sales return or credit to customer.

Typical posting:

```text
Dr Sales Return
Dr Output GST Payable
Cr Accounts Receivable
```

### 8.9 Stock Journal

Purpose: Records stock value adjustment when inventory module is connected.

Rules:

- Should be posted by inventory module.
- Manual posting requires inventory admin permission.
- Must reconcile inventory value with accounting value.

### 8.10 Manufacturing Journal

Purpose: Moves raw material and conversion cost into finished goods.

Rules:

- Should be generated by manufacturing module.
- Must preserve cost traceability by batch, work order, or production order in later phases.

## 9. API Design

Base path:

```text
/api/v1/companies/{companyId}/accounting
```

### 9.1 Account Types

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/account-types` | List account types |
| GET | `/account-types/{id}` | Get account type |

### 9.2 Account Groups

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/account-groups` | Create group |
| GET | `/account-groups` | List tree or flat groups |
| GET | `/account-groups/{id}` | Get group |
| PUT | `/account-groups/{id}` | Update group |
| PATCH | `/account-groups/{id}/status` | Activate or deactivate |

Create group sample:

```json
{
  "accountTypeCode": "ASSET",
  "parentGroupId": 12,
  "code": "BANK_ACCOUNTS",
  "name": "Bank Accounts",
  "displayOrder": 20
}
```

### 9.3 Ledgers

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/ledgers` | Create ledger |
| GET | `/ledgers` | Search ledgers |
| GET | `/ledgers/{id}` | Get ledger |
| PUT | `/ledgers/{id}` | Update ledger |
| PATCH | `/ledgers/{id}/status` | Activate or deactivate |

Create ledger sample:

```json
{
  "accountGroupId": 22,
  "code": "BANK-SBI-001",
  "name": "SBI Current Account",
  "ledgerType": "BANK",
  "normalBalance": "DEBIT",
  "postingAllowed": true,
  "manualPostingAllowed": true
}
```

### 9.4 Opening Balances

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/opening-balances` | Create or update opening balance |
| GET | `/opening-balances` | List opening balances |
| POST | `/opening-balances/validate` | Validate opening trial balance |
| POST | `/opening-balances/lock` | Lock opening balances |

### 9.5 Vouchers

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/vouchers/draft` | Save draft voucher |
| POST | `/vouchers/post` | Create and post voucher |
| GET | `/vouchers` | Search vouchers |
| GET | `/vouchers/{id}` | Get voucher |
| POST | `/vouchers/{id}/submit` | Submit for approval |
| POST | `/vouchers/{id}/approve` | Approve voucher |
| POST | `/vouchers/{id}/post` | Post approved voucher |
| POST | `/vouchers/{id}/reverse` | Reverse posted voucher |
| POST | `/vouchers/{id}/cancel` | Cancel draft or submitted voucher |

Post voucher sample:

```json
{
  "voucherTypeCode": "JOURNAL",
  "voucherDate": "2026-04-15",
  "narration": "Opening adjustment",
  "referenceNumber": "ADJ-001",
  "lines": [
    {
      "ledgerId": 101,
      "debitAmount": 1000.00,
      "creditAmount": 0.00,
      "narration": "Debit adjustment"
    },
    {
      "ledgerId": 202,
      "debitAmount": 0.00,
      "creditAmount": 1000.00,
      "narration": "Credit adjustment"
    }
  ]
}
```

### 9.6 Reports

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/reports/ledger` | Ledger statement |
| GET | `/reports/day-book` | Day book |
| GET | `/reports/cash-book` | Cash book |
| GET | `/reports/bank-book` | Bank book |
| GET | `/reports/trial-balance` | Trial balance |
| GET | `/reports/profit-and-loss` | P&L |
| GET | `/reports/balance-sheet` | Balance sheet |
| GET | `/reports/cash-flow` | Cash flow |
| GET | `/reports/gst-summary` | GST summary |

## 10. Spring Boot Backend Design

Recommended package structure:

```text
com.qpoos.erp.accounting
|-- accounttype
|   |-- AccountTypeEntity
|   |-- AccountTypeRepository
|   |-- AccountTypeService
|   |-- AccountTypeController
|
|-- accountgroup
|   |-- AccountGroupEntity
|   |-- AccountGroupRepository
|   |-- AccountGroupService
|   |-- AccountGroupController
|   |-- dto
|
|-- ledger
|   |-- LedgerEntity
|   |-- LedgerRepository
|   |-- LedgerService
|   |-- LedgerController
|   |-- dto
|
|-- voucher
|   |-- VoucherEntity
|   |-- VoucherLineEntity
|   |-- VoucherRepository
|   |-- VoucherLineRepository
|   |-- VoucherService
|   |-- VoucherController
|   |-- PostingEngine
|   |-- VoucherValidator
|   |-- dto
|
|-- period
|   |-- FinancialYearEntity
|   |-- AccountingPeriodEntity
|   |-- FinancialYearService
|
|-- tax
|   |-- TaxCodeEntity
|   |-- TaxCodeService
|
|-- report
|   |-- AccountingReportService
|   |-- LedgerReportService
|   |-- TrialBalanceService
|   |-- FinancialStatementService
|
|-- common
|   |-- AccountingException
|   |-- MoneyUtils
|   |-- AccountingConstants
|   |-- AccountingPermission
```

### 10.1 Service Responsibilities

AccountGroupService:

- Create and update groups.
- Validate parent-child hierarchy.
- Build group tree.
- Prevent invalid deletion.

LedgerService:

- Create and update ledgers.
- Validate ledger code and group.
- Validate posting permission.
- Support ledger search.

VoucherService:

- Save drafts.
- Submit for approval.
- Post vouchers through PostingEngine.
- Reverse vouchers.
- Search vouchers.

PostingEngine:

- Execute all posting validations.
- Generate voucher number.
- Persist header and lines.
- Publish audit event.

ReportService:

- Generate ledger and financial reports.
- Use read-only transactions.
- Use optimized SQL or projections for performance.

## 11. Report Generation Design

### 11.1 Ledger Statement

Inputs:

- Company ID
- Ledger ID
- From date
- To date
- Include draft flag, default false

Output:

- Opening balance
- Transaction rows
- Debit
- Credit
- Running balance
- Closing balance

### 11.2 Day Book

Shows all posted vouchers for a date range, ordered by voucher date and voucher number.

### 11.3 Cash Book

Includes only ledgers where `ledger_type = CASH`.

### 11.4 Bank Book

Includes only ledgers where `ledger_type = BANK`.

### 11.5 Trial Balance

Calculation:

```text
Opening debit/credit
+ voucher debit/credit movement
= closing debit/credit
```

Rules:

- Total closing debit must equal total closing credit.
- Groups should roll up child ledger balances.
- Zero-balance rows may be hidden by report option.

### 11.6 Profit and Loss

Includes:

- Income accounts
- Cost of goods sold
- Direct expenses
- Indirect expenses
- Other income
- Finance costs
- Depreciation

Calculation:

```text
Net Profit = Total Income - Total Expenses
```

### 11.7 Balance Sheet

Includes:

- Assets
- Liabilities
- Equity
- Current year profit or loss

Validation:

```text
Assets = Liabilities + Equity
```

### 11.8 Cash Flow

Initial method: Indirect method.

Sections:

- Operating activities
- Investing activities
- Financing activities
- Net increase or decrease in cash
- Opening cash and bank
- Closing cash and bank

### 11.9 GST Reports

Core outputs:

- Output GST summary
- Input GST summary
- GST payable summary
- Tax-code-wise taxable value
- Customer or supplier GST details in later phases

## 12. Business Rules Catalogue

### 12.1 Master Data Rules

1. Every ledger belongs to exactly one account group.
2. Every account group belongs to exactly one account type.
3. A posting account cannot have child groups.
4. A group account cannot accept voucher entries.
5. Account type normal balance cannot be changed after posting.
6. Ledger code must be unique within a company.
7. Account group code must be unique within a company.
8. Inactive ledgers cannot be used in vouchers.
9. System-defined ledgers cannot be deleted.
10. Control accounts cannot be manually posted unless permitted.

### 12.2 Voucher Rules

11. A voucher must have at least two lines.
12. Voucher total debit must equal total credit.
13. Voucher date must be inside an open financial year.
14. Voucher date must be inside an unlocked accounting period.
15. Posted vouchers cannot be edited.
16. Posted vouchers can only be reversed.
17. Cancelled vouchers must not appear in financial reports.
18. Draft vouchers must not affect reports.
19. Voucher number must be unique.
20. Voucher line cannot have both debit and credit amounts.
21. Voucher line cannot have zero debit and zero credit.
22. Debit and credit amounts cannot be negative.
23. Voucher narration is mandatory for journal vouchers.
24. Reversal voucher must reference original voucher.
25. Original voucher reversal status must be updated after reversal.

### 12.3 Period Rules

26. Financial years cannot overlap for the same company.
27. Accounting periods cannot overlap within a financial year.
28. Closed financial years cannot accept postings.
29. Locked periods cannot accept postings.
30. Closing a year requires balanced trial balance.
31. Reopening a closed year requires audit reason.

### 12.4 Tax Rules

32. GST ledgers require tax code mapping.
33. Output GST is normally credit balance.
34. Input GST is normally debit balance.
35. GST manual adjustment requires permission.
36. Taxable sales require GST tax code unless marked exempt.
37. GST report must exclude cancelled vouchers.

### 12.5 Security and Audit Rules

38. User must have company access before using accounting APIs.
39. Posting requires accounting post permission.
40. Reversal requires accounting reverse permission.
41. Master data deletion requires admin permission.
42. Every posting must create audit log.
43. Every reversal must create audit log.
44. Every opening balance edit must create audit log.
45. Audit logs must not be editable from normal APIs.

### 12.6 Reporting Rules

46. Reports should include posted vouchers only by default.
47. Ledger report must include opening balance.
48. Trial balance must balance.
49. Profit and loss must include income and expense accounts only.
50. Balance sheet must include asset, liability, and equity accounts only.

## 13. Validation Matrix

| Area | Validation | Error example |
| --- | --- | --- |
| Account group | Parent under same account type | Parent group must belong to same account type |
| Account group | No circular parent | Circular account group hierarchy is not allowed |
| Ledger | Group active | Ledger cannot be created under inactive group |
| Ledger | Unique code | Ledger code already exists |
| Opening balance | One side only | Opening balance cannot have both debit and credit |
| Voucher | Lines required | Voucher requires at least two lines |
| Voucher | Balanced amount | Voucher debit and credit totals must match |
| Voucher | Open period | Voucher date is in a locked accounting period |
| Voucher line | Active ledger | Ledger is inactive |
| Voucher line | Posting allowed | Ledger does not allow posting |
| Tax | Active tax code | Tax code is inactive |
| Report | Valid date range | From date cannot be after to date |

## 14. Performance Guidelines

- Use indexes on company, date, ledger, status, and voucher type.
- Use read-only transactions for reports.
- Avoid loading voucher lines with full entity graphs for reports.
- Use projections or native SQL for large reports.
- Cache account type and account group tree when safe.
- Keep voucher posting transactional.
- Use pessimistic lock for numbering series.
- Paginate voucher search and ledger statements.
- Archive old audit logs only through controlled retention policy.

## 15. Security Guidelines

- Every accounting API must validate authenticated user.
- Every accounting API must validate company access.
- Separate permissions for create, update, approve, post, reverse, close period, and manage chart of accounts.
- Never trust company ID from request without permission check.
- Log sensitive accounting changes.
- Protect report exports with the same authorization as report views.

## 16. Implementation Roadmap

### Milestone 1: Foundation

- Create accounting package.
- Add account type seed data.
- Add financial year and accounting period entities.
- Add account group entity and APIs.
- Add ledger entity and APIs.

### Milestone 2: Opening Balances

- Add opening balance entity.
- Add validation for balanced opening trial balance.
- Add opening balance lock.

### Milestone 3: Voucher Core

- Add voucher type entity and seed data.
- Add numbering series.
- Add voucher header and voucher line entities.
- Add draft voucher APIs.
- Add posting engine.

### Milestone 4: Reports

- Ledger statement.
- Day book.
- Cash book.
- Bank book.
- Trial balance.
- Profit and loss.
- Balance sheet.

### Milestone 5: Tax and Dimensions

- Add tax code entity.
- Add GST posting support.
- Add cost centers.
- Add projects.
- Add dimensions.

### Milestone 6: Controls and Audit

- Add approval workflow.
- Add reversal workflow.
- Add period locking.
- Add audit logs.
- Add reporting export support.

## 17. Testing Strategy

### Unit Tests

- Account group hierarchy validation.
- Ledger validation.
- Voucher line validation.
- Debit-credit balancing.
- Numbering series generation.
- Posting engine rules.

### Integration Tests

- Create company and financial year.
- Create chart of accounts.
- Create opening balances.
- Post sales invoice.
- Post purchase invoice.
- Post payment and receipt.
- Reverse voucher.
- Generate trial balance.

### Report Tests

- Trial balance balances after postings.
- Ledger statement opening and closing balances are correct.
- Profit and loss net profit is correct.
- Balance sheet balances.
- Cancelled vouchers are excluded.
- Reversed vouchers are handled correctly.

## 18. Deliverables Checklist

- Complete accounting architecture.
- Complete chart of accounts structure.
- Ledger documentation template.
- Core Indian ledger examples.
- PostgreSQL table design.
- Posting rules by voucher type.
- Spring Boot package design.
- REST API specification.
- Report generation design.
- Business rules catalogue.
- Validation matrix.
- Performance guidelines.
- Security guidelines.
- Implementation roadmap.
- Testing strategy.

## 19. Next Expansion Plan

This report is the foundation document. It should be expanded into a full accounting design series in this order:

1. Full Indian Chart of Accounts with 500+ predefined ledgers.
2. Complete SQL DDL for PostgreSQL.
3. ERD diagram.
4. Full OpenAPI contract.
5. Posting engine pseudocode.
6. Report SQL query library.
7. Spring Boot implementation checklist.
8. Migration and seed data scripts.

