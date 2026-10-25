import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Plus, Trash2 } from "lucide-react";
import { CustomSelect } from "@/components/ui/CustomSelect";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { Spinner } from "@/components/ui/Spinner";
import { ApiError } from "@/lib/apiClient";
import { accountingService, type Ledger } from "@/services/accountingService";
import {
  transactionService,
  type Transaction,
  type TransactionPayload,
} from "@/services/transactionService";
import { vendorService, type Vendor } from "@/services/vendorService";
import "./TransactionFormModal.css";

type LineFormState = {
  id: string;
  accountId: string;
  rate: string;
  discount: string;
  taxRate: string;
};

type TransactionFormState = {
  transactionDate: string;
  vendorId: string;
  referenceNo: string;
  paymentDate: string;
  paymentLedgerId: string;
  roundOff: string;
  notes: string;
  attachments: string;
};

type TransactionFormErrors = Partial<
  Record<keyof TransactionFormState | "lines", string>
>;

export type TransactionFormProps = {
  transaction?: Transaction;
  onCancel: () => void;
  onSaved?: (transaction: Transaction) => void;
  /** Hide the internal header buttons when the page renders its own FormActionBar. */
  hideHeaderActions?: boolean;
};

const MONEY_PATTERN = /^-?\d+(?:\.\d{1,2})?$/;
const POSITIVE_MONEY_PATTERN = /^\d+(?:\.\d{1,2})?$/;

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

