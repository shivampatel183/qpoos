import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { FormActionBar } from "@/components/ui/FormActionBar";
import { CustomSelect } from "@/components/ui/CustomSelect";
import {
  DocumentItemLines,
  type DocumentItemLine,
} from "@/components/transactions/DocumentItemLines";
import { Spinner } from "@/components/ui/Spinner";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import { productService, type Product } from "@/services/productService";
import { vendorService, type Vendor } from "@/services/vendorService";
import {
  paymentTermService,
  purchaseOrderService,
  type PaymentTerm,
  type PurchaseOrder,
} from "@/services/purchaseOrderService";
import "./PurchaseOrderPage.css";

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

export function PurchaseOrderFormPage() {
  const navigate = useNavigate();
  const { orderId } = useParams();
  const popup = useMessagePopup();
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [terms, setTerms] = useState<PaymentTerm[]>([]);
  const [order, setOrder] = useState<PurchaseOrder | null>(null);
  const [vendorId, setVendorId] = useState("");
  const [paymentTermId, setPaymentTermId] = useState("");
  const [orderDate, setOrderDate] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [deliveryTerm, setDeliveryTerm] = useState("");
  const [expectedDate, setExpectedDate] = useState("");
  const [address, setAddress] = useState({
    line1: "",
    line2: "",
    city: "",
    state: "",
    country: "",
    postalCode: "",
  });
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
      orderId
        ? purchaseOrderService.get(Number(orderId), controller.signal)
        : Promise.resolve(null),
    ])
      .then(([vendorList, productList, termList, loadedOrder]) => {
        setVendors(vendorList);
        setProducts(productList);
        setTerms(termList);
        if (loadedOrder) {
          setOrder(loadedOrder);
          setVendorId(String(loadedOrder.vendorId));
          setPaymentTermId(
            loadedOrder.paymentTermId ? String(loadedOrder.paymentTermId) : "",
          );
          setOrderDate(loadedOrder.orderDate);
          setDeliveryTerm(loadedOrder.deliveryTerm ?? "");
          setExpectedDate(loadedOrder.expectedDeliveryDate ?? "");
          setAddress({
            line1: loadedOrder.deliveryAddress?.line1 ?? "",
            line2: loadedOrder.deliveryAddress?.line2 ?? "",
            city: loadedOrder.deliveryAddress?.city ?? "",
            state: loadedOrder.deliveryAddress?.state ?? "",
            country: loadedOrder.deliveryAddress?.country ?? "",
            postalCode: loadedOrder.deliveryAddress?.postalCode ?? "",
          });
          setNotes(loadedOrder.notes ?? "");
          setLines(
            loadedOrder.lines.map((line) => ({
              id: String(line.id),
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
          popup.error(getError(error, "Failed to load purchase order."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [orderId, popup]);

  function updateLine(id: string, changes: Partial<DocumentItemLine>) {
    setLines((current) =>
      current.map((line) => (line.id === id ? { ...line, ...changes } : line)),
    );
  }

  function payload() {
    return {
      orderDate,
      vendorId: Number(vendorId),
      paymentTermId: paymentTermId ? Number(paymentTermId) : null,
      deliveryTerm: deliveryTerm.trim() || null,
      expectedDeliveryDate: expectedDate || null,
      deliveryAddress: Object.values(address).some(Boolean) ? address : null,
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
      const saved = order
        ? await purchaseOrderService.update(order.id, payload())
        : await purchaseOrderService.create(payload());
      popup.success("Purchase order saved.");
      navigate(`/purchase-orders/${saved.id}`);
    } catch (error) {
      popup.error(getError(error, "Failed to save purchase order."));
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(action: "submit" | "approve" | "cancel") {
    if (!order) return;
    setSaving(true);
    try {
      const updated = await purchaseOrderService[action](order.id);
      setOrder(updated);
      popup.success(`Purchase order ${action}ed.`);
    } catch (error) {
      popup.error(getError(error, "Failed to update purchase order status."));
    } finally {
      setSaving(false);
    }
  }

  if (loading)
    return (
      <div className="purchase-order-page__loading">
        <Spinner size={22} /> Loading purchase order...
      </div>
    );
  return (
    <div className="purchase-order-page">
      <FormActionBar
        backLabel="Back to purchase orders"
        onBack={() => navigate("/purchase-orders")}
        onCancel={() => navigate("/purchase-orders")}
        formId="purchase-order-form"
        saving={saving}
        saveDisabled={!!order && order.status !== "DRAFT"}
      />
      <form
        id="purchase-order-form"
        className="purchase-order-form"
        onSubmit={submit}
      >
        <header>
          <h1>
            {order ? `Edit ${order.orderNumber}` : "New Purchase Order"}
          </h1>
          <div className="purchase-order-form__actions">
            {order?.status === "DRAFT" && (
              <button
                type="button"
                onClick={() => changeStatus("submit")}
                disabled={saving}
              >
                Submit
              </button>
            )}
            {order?.status === "SENT" && (
              <button
                type="button"
                className="btn-success"
                onClick={() => changeStatus("approve")}
                disabled={saving}
              >
                Approve
              </button>
            )}
            {order && ["DRAFT", "SENT", "APPROVED"].includes(order.status) && (
              <button
                type="button"
                className="btn-danger"
                onClick={() => changeStatus("cancel")}
                disabled={saving}
              >
                Cancel Order
              </button>
            )}
          </div>
        </header>
        <section className="purchase-order-form__section purchase-order-form__section--details">
          <h2>Order Details</h2>
          <div className="purchase-order-form__grid">
            <label>
              Date
              <input
                type="date"
                value={orderDate}
                onChange={(event) => setOrderDate(event.target.value)}
                disabled={!!order && order.status !== "DRAFT"}
                required
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
                disabled={!!order && order.status !== "DRAFT"}
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
                disabled={!!order && order.status !== "DRAFT"}
              />
            </label>
            <label>
              Expected Delivery
              <input
                type="date"
                value={expectedDate}
                onChange={(event) => setExpectedDate(event.target.value)}
                disabled={!!order && order.status !== "DRAFT"}
              />
            </label>
            <label>
              Delivery Term
              <input
                value={deliveryTerm}
                onChange={(event) => setDeliveryTerm(event.target.value)}
                maxLength={120}
                disabled={!!order && order.status !== "DRAFT"}
              />
            </label>
          </div>
        </section>
        <section className="purchase-order-form__section purchase-order-form__section--address">
          <h2>Delivery Address</h2>
          <div className="purchase-order-form__grid">
            {(
              [
                "line1",
                "line2",
                "city",
                "state",
                "country",
                "postalCode",
              ] as const
            ).map((field) => (
              <label key={field}>
                {field === "line1"
                  ? "Address line 1"
                  : field === "line2"
                    ? "Address line 2"
                    : field === "postalCode"
                      ? "Postal code"
                      : field.charAt(0).toUpperCase() + field.slice(1)}
                <input
                  value={address[field]}
                  onChange={(event) =>
                    setAddress((current) => ({
                      ...current,
                      [field]: event.target.value,
                    }))
                  }
                  disabled={!!order && order.status !== "DRAFT"}
                />
              </label>
            ))}
          </div>
        </section>
        <DocumentItemLines
          size="m"
          lines={lines}
          products={products}
          loading={loading}
          onAdd={() => setLines((current) => [...current, emptyLine()])}
          onChange={updateLine}
          onRemove={(id) =>
            setLines((current) =>
              current.length === 1
                ? current
                : current.filter((line) => line.id !== id),
            )
          }
          readOnly={!!order && order.status !== "DRAFT"}
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
          <label className="purchase-order-form__notes">
            Notes
            <textarea
              rows={4}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              disabled={!!order && order.status !== "DRAFT"}
            />
          </label>
        </section>
      </form>
    </div>
  );
}
