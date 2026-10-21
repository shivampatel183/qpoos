import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import {
  purchaseBillService,
  type PurchaseBill,
} from "@/services/purchaseBillService";
import "../purchase-orders/PurchaseOrderPage.css";

export function PurchaseBillDetailPage() {
  const { billId } = useParams();
  const navigate = useNavigate();
  const popup = useMessagePopup();
  const [bill, setBill] = useState<PurchaseBill | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    purchaseBillService
      .get(Number(billId), controller.signal)
      .then(setBill)
      .catch((error) => {
        if (!controller.signal.aborted)
          popup.error(
            error instanceof ApiError ? error.message : "Failed to load purchase bill.",
          );
      });
    return () => controller.abort();
  }, [billId, popup]);

  async function action(name: "post" | "cancel") {
    if (!bill) return;
    try {
      const updated = await purchaseBillService[name](bill.id);
      setBill(updated);
      popup.success(`Purchase bill ${name}ed.`);
    } catch (error) {
      popup.error(error instanceof ApiError ? error.message : "Action failed.");
    }
  }

  if (!bill)
    return <div className="purchase-order-page__loading">Loading purchase bill...</div>;

  return (
    <div className="purchase-order-page">
      <button className="purchase-order-page__back" onClick={() => navigate("/purchase-bills")}>
        <ArrowLeft size={16} /> Purchase Bills
      </button>

      <section className="purchase-order-form">
        <header>
          <div>
            <h1>{bill.billNumber}</h1>
            <p>
              {bill.vendorName} · {bill.status} · {bill.billDate}
            </p>
          </div>
          <div className="purchase-order-form__actions">
            {bill.status === "DRAFT" && (
              <>
                <button onClick={() => navigate(`/purchase-bills/${bill.id}/edit`)}>Edit</button>
                <button onClick={() => action("post")}>Post</button>
              </>
            )}
            {bill.status === "DRAFT" && (
              <button onClick={() => action("cancel")}>Cancel</button>
            )}
          </div>
        </header>

        <section>
          <h2>Items</h2>
          {bill.lines.map((line) => (
            <div className="purchase-order-form__line" key={line.id ?? `${bill.id}-${line.lineNo}`}>
              <strong>{line.lineNo}</strong>
              <span>{line.productName}</span>
              <span>
                {line.quantity} {line.unit ?? ""}
              </span>
              <span>{line.rate}</span>
              <span>{line.taxRate}%</span>
              <strong>{line.amount}</strong>
            </div>
          ))}
          <p>
            Total: {new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(bill.totalAmount)}
          </p>
        </section>
      </section>
    </div>
  );
}