function lineId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random()}`;
}

function newLine(): LineFormState {
  return {
    id: lineId(),
    accountId: "",
    rate: "0.00",
    discount: "0.00",
    taxRate: "0",
  };
}

function createFormState(transaction?: Transaction): TransactionFormState {
  const transactionDate = transaction?.transactionDate ?? today();
  return {
    transactionDate,
    vendorId: transaction?.vendorId ? String(transaction.vendorId) : "",
    referenceNo: transaction?.referenceNo ?? "",
    paymentDate: transaction?.paymentDate ?? transactionDate,
    paymentLedgerId: transaction?.paymentLedgerId
      ? String(transaction.paymentLedgerId)
      : "",
    roundOff: String(transaction?.roundOff ?? 0),
    notes: transaction?.notes ?? "",
    attachments: transaction?.attachments ?? "",
  };
}

function createLineState(transaction?: Transaction): LineFormState[] {
  if (!transaction?.lines?.length) return [newLine()];
  return transaction.lines.map((line) => ({
    id: String(line.id ?? lineId()),
    accountId: line.accountId ? String(line.accountId) : "",
    rate: String(line.rate),
    discount: String(line.discount),
    taxRate: String(line.taxRate),
  }));
}

function optionalValue(value: string): string | null {
  return value.trim() || null;
}

function optionalNumber(value: string): number | null {
  return value.trim() ? Number(value) : null;
}

function amount(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function getLineAmounts(line: LineFormState) {
  const rate = amount(line.rate);
  const discount = amount(line.discount);
  const base = Math.max(rate - discount, 0);
  const taxAmount = roundMoney((base * amount(line.taxRate)) / 100);
  return {
    gross: roundMoney(rate),
    discount: roundMoney(discount),
    taxAmount,
    amount: roundMoney(base + taxAmount),
  };
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function validateForm(
  form: TransactionFormState,
  lines: LineFormState[],
): TransactionFormErrors {
  const errors: TransactionFormErrors = {};
  if (!form.transactionDate) errors.transactionDate = "This field is required.";
  if (!form.paymentDate) errors.paymentDate = "Payment date is required.";
  if (!form.paymentLedgerId)
    errors.paymentLedgerId = "Select the cash or bank account used.";
  if (form.roundOff.trim() && !MONEY_PATTERN.test(form.roundOff.trim())) {
    errors.roundOff = "Enter a valid amount.";
  }
  if (lines.length === 0) errors.lines = "Add at least one line.";

  const invalidLine = lines.find((line) => {
    return (
      !line.accountId ||
      !POSITIVE_MONEY_PATTERN.test(line.rate.trim()) ||
      !POSITIVE_MONEY_PATTERN.test(line.discount.trim()) ||
      (line.taxRate.trim() && !POSITIVE_MONEY_PATTERN.test(line.taxRate.trim()))
    );
  });

  if (invalidLine) {
    errors.lines =
      "Each line needs an account, a valid rate and discount, and a valid tax rate.";
  }

  return errors;
}

function toPayload(
  form: TransactionFormState,
  lines: LineFormState[],
  totals: {
    subtotal: number;
    discountAmount: number;
    taxAmount: number;
    totalAmount: number;
  },
): TransactionPayload {
  return {
    transactionDate: form.transactionDate,
    transactionType: "EXPENSE",
    billNo: null,
    vendorId: optionalNumber(form.vendorId),
    dueDate: null,
    referenceNo: optionalValue(form.referenceNo),
    subtotal: totals.subtotal,
    discountAmount: totals.discountAmount,
    taxAmount: totals.taxAmount,
    roundOff: amount(form.roundOff),
    totalAmount: totals.totalAmount,
    paymentDate: optionalValue(form.paymentDate),
    paymentLedgerId: optionalNumber(form.paymentLedgerId),
    paymentStatus: "PAID",
    notes: optionalValue(form.notes),
    attachments: optionalValue(form.attachments),
    lines: lines.map((line, index) => {
      const lineAmounts = getLineAmounts(line);
      return {
        lineNo: index + 1,
        lineType: "ACCOUNT",
        itemId: null,
        accountId: optionalNumber(line.accountId),
        description: null,
        quantity: 1,
        unit: null,
        rate: amount(line.rate),
        discount: amount(line.discount),
        taxRate: amount(line.taxRate),
        taxAmount: lineAmounts.taxAmount,
        amount: lineAmounts.amount,
      };
    }),
  };
}

export function TransactionForm({
  transaction,
  onCancel,
  onSaved,
  hideHeaderActions = false,
}: TransactionFormProps) {
  const messagePopup = useMessagePopup();
  const [form, setForm] = useState<TransactionFormState>(() =>
    createFormState(transaction),
  );
  const [lines, setLines] = useState<LineFormState[]>(() =>
    createLineState(transaction),
  );
  const [errors, setErrors] = useState<TransactionFormErrors>({});
  const [saving, setSaving] = useState(false);
  const [loadingLookups, setLoadingLookups] = useState(true);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [ledgers, setLedgers] = useState<Ledger[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    async function loadLookups() {
      setLoadingLookups(true);
      const [vendorResult, ledgerResult] = await Promise.allSettled([
        vendorService.list(controller.signal),
        accountingService.listLedgers(controller.signal),
      ]);
      if (controller.signal.aborted) return;

      if (vendorResult.status === "fulfilled") {
        setVendors(vendorResult.value);
      } else {
        messagePopup.error(
          getErrorMessage(vendorResult.reason, "Failed to load vendors."),
        );
      }

      if (ledgerResult.status === "fulfilled") {
        setLedgers(ledgerResult.value.filter((ledger) => ledger.active));
      } else {
        messagePopup.error(
          getErrorMessage(
            ledgerResult.reason,
            "Failed to load Chart of Accounts.",
          ),
        );
      }

      if (!controller.signal.aborted) {
        setLoadingLookups(false);
      }
    }

    void loadLookups();
    return () => controller.abort();
  }, [messagePopup]);

  const totals = useMemo(() => {
    const subtotal = roundMoney(
      lines.reduce((sum, line) => sum + getLineAmounts(line).gross, 0),
    );
    const discountAmount = roundMoney(
      lines.reduce((sum, line) => sum + getLineAmounts(line).discount, 0),
    );
    const taxAmount = roundMoney(
      lines.reduce((sum, line) => sum + getLineAmounts(line).taxAmount, 0),
    );
    const totalAmount = roundMoney(
      subtotal - discountAmount + taxAmount + amount(form.roundOff),
    );
    return { subtotal, discountAmount, taxAmount, totalAmount };
  }, [form.roundOff, lines]);

  const vendorOptions = useMemo(
    () =>
      vendors.map((vendor) => ({
        value: String(vendor.id),
        label: vendor.displayName,
      })),
    [vendors],
  );
  const ledgerOptions = useMemo(
    () =>
      ledgers
        .filter((ledger) => ledger.postingAllowed !== false)
        .map((ledger) => ({
          value: String(ledger.id),
          label: `${ledger.name} - ${ledger.accountTypeName}`,
        })),
    [ledgers],
  );
  const paymentLedgerOptions = useMemo(
    () =>
      ledgers
        .filter(
          (ledger) =>
            ledger.postingAllowed !== false &&
            (ledger.ledgerType === "CASH" || ledger.ledgerType === "BANK"),
        )
        .map((ledger) => ({
          value: String(ledger.id),
          label: `${ledger.name} - ${ledger.accountTypeName}`,
        })),
    [ledgers],
  );

  function handleChange(
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) {
    const { name, value } = event.target;
    setForm((current) => {
      const next = { ...current, [name]: value };
      return next;
    });
    setErrors((current) => {
      if (!current[name as keyof TransactionFormState]) return current;
      const next = { ...current };
      delete next[name as keyof TransactionFormState];
      return next;
    });
  }

  function handleSelect(name: keyof TransactionFormState, value: string) {
    setForm((current) => {
      return { ...current, [name]: value };
    });
    setErrors((current) => {
      const next = { ...current };
      delete next[name];
      return next;
    });
  }

  function updateLine(id: string, changes: Partial<LineFormState>) {
    setLines((current) =>
      current.map((line) => {
        if (line.id !== id) return line;
        return { ...line, ...changes };
      }),
    );
    setErrors((current) => {
      if (!current.lines) return current;
      const next = { ...current };
      delete next.lines;
      return next;
    });
  }

  function removeLine(id: string) {
    setLines((current) =>
      current.length === 1 ? current : current.filter((line) => line.id !== id),
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    const validationErrors = validateForm(form, lines);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setSaving(true);
    try {
      const payload = toPayload(form, lines, totals);
      const saved = transaction
        ? await transactionService.update(transaction.id, payload)
        : await transactionService.create(payload);
      messagePopup.success(
        transaction
          ? "Expense updated successfully."
          : "Expense saved and posted successfully.",
      );
      onSaved?.(saved);
    } catch (error) {
      messagePopup.error(getErrorMessage(error, "Failed to save transaction."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="transaction-form-page__panel">
      <div className="transaction-form-page__header">
        <div>
          <h2>{transaction ? "Edit Expense" : "New Expense"}</h2>
        </div>
        {!hideHeaderActions && (
          <div className="transaction-form-page__actions">
            <button
              type="button"
              className="transaction-form__button transaction-form__button--secondary"
              onClick={onCancel}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              form="transaction-form"
              className="transaction-form__button transaction-form__button--primary"
              disabled={saving || loadingLookups}
            >
              {saving ? (
                <>
                  <Spinner white size={14} />
                  Saving...
                </>
              ) : (
                "Save & Post"
              )}
            </button>
          </div>
        )}
      </div>
      <form
        id="transaction-form"
        className="transaction-form"
        onSubmit={handleSubmit}
        noValidate
      >
        {loadingLookups && (
          <div className="transaction-form__loading">
            <Spinner size={18} />
            <span>Loading form options...</span>
          </div>
        )}

        <section className="transaction-form__section">
          <h3>Expense Details</h3>
          <div className="transaction-form__grid">
            <label
              className={`transaction-form__field transaction-form__field--required${errors.transactionDate ? " transaction-form__field--invalid" : ""}`}
            >
              <span>Date</span>
              <input
                type="date"
                name="transactionDate"
                value={form.transactionDate}
                onChange={handleChange}
                autoFocus
                required
              />
              {errors.transactionDate && (
                <small className="transaction-form__error">
                  {errors.transactionDate}
                </small>
              )}
            </label>

            <label className="transaction-form__field">
              <span>Vendor / Payee</span>
              <CustomSelect
                name="vendorId"
                value={form.vendorId}
                onChange={(value) => handleSelect("vendorId", value)}
                options={[{ value: "", label: "No vendor" }, ...vendorOptions]}
                disabled={loadingLookups}
              />
            </label>

            <label className="transaction-form__field">
              <span>Reference No</span>
              <input
                name="referenceNo"
                value={form.referenceNo}
                onChange={handleChange}
                maxLength={100}
              />
            </label>

            <label
              className={`transaction-form__field transaction-form__field--required${errors.paymentDate ? " transaction-form__field--invalid" : ""}`}
            >
              <span>Payment Date</span>
              <input
                type="date"
                name="paymentDate"
                value={form.paymentDate}
                onChange={handleChange}
                required
              />
              {errors.paymentDate && (
                <small className="transaction-form__error">
                  {errors.paymentDate}
                </small>
              )}
            </label>

            <label
              className={`transaction-form__field transaction-form__field--required${errors.paymentLedgerId ? " transaction-form__field--invalid" : ""}`}
            >
              <span>Paid From (Chart of Accounts)</span>
              <CustomSelect
                name="paymentLedgerId"
                value={form.paymentLedgerId}
                onChange={(value) => handleSelect("paymentLedgerId", value)}
                options={[
                  { value: "", label: "Select cash or bank account" },
                  ...(paymentLedgerOptions.length > 0
                    ? paymentLedgerOptions
                    : [
                        {
                          value: "",
                          label: "No cash or bank accounts found",
                          disabled: true,
                        },
                      ]),
                ]}
                disabled={loadingLookups}
              />
              {errors.paymentLedgerId && (
                <small className="transaction-form__error">
                  {errors.paymentLedgerId}
                </small>
              )}
            </label>

            <label
              className={`transaction-form__field${errors.roundOff ? " transaction-form__field--invalid" : ""}`}
            >
              <span>Round Off</span>
              <input
                name="roundOff"
                value={form.roundOff}
                onChange={handleChange}
                inputMode="decimal"
              />
              {errors.roundOff && (
                <small className="transaction-form__error">
                  {errors.roundOff}
                </small>
              )}
            </label>
          </div>
        </section>

        <section className="transaction-form__section">
          <div className="transaction-form__section-heading">
            <h3>Lines</h3>
            <button
              type="button"
              className="transaction-form__add-line"
              onClick={() => setLines((current) => [...current, newLine()])}
            >
              <Plus size={15} />
              Add Line
            </button>
          </div>
          {errors.lines && (
            <small className="transaction-form__error">{errors.lines}</small>
          )}

          <div className="transaction-form__lines transaction-form__lines-scroll transaction-form__lines-scroll--expense">
            {lines.length > 0 && (
              <div className="transaction-form__line-header">
                <div className="transaction-form__line-header-grid">
                  {/* <span>Line Type</span> */}
                  <span>Account</span>
                  <span>Rate</span>
                  <span>Discount</span>
                  <span>Tax %</span>
                  <span>Amount</span>
                  <span></span>
                </div>
              </div>
            )}
            {lines.map((line, index) => {
              const lineAmounts = getLineAmounts(line);
              return (
                <div className="transaction-form__line" key={line.id}>
                  <div className="transaction-form__line-grid">
                    <div className="transaction-form__field transaction-form__line-target transaction-form__field--no-label">
                      <CustomSelect
                        value={line.accountId}
                        onChange={(value) =>
                          updateLine(line.id, { accountId: value })
                        }
                        options={
                          ledgerOptions.length > 0
                            ? ledgerOptions
                            : [
                                {
                                  value: "",
                                  label: "No postable Chart of Accounts found",
                                  disabled: true,
                                },
                              ]
                        }
                        placeholder="Select account from Chart of Accounts"
                        disabled={loadingLookups}
                      />
                    </div>

                    <label className="transaction-form__field transaction-form__line-rate transaction-form__field--no-label">
                      <input
                        value={line.rate}
                        onChange={(event) =>
                          updateLine(line.id, { rate: event.target.value })
                        }
                        inputMode="decimal"
                      />
                    </label>

                    <label className="transaction-form__field transaction-form__line-discount transaction-form__field--no-label">
                      <input
                        value={line.discount}
                        onChange={(event) =>
                          updateLine(line.id, { discount: event.target.value })
                        }
                        inputMode="decimal"
                      />
                    </label>

                    <label className="transaction-form__field transaction-form__line-tax transaction-form__field--no-label">
                      <input
                        value={line.taxRate}
                        onChange={(event) =>
                          updateLine(line.id, { taxRate: event.target.value })
                        }
                        inputMode="decimal"
                      />
                    </label>

                    <div className="transaction-form__line-amount transaction-form__field--no-label">
                      <strong>{formatMoney(lineAmounts.amount)}</strong>
                    </div>

                    <button
                      type="button"
                      className="transaction-form__icon-button transaction-form__line-delete"
                      onClick={() => removeLine(line.id)}
                      disabled={lines.length === 1}
                      title="Remove line"
                      aria-label={`Remove line ${index + 1}`}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="transaction-form__summary">
          <div>
            <span>Subtotal</span>
            <strong>{formatMoney(totals.subtotal)}</strong>
          </div>
          <div>
            <span>Discount</span>
            <strong>{formatMoney(totals.discountAmount)}</strong>
          </div>
          <div>
            <span>Tax</span>
            <strong>{formatMoney(totals.taxAmount)}</strong>
          </div>
          <div className="transaction-form__summary-total">
            <span>Total</span>
            <strong>{formatMoney(totals.totalAmount)}</strong>
          </div>
        </section>

        <section className="transaction-form__section">
          <h3>Other</h3>
          <div className="transaction-form__grid">
            <label className="transaction-form__field transaction-form__field--wide">
              <span>Notes</span>
              <textarea
                name="notes"
                value={form.notes}
                onChange={handleChange}
                maxLength={4000}
                rows={4}
              />
            </label>
            <label className="transaction-form__field transaction-form__field--wide">
              <span>Attachments</span>
              <textarea
                name="attachments"
                value={form.attachments}
                onChange={handleChange}
                maxLength={4000}
                rows={3}
              />
            </label>
          </div>
        </section>
      </form>
    </div>
  );
}
