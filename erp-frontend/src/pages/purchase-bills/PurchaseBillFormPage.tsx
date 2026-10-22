import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { FormActionBar } from "@/components/ui/FormActionBar";
import { CustomSelect } from "@/components/ui/CustomSelect";
import {
  DocumentItemLines,
  type DocumentItemLine,
} from "@/components/transactions/DocumentItemLines/DocumentItemLines";
import { Spinner } from "@/components/ui/Spinner";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import { productService, type Product } from "@/services/productService";
import { vendorService, type Vendor } from "@/services/vendorService";
import {
  paymentTermService,
  purchaseBillService,
  type PaymentTerm,
  type PurchaseBill,
} from "@/services/purchaseBillService";
import "../purchase-orders/PurchaseOrderPage.css";

function getError(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function emptyLine(): DocumentItemLine {
  return {
    id: crypto.randomUUID(),
    productId: "",
    description: "",
    quantity: "1",
    unit: "",
    rate: "0",
    discount: "0",
    taxRate: "0",
  };
}

function amount(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function money(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function PurchaseBillFormPage() {
  const navigate = useNavigate();
  const { billId } = useParams();
  const popup = useMessagePopup();
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [terms, setTerms] = useState<PaymentTerm[]>([]);
  const [bill, setBill] = useState<PurchaseBill | null>(null);
  const [vendorId, setVendorId] = useState("");
  const [paymentTermId, setPaymentTermId] = useState("");
  const [billDate, setBillDate] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [dueDate, setDueDate] = useState("");
  const [notes, setNotes] = useState("");
  const [lines, setLines] = useState<DocumentItemLine[]>([emptyLine()]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const totals = useMemo(() => {
    const subtotal = roundMoney(
      lines.reduce(
        (sum, line) => sum + amount(line.quantity) * amount(line.rate),
        0,
      ),
    );
    const discountAmount = roundMoney(
      lines.reduce((sum, line) => sum + amount(line.discount), 0),
    );
    const taxAmount = roundMoney(
      lines.reduce((sum, line) => {
        const taxable = Math.max(
          amount(line.quantity) * amount(line.rate) - amount(line.discount),
          0,
        );
        return sum + (taxable * amount(line.taxRate)) / 100;
      }, 0),
    );
    return {
      subtotal,
      discountAmount,
      taxAmount,
      totalAmount: roundMoney(subtotal - discountAmount + taxAmount),
    };
  }, [lines]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      vendorService.list(controller.signal),
      productService.list(controller.signal),
      paymentTermService.list(controller.signal),
      billId
        ? purchaseBillService.get(Number(billId), controller.signal)
        : Promise.resolve(null),
    ])
      .then(([vendorList, productList, termList, loadedBill]) => {
        setVendors(vendorList);
        setProducts(productList);
        setTerms(termList);
        if (loadedBill) {
          setBill(loadedBill);
          setVendorId(String(loadedBill.vendorId));
          setPaymentTermId(
            loadedBill.paymentTermId ? String(loadedBill.paymentTermId) : "",
          );
          setBillDate(loadedBill.billDate);
          setDueDate(loadedBill.dueDate ?? "");
          setNotes(loadedBill.notes ?? "");
          setLines(
            loadedBill.lines.map((line) => ({
              id: String(line.id ?? crypto.randomUUID()),
              productId: String(line.productId),
              description: line.description ?? "",
              quantity: String(line.quantity),
              unit: line.unit ?? "",
              rate: String(line.rate),
              discount: String(line.discount),
              taxRate: String(line.taxRate),
            })),
          );
        }
      })
      .catch((error) => {
        if (!controller.signal.aborted)
          popup.error(getError(error, "Failed to load purchase bill."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [billId, popup]);

  function updateLine(id: string, changes: Partial<DocumentItemLine>) {
    setLines((current) =>
      current.map((line) => (line.id === id ? { ...line, ...changes } : line)),
    );
  }

  function payload() {
    return {
      billDate,
      vendorId: Number(vendorId),
      paymentTermId: paymentTermId ? Number(paymentTermId) : null,
      dueDate: dueDate || null,
      purchaseOrderId: null,
      roundOff: 0,
      notes: notes.trim() || null,
      attachments: null,
      lines: lines.map((line, index) => ({
        lineNo: index + 1,
        productId: Number(line.productId),
        description: line.description || null,
        quantity: Number(line.quantity),
        unit: line.unit || null,
        rate: Number(line.rate),
        discount: Number(line.discount),
        taxRate: Number(line.taxRate),
      })),
    };
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (
      !vendorId ||
      lines.some((line) => !line.productId || Number(line.quantity) <= 0)
    ) {
      popup.error("Select a vendor and a valid product line.");
      return;
    }
    setSaving(true);
    try {
      const saved = bill
        ? await purchaseBillService.update(bill.id, payload())
        : await purchaseBillService.create(payload());
      popup.success("Purchase bill saved.");
      navigate(`/purchase-bills/${saved.id}`);
    } catch (error) {
      popup.error(getError(error, "Failed to save purchase bill."));
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(action: "post" | "cancel") {
    if (!bill) return;
    setSaving(true);
    try {
      const updated = await purchaseBillService[action](bill.id);
      setBill(updated);
      popup.success(`Purchase bill ${action}ed.`);
    } catch (error) {
      popup.error(getError(error, "Failed to update purchase bill status."));
    } finally {
      setSaving(false);
    }
  }

  if (loading)
    return (
      <div className="purchase-order-page__loading">
        <Spinner size={22} /> Loading purchase bill...
      </div>
    );

  return (
    <div className="purchase-order-page">
      <FormActionBar
        backLabel="Back to purchase bills"
        onBack={() => navigate("/purchase-bills")}
        onCancel={() => navigate("/purchase-bills")}
        formId="purchase-bill-form"
        saveLabel={bill?.status === "DRAFT" ? "Save Draft" : "Save"}
        saving={saving}
        saveDisabled={!!bill && bill.status !== "DRAFT"}
      />
      <form
        id="purchase-bill-form"
        className="purchase-order-form"
        onSubmit={submit}
      >
        <header>
          <h1>{bill ? `Edit ${bill.billNumber}` : "New Purchase Bill"}</h1>
          <div className="purchase-order-form__actions">
            {bill?.status === "DRAFT" && (
              <button
                type="button"
                className="btn-success"
                onClick={() => changeStatus("post")}
                disabled={saving}
              >
                Post
              </button>
            )}
            {bill?.status === "DRAFT" && (
              <button
                type="button"
                className="btn-danger"
                onClick={() => changeStatus("cancel")}
                disabled={saving}
              >
                Cancel Bill
              </button>
            )}
          </div>
        </header>

        <section className="purchase-order-form__section purchase-order-form__section--details">
          <h2>Bill Details</h2>
          <div className="purchase-order-form__grid">
            <label>
              Bill Date
              <input
                type="date"
                value={billDate}
                onChange={(event) => setBillDate(event.target.value)}
                required
                disabled={!!bill && bill.status !== "DRAFT"}
              />
            </label>
            <label>
              Vendor
              <CustomSelect
                value={vendorId}
                onChange={setVendorId}
                options={vendors.map((vendor) => ({
                  value: String(vendor.id),
                  label: vendor.displayName,
                }))}
                placeholder="Select vendor"
                disabled={!!bill && bill.status !== "DRAFT"}
              />
            </label>
            <label>
              Payment Term
              <CustomSelect
                value={paymentTermId}
                onChange={setPaymentTermId}
                options={[
                  { value: "", label: "No term" },
                  ...terms.map((term) => ({
                    value: String(term.id),
                    label: `${term.name} (${term.dueDays} days)`,
                  })),
                ]}
                disabled={!!bill && bill.status !== "DRAFT"}
              />
            </label>
            <label>
              Due Date
              <input
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
                disabled={!!bill && bill.status !== "DRAFT"}
              />
            </label>
          </div>
        </section>

        <DocumentItemLines
          lines={lines}
          products={products}
          onAdd={() => setLines((current) => [...current, emptyLine()])}
          onChange={updateLine}
          onRemove={(id) =>
            setLines((current) =>
              current.length === 1
                ? current
                : current.filter((line) => line.id !== id),
            )
          }
          readOnly={!!bill && bill.status !== "DRAFT"}
        />

        <section className="purchase-order-form__summary">
          <div>
            <span>Subtotal</span>
            <strong>{money(totals.subtotal)}</strong>
          </div>
          <div>
            <span>Discount</span>
            <strong>{money(totals.discountAmount)}</strong>
          </div>
          <div>
            <span>Tax</span>
            <strong>{money(totals.taxAmount)}</strong>
          </div>
          <div className="purchase-order-form__summary-total">
            <span>Total</span>
            <strong>{money(totals.totalAmount)}</strong>
          </div>
        </section>

        <section className="purchase-order-form__section purchase-order-form__section--notes">
          <h2>Notes</h2>
          <textarea
            className="purchase-order-form__notes"
            rows={4}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Add notes"
            disabled={!!bill && bill.status !== "DRAFT"}
          />
        </section>
      </form>
    </div>
  );
}
