import { useCallback, useEffect, useMemo, useState } from "react";
import { Plus } from "lucide-react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { ApiError } from "@/lib/apiClient";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn, SortState } from "@/components/ui/DataTable";
import { useCompany } from "@/context/CompanyContext";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { accountingService, type Ledger } from "@/services/accountingService";
import { vendorService, type Vendor } from "@/services/vendorService";
import { paymentService, type Payment, type PaymentSummary, type PayableDocument, type PaymentMethod } from "@/services/paymentService";
import "../purchase-orders/PurchaseOrderPage.css";
import "./payments.css";

const currency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(value);

export function PaymentListPage() {
  const navigate = useNavigate();
  const { currentCompany } = useCompany();
  const [payments, setPayments] = useState<PaymentSummary[]>([]);
  const [page, setPage] = useState(1);
  const [totalRows, setTotalRows] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<SortState>({ key: "paymentDate", direction: "desc" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const load = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError("");
    try {
      const sortKey = sort.key === "vendorName" ? "vendor.displayName" : sort.key;
      const result = await paymentService.list(page - 1, search, `${sortKey},${sort.direction}`, signal);
      setPayments(result.content);
      setTotalRows(result.totalElements);
    } catch (e) {
      if (!signal?.aborted) setError(e instanceof ApiError ? e.message : "Could not load payments.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [page, search, sort]);
  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [currentCompany?.id, load]);

  const columns = useMemo<DataTableColumn<PaymentSummary>[]>(() => [
    { key: "paymentNumber", header: "Payment", width: "155px", render: (payment) => <div className="payment-table__identity"><strong>{payment.paymentNumber}</strong><small>{payment.paymentMethod}</small></div> },
    { key: "paymentDate", header: "Date" },
    { key: "vendorName", header: "Vendor" },
    { key: "allocationCount", header: "Documents", align: "center", sortable: false, render: (payment) => `${payment.allocationCount} document${payment.allocationCount === 1 ? "" : "s"}` },
    { key: "amount", header: "Amount", align: "right", render: (payment) => currency(payment.amount) },
    { key: "status", header: "Status", align: "center", render: (payment) => <span className={`payments-status payments-status--${payment.status.toLowerCase()}`}>{payment.status}</span> },
  ], []);

  return <div className="purchase-order-page payments-page">
    <DataTable
      data={payments}
      columns={columns}
      rowKey="id"
      title="Payments"
      subtitle="Supplier payments and allocations to bills and payable expenses."
      loading={loading}
      error={error || null}
      onRetry={() => void load()}
      searchable
      manualSearch
      searchPlaceholder="Search payment number, vendor or reference..."
      onSearchChange={(value) => { setPage(1); setSearch(value); }}
      manualPagination
      totalRows={totalRows}
      page={page}
      pageSize={50}
      pageSizeOptions={[50]}
      onPageChange={setPage}
      manualSorting
      defaultSort={{ key: "paymentDate", direction: "desc" }}
      onSortChange={setSort}
      density="compact"
      onRowClick={(payment) => navigate(`/payments/${payment.id}`)}
      primaryAction={{ label: "New Payment", icon: Plus, onClick: () => navigate("/payments/new") }}
      emptyTitle="No payments yet"
      emptyMessage="Create a payment to settle one or more bills or payable expenses."
      emptyAction={{ label: "Create payment", icon: Plus, onClick: () => navigate("/payments/new") }}
    />
  </div>;
}

export function PaymentFormPage() {
  const navigate = useNavigate();
  const { paymentId } = useParams();
  const popup = useMessagePopup();
  const [search] = useSearchParams();
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [bills, setBills] = useState<PayableDocument[]>([]);
  const [vendorId, setVendorId] = useState(search.get("vendorId") ?? "");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [method, setMethod] = useState<PaymentMethod>("BANK");
  const [ledgerId, setLedgerId] = useState("");
  const [reference, setReference] = useState("");
  const [notes, setNotes] = useState("");
  const [amounts, setAmounts] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const presetBill = Number(search.get("billId")) || 0;
  const total = useMemo(() => Object.values(amounts).reduce((sum, value) => sum + (Number(value) || 0), 0), [amounts]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([vendorService.list(controller.signal), accountingService.listLedgers(controller.signal)])
      .then(([v, l]) => { setVendors(v); setLedgers(l.filter((x) => x.active && x.postingAllowed && (x.ledgerType === "CASH" || x.ledgerType === "BANK"))); })
      .catch((e) => { if (!controller.signal.aborted) setError(e instanceof ApiError ? e.message : "Could not load payment setup."); });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!paymentId) return;
    const controller = new AbortController();
    paymentService.get(Number(paymentId), controller.signal).then((payment) => {
      if (payment.status !== "DRAFT") { navigate(`/payments/${payment.id}`, { replace: true }); return; }
      setVendorId(String(payment.vendorId));
      setDate(payment.paymentDate);
      setMethod(payment.paymentMethod);
      setLedgerId(payment.sourceLedgerId == null ? "" : String(payment.sourceLedgerId));
      setReference(payment.referenceNumber ?? "");
      setNotes(payment.notes ?? "");
      setAmounts(Object.fromEntries(payment.allocations.map((a) => [`${a.documentType}:${a.documentId}`, a.allocatedAmount.toFixed(2)])));
    }).catch((e) => { if (!controller.signal.aborted) setError(e instanceof ApiError ? e.message : "Could not load draft payment."); });
    return () => controller.abort();
  }, [paymentId, navigate]);

  useEffect(() => {
    if (!vendorId) { setBills([]); setAmounts({}); return; }
    const controller = new AbortController();
    paymentService.payableDocuments(Number(vendorId), controller.signal).then((rows) => {
      setBills(rows);
      if (presetBill) {
        const selected = rows.find((row) => row.documentType === "PURCHASE_BILL" && row.id === presetBill);
        if (selected) setAmounts({ [`${selected.documentType}:${selected.id}`]: selected.outstandingAmount.toFixed(2) });
      }
    }).catch((e) => { if (!controller.signal.aborted) setError(e instanceof ApiError ? e.message : "Could not load payable bills."); });
    return () => controller.abort();
  }, [vendorId, presetBill]);

  function setAllocation(document: PayableDocument, value: string) {
    const parsed = Number(value);
    if (Number.isFinite(parsed) && parsed > document.outstandingAmount) value = document.outstandingAmount.toFixed(2);
    const key = `${document.documentType}:${document.id}`;
    setAmounts((current) => ({ ...current, [key]: value }));
  }

  async function save() {
    setError("");
    const allocations = Object.entries(amounts).map(([key, value]) => {
      const separator = key.indexOf(":");
      return { documentType: key.slice(0, separator), documentId: Number(key.slice(separator + 1)), allocatedAmount: Number(value) };
    }).filter((row) => row.allocatedAmount > 0);
    if (!vendorId || !ledgerId || !date || allocations.length === 0 || total <= 0) { setError("Choose a vendor, payment account, date, and at least one bill allocation."); return; }
    setSaving(true);
    try {
      const payload = { paymentDate: date, vendorId: Number(vendorId), paymentMethod: method,
        sourceLedgerId: Number(ledgerId), amount: Number(total.toFixed(2)), referenceNumber: reference || null, notes: notes || null, allocations };
      const payment = paymentId ? await paymentService.update(Number(paymentId), payload) : await paymentService.create(payload);
      popup.success(paymentId ? "Payment draft updated." : "Payment draft saved.");
      navigate(`/payments/${payment.id}`);
    } catch (e) { setError(e instanceof ApiError ? e.message : "Could not save payment."); }
    finally { setSaving(false); }
  }

  return <div className="purchase-order-page payments-page payments-form"><Link to="/payments" className="purchase-order-page__back">← Payments</Link>
    <section className="purchase-order-form">
    <header><div><h1>{paymentId ? "Edit supplier payment" : "New supplier payment"}</h1><p className="payments-subtitle">Allocate one payment across one or more posted bills or payable expenses.</p></div></header>
    {error && <p role="alert" className="payments-error">{error}</p>}
    <section className="purchase-order-form__section purchase-order-form__section--details"><div className="purchase-order-form__grid payments-fields">
      <label>Vendor<select value={vendorId} onChange={(e) => { setVendorId(e.target.value); setAmounts({}); }}><option value="">Select vendor</option>{vendors.map((v) => <option key={v.id} value={v.id}>{v.displayName}</option>)}</select></label>
      <label>Payment date<input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></label>
      <label>Payment method<select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>{(["BANK", "CASH", "UPI", "CHEQUE", "CARD"] as PaymentMethod[]).map((x) => <option key={x}>{x}</option>)}</select></label>
      <label>Paid from<select value={ledgerId} onChange={(e) => setLedgerId(e.target.value)}><option value="">Select cash or bank account</option>{ledgers.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
      <label>Reference<input value={reference} onChange={(e) => setReference(e.target.value)} maxLength={100} placeholder="UTR, cheque or transaction reference" /></label>
      <label>Notes<input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={4000} /></label>
    </div></section>
    <section className="purchase-order-form__section"><div className="purchase-order-form__section-title payments-section-title"><h2>Bill and expense allocations</h2><span>{bills.length} open documents</span></div>
      {!vendorId ? <p className="payments-muted">Select a vendor to see their open bills and payable expenses.</p> : bills.length === 0 ? <p className="payments-muted">No posted payable documents have an outstanding balance.</p> :
        <div className="payments-table-wrap"><table className="payments-table"><thead><tr><th>Document</th><th>Date</th><th>Due</th><th>Total</th><th>Settled</th><th>Outstanding</th><th>Apply now</th></tr></thead><tbody>
          {bills.map((bill) => { const key = `${bill.documentType}:${bill.id}`; return <tr key={key}><td>{bill.documentType === "PURCHASE_BILL" ? <Link to={`/purchase-bills/${bill.id}`}>{bill.documentNumber}</Link> : <Link to="/expenses/all">{bill.documentNumber} (Expense)</Link>}</td><td>{bill.documentDate}</td><td>{bill.dueDate ?? "—"}</td><td>{currency(bill.totalAmount)}</td><td>{currency(bill.settledAmount)}</td><td>{currency(bill.outstandingAmount)}</td><td><input aria-label={`Amount to apply to ${bill.documentNumber}`} type="number" min="0" max={bill.outstandingAmount} step="0.01" value={amounts[key] ?? ""} onChange={(e) => setAllocation(bill, e.target.value)} /></td></tr>; })}
        </tbody></table></div>}
      <div className="payments-total"><span>Payment total</span><strong>{currency(total)}</strong></div>
    </section>
    <div className="purchase-order-form__actions payments-actions"><button type="button" className="payments-secondary" onClick={() => navigate(paymentId ? `/payments/${paymentId}` : "/payments")}>Cancel</button><button type="button" className="payments-primary" disabled={saving || total <= 0} onClick={save}>{saving ? "Saving…" : paymentId ? "Update draft" : "Save draft"}</button></div>
    </section>
  </div>;
}

export function PaymentDetailPage() {
  const { paymentId } = useParams();
  const popup = useMessagePopup();
  const [payment, setPayment] = useState<Payment | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [reverseReason, setReverseReason] = useState("");
  const load = () => paymentService.get(Number(paymentId)).then(setPayment).catch((e) => setError(e instanceof ApiError ? e.message : "Could not load payment."));
  useEffect(() => { void load(); }, [paymentId]);
  async function act(action: "post" | "void" | "reverse") {
    if (!payment) return;
    setBusy(true); setError("");
    try {
      const result = action === "post" ? await paymentService.post(payment.id)
        : action === "void" ? await paymentService.void(payment.id)
        : await paymentService.reverse(payment.id, reverseReason, new Date().toISOString().slice(0, 10));
      setPayment(result); popup.success(action === "post" ? "Payment posted to the accounting journal." : action === "void" ? "Draft payment voided." : "Payment reversed.");
    } catch (e) { setError(e instanceof ApiError ? e.message : "Payment action failed."); }
    finally { setBusy(false); }
  }
  if (!payment) return <div className="purchase-order-page payments-page">{error || "Loading payment…"}</div>;
  return <div className="purchase-order-page payments-page"><Link to="/payments" className="purchase-order-page__back">← Payments</Link>
    <section className="purchase-order-form">
    {error && <p role="alert" className="payments-error">{error}</p>}
    <header><div><h1>{payment.paymentNumber}</h1><p className="payments-subtitle">{payment.vendorName} · {payment.paymentDate} · {payment.status}</p></div><div className="purchase-order-form__actions payments-actions">
      {payment.status === "DRAFT" && <><Link className="payments-edit-link" to={`/payments/${payment.id}/edit`}>Edit draft</Link><button type="button" className="payments-secondary" disabled={busy} onClick={() => void act("void")}>Void draft</button><button type="button" className="btn-success" disabled={busy} onClick={() => void act("post")}>{busy ? "Posting…" : "Post payment"}</button></>}
      {payment.status === "POSTED" && <><input aria-label="Reason for reversal" value={reverseReason} onChange={(e) => setReverseReason(e.target.value)} placeholder="Reason for reversal" /><button type="button" className="btn-danger" disabled={busy || reverseReason.trim().length < 3} onClick={() => { if (window.confirm(`Reverse ${payment.paymentNumber} and restore bill balances?`)) void act("reverse"); }}>Reverse payment</button></>}
    </div></header>
    <section className="purchase-order-form__section payments-summary"><div><span>Amount</span><strong>{currency(payment.amount)}</strong></div><div><span>Method</span><strong>{payment.paymentMethod}</strong></div><div><span>Paid from</span><strong>{payment.sourceLedgerName ?? "—"}</strong></div><div><span>Reference</span><strong>{payment.referenceNumber || "—"}</strong></div><div><span>Journal entry</span><strong>{payment.voucherId ? <Link to={`/accounting/journal?entryId=${payment.voucherId}`}>View #{payment.voucherId}</Link> : "Not posted"}</strong></div>{payment.reversalVoucherId && <div><span>Reversal journal</span><strong>#{payment.reversalVoucherId}</strong></div>}</section>
    <section className="purchase-order-form__section"><h2>Allocated documents</h2><div className="payments-table-wrap"><table className="payments-table"><thead><tr><th>Document</th><th>Applied</th><th>Outstanding after payment</th></tr></thead><tbody>{payment.allocations.map((a) => <tr key={a.id ?? `${a.documentType}-${a.documentId}`}><td>{a.documentType === "PURCHASE_BILL" ? <Link to={`/purchase-bills/${a.documentId}`}>{a.documentNumber ?? `Bill ${a.documentId}`}</Link> : <Link to="/expenses/all">{a.documentNumber ?? `Expense ${a.documentId}`}</Link>}</td><td>{currency(a.allocatedAmount)}</td><td>{a.outstandingAmount == null ? "—" : currency(a.outstandingAmount)}</td></tr>)}</tbody></table></div></section>
    </section>
  </div>;
}
