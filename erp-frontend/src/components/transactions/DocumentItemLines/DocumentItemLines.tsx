import { Plus, Trash2 } from "lucide-react";
import { CustomSelect } from "@/components/ui/CustomSelect";
import type { Product } from "@/services/productService";
import "../TransactionFormModal/TransactionFormModal.css";

export type DocumentItemLine = {
  id: string;
  productId: string;
  description: string;
  quantity: string;
  unit: string;
  rate: string;
  discount: string;
  taxRate: string;
};

export type DocumentItemLinesProps = {
  size?: "s" | "m" | "l";
  lines: DocumentItemLine[];
  products: Product[];
  loading?: boolean;
  error?: string;
  onAdd: () => void;
  onChange: (id: string, changes: Partial<DocumentItemLine>) => void;
  onRemove: (id: string) => void;
  readOnly?: boolean;
};

function numberValue(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function money(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function lineAmount(line: DocumentItemLine): number {
  const gross = numberValue(line.quantity) * numberValue(line.rate);
  const taxable = Math.max(gross - numberValue(line.discount), 0);
  return money(taxable + (taxable * numberValue(line.taxRate)) / 100);
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function DocumentItemLines({
  size = "m",
  lines,
  products,
  loading = false,
  error,
  onAdd,
  onChange,
  onRemove,
  readOnly = false,
}: DocumentItemLinesProps) {
  const productOptions = products.map((product) => ({
    value: String(product.id),
    label: product.name,
  }));

  return (
    <section className="transaction-form__section">
      <div className="transaction-form__section-heading">
        <h3>Items</h3>
        <button
          type="button"
          className="transaction-form__add-line"
          onClick={onAdd}
          disabled={readOnly}
        >
          <Plus size={15} />
          Add Item
        </button>
      </div>
      {error && <small className="transaction-form__error">{error}</small>}
      <div
        className={`transaction-form__lines transaction-form__lines-scroll transaction-form__lines-scroll--${size}`}
      >
        <div className="transaction-form__line-header">
          <div className="transaction-form__line-header-grid">
            <span>Line</span>
            <span>Item</span>
            <span>Description</span>
            <span>Qty</span>
            <span>Unit</span>
            <span>Rate</span>
            <span>Discount</span>
            <span>Tax %</span>
            <span>Amount</span>
            <span />
          </div>
        </div>
        {lines.map((line, index) => (
          <div className="transaction-form__line" key={line.id}>
            <div className="transaction-form__line-grid">
              <div className="transaction-form__field transaction-form__line-type transaction-form__field--no-label">
                <strong>{index + 1}</strong>
              </div>
              <div className="transaction-form__field transaction-form__line-target transaction-form__field--no-label">
                <CustomSelect
                  value={line.productId}
                  onChange={(value) => {
                    const product = products.find(
                      (candidate) => String(candidate.id) === value,
                    );
                    onChange(line.id, {
                      productId: value,
                      description: product?.description ?? product?.name ?? "",
                      rate: String(
                        product?.purchaseCost ?? product?.salePrice ?? 0,
                      ),
                      unit: product?.unitId ?? "",
                    });
                  }}
                  options={productOptions}
                  placeholder="Select item"
                  disabled={loading || readOnly}
                />
              </div>
              <label className="transaction-form__field transaction-form__line-description transaction-form__field--no-label">
                <input
                  value={line.description}
                  onChange={(event) =>
                    onChange(line.id, { description: event.target.value })
                  }
                  disabled={readOnly}
                  maxLength={2000}
                />
              </label>
              <label className="transaction-form__field transaction-form__line-qty transaction-form__field--no-label">
                <input
                  type="number"
                  min="0.001"
                  step="0.001"
                  value={line.quantity}
                  onChange={(event) =>
                    onChange(line.id, { quantity: event.target.value })
                  }
                  disabled={readOnly}
                />
              </label>
              <label className="transaction-form__field transaction-form__line-unit transaction-form__field--no-label">
                <input
                  value={line.unit}
                  onChange={(event) =>
                    onChange(line.id, { unit: event.target.value })
                  }
                  disabled={readOnly}
                  maxLength={50}
                />
              </label>
              <label className="transaction-form__field transaction-form__line-rate transaction-form__field--no-label">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={line.rate}
                  onChange={(event) =>
                    onChange(line.id, { rate: event.target.value })
                  }
                  disabled={readOnly}
                />
              </label>
              <label className="transaction-form__field transaction-form__line-discount transaction-form__field--no-label">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={line.discount}
                  onChange={(event) =>
                    onChange(line.id, { discount: event.target.value })
                  }
                  disabled={readOnly}
                />
              </label>
              <label className="transaction-form__field transaction-form__line-tax transaction-form__field--no-label">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={line.taxRate}
                  onChange={(event) =>
                    onChange(line.id, { taxRate: event.target.value })
                  }
                  disabled={readOnly}
                />
              </label>
              <div className="transaction-form__line-amount transaction-form__field--no-label">
                <strong>{formatMoney(lineAmount(line))}</strong>
              </div>
              <button
                type="button"
                className="transaction-form__icon-button transaction-form__line-delete"
                onClick={() => onRemove(line.id)}
                disabled={readOnly || lines.length === 1}
                title="Remove item"
                aria-label={`Remove item ${index + 1}`}
              >
                <Trash2 size={16} />
              </button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
