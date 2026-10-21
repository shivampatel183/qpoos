-- Lucidchart ERD import model for the implemented backend entities.
-- Relationship-focused: non-key business columns are intentionally omitted.
-- Scalar ID fields without JPA relationships (for example company.user_id,
-- product.category_id, and payment.voucher_id) are not modeled as foreign keys.

CREATE TABLE users (
    id UUID PRIMARY KEY
);

CREATE TABLE company (
    id UUID PRIMARY KEY,
    user_id UUID
);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id)
);

CREATE TABLE forgot_password_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id)
);

CREATE TABLE vendors (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id)
);

CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id)
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id)
);

CREATE TABLE payment_terms (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id)
);

CREATE TABLE document_number_sequences (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id)
);

CREATE TABLE account_types (
    id BIGINT PRIMARY KEY
);

CREATE TABLE account_type_definitions (
    id BIGINT PRIMARY KEY,
    account_group_id BIGINT NOT NULL REFERENCES account_types(id)
);

CREATE TABLE account_groups (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    account_type_id BIGINT NOT NULL REFERENCES account_types(id),
    account_type_definition_id BIGINT REFERENCES account_type_definitions(id),
    parent_group_id BIGINT REFERENCES account_groups(id)
);

CREATE TABLE ledgers (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    account_group_id BIGINT NOT NULL REFERENCES account_groups(id)
);

CREATE TABLE purchase_orders (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    payment_term_id BIGINT REFERENCES payment_terms(id)
);

CREATE TABLE purchase_order_lines (
    id BIGINT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id)
);

CREATE TABLE purchase_bills (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    payment_term_id BIGINT REFERENCES payment_terms(id),
    purchase_order_id BIGINT REFERENCES purchase_orders(id)
);

CREATE TABLE purchase_bill_lines (
    id BIGINT PRIMARY KEY,
    purchase_bill_id BIGINT NOT NULL REFERENCES purchase_bills(id),
    product_id BIGINT NOT NULL REFERENCES products(id)
);

CREATE TABLE transactions (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vendor_id BIGINT REFERENCES vendors(id)
);

CREATE TABLE transaction_lines (
    id BIGINT PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    item_id BIGINT REFERENCES products(id),
    account_id BIGINT REFERENCES ledgers(id)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    source_ledger_id BIGINT REFERENCES ledgers(id)
);

CREATE TABLE payment_allocations (
    id BIGINT PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    transaction_id BIGINT NOT NULL REFERENCES transactions(id)
);