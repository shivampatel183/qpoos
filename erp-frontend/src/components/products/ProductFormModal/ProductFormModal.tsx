import { useState, type ChangeEvent, type FormEvent } from "react";
import { Modal } from "@/components/ui/Modal";
import { CustomSelect } from "@/components/ui/CustomSelect";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { Spinner } from "@/components/ui/Spinner";
import { ApiError } from "@/lib/apiClient";
import {
  productService,
  type Product,
  type ProductPayload,
  type ProductType,
} from "@/services/productService";
import "./ProductFormModal.css";

type ProductFormState = {
  name: string;
  description: string;
  type: ProductType | "";
  sku: string;
  barcode: string;
  hsnCode: string;
  trackInventory: boolean;
  openingQuantity: string;
  asOfDate: string;
  alertQuantity: string;
  reorderQuantity: string;
  salePrice: string;
  purchaseCost: string;
  unitId: string;
  categoryId: string;
  saleTaxId: string;
  purchaseTaxId: string;
};

type ProductFormErrors = Partial<Record<keyof ProductFormState, string>>;

export type ProductFormModalProps = {
  product?: Product;
  onClose: () => void;
  onSaved?: (product: Product) => void;
};

const DECIMAL_PATTERN = /^\d+(?:\.\d{1,2})?$/;
const QTY_PATTERN = /^\d+(?:\.\d+)?$/;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const PRODUCT_TYPE_OPTIONS = [
  {
    value: "INVENTORY",
    label: "Inventory — physical goods with stock tracking",
  },
  {
    value: "NON_INVENTORY",
    label: "Non-Inventory — physical goods, no stock tracking",
  },
  { value: "SERVICE", label: "Service — no inventory" },
];

function createFormState(product?: Product): ProductFormState {
  return {
    name: product?.name ?? "",
    description: product?.description ?? "",
    type: product?.type ?? "",
    sku: product?.sku ?? "",
    barcode: product?.barcode ?? "",
    hsnCode: product?.hsnCode ?? "",
    trackInventory: product?.trackInventory ?? false,
    openingQuantity:
      product?.openingQuantity != null ? String(product.openingQuantity) : "",
    asOfDate: product?.asOfDate ?? "",
    alertQuantity:
      product?.alertQuantity != null ? String(product.alertQuantity) : "",
    reorderQuantity:
      product?.reorderQuantity != null ? String(product.reorderQuantity) : "",
    salePrice: product?.salePrice ?? "",
    purchaseCost: product?.purchaseCost ?? "",
    unitId: product?.unitId ?? "",
    categoryId: product?.categoryId ?? "",
    saleTaxId: product?.saleTaxId ?? "",
    purchaseTaxId: product?.purchaseTaxId ?? "",
  };
}

function optionalStr(value: string): string | null {
  return value.trim() || null;
}

function optionalNum(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const n = Number(trimmed);
  return Number.isFinite(n) ? n : null;
}

function toPayload(form: ProductFormState): ProductPayload {
  const isInventory = form.type === "INVENTORY";
  return {
    name: form.name.trim(),
    description: optionalStr(form.description),
    type: form.type as ProductType,
    sku: optionalStr(form.sku),
    barcode: optionalStr(form.barcode),
    hsnCode: optionalStr(form.hsnCode),
    trackInventory: form.trackInventory,
    openingQuantity: isInventory ? optionalNum(form.openingQuantity) : null,
    asOfDate: isInventory ? optionalStr(form.asOfDate) : null,
    alertQuantity: isInventory ? optionalNum(form.alertQuantity) : null,
    reorderQuantity: isInventory ? optionalNum(form.reorderQuantity) : null,
    salePrice: optionalStr(form.salePrice),
    purchaseCost: optionalStr(form.purchaseCost),
    unitId: optionalStr(form.unitId),
    categoryId: optionalStr(form.categoryId),
    saleTaxId: optionalStr(form.saleTaxId),
    purchaseTaxId: optionalStr(form.purchaseTaxId),
  };
}

function validateForm(form: ProductFormState): ProductFormErrors {
  const errors: ProductFormErrors = {};
  if (!form.name.trim()) errors.name = "This field is required.";
  if (!form.type) errors.type = "Select a product type.";
  if (form.salePrice.trim() && !DECIMAL_PATTERN.test(form.salePrice.trim())) {
    errors.salePrice = "Enter a valid amount (e.g. 500.00).";
  }
  if (
    form.purchaseCost.trim() &&
    !DECIMAL_PATTERN.test(form.purchaseCost.trim())
  ) {
    errors.purchaseCost = "Enter a valid amount (e.g. 500.00).";
  }
  if (
    form.openingQuantity.trim() &&
    !QTY_PATTERN.test(form.openingQuantity.trim())
  ) {
    errors.openingQuantity = "Enter a valid quantity (min 0).";
  }
  if (
    form.alertQuantity.trim() &&
    !QTY_PATTERN.test(form.alertQuantity.trim())
  ) {
    errors.alertQuantity = "Enter a valid quantity (min 0).";
  }
  if (
    form.reorderQuantity.trim() &&
    !QTY_PATTERN.test(form.reorderQuantity.trim())
  ) {
    errors.reorderQuantity = "Enter a valid quantity (min 0).";
  }
  if (form.asOfDate.trim() && !DATE_PATTERN.test(form.asOfDate.trim())) {
    errors.asOfDate = "Enter date as YYYY-MM-DD.";
  }
  return errors;
}

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

export function ProductFormModal({
  product,
  onClose,
  onSaved,
}: ProductFormModalProps) {
  const messagePopup = useMessagePopup();
  const [form, setForm] = useState<ProductFormState>(() =>
    createFormState(product),
  );
  const [errors, setErrors] = useState<ProductFormErrors>({});
  const [saving, setSaving] = useState(false);

  const isInventory = form.type === "INVENTORY";

  function handleChange(
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) {
    const { name, value, type } = event.target;
    const newValue =
      type === "checkbox" ? (event.target as HTMLInputElement).checked : value;
    setForm((current) => ({ ...current, [name]: newValue }));
    setErrors((current) => {
      if (!current[name as keyof ProductFormState]) return current;
      const next = { ...current };
      delete next[name as keyof ProductFormState];
      return next;
    });
  }

  function handleTypeChange(value: string) {
    setForm((current) => ({
      ...current,
      type: value as ProductType,
      // clear inventory-only fields when switching away
      ...(value !== "INVENTORY" && {
        openingQuantity: "",
        asOfDate: "",
        alertQuantity: "",
        reorderQuantity: "",
      }),
    }));
    setErrors((current) => {
      const next = { ...current };
      delete next.type;
      return next;
    });
  }

  function fieldClass(name: keyof ProductFormState, extra = ""): string {
    return [
      "product-form__field",
      extra,
      errors[name] ? "product-form__field--invalid" : "",
    ]
      .filter(Boolean)
      .join(" ");
  }

  function fieldError(name: keyof ProductFormState) {
    return errors[name] ? (
      <small className="product-form__error">{errors[name]}</small>
    ) : null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      const firstKey = Object.keys(validationErrors)[0];
      const firstInvalid = event.currentTarget.querySelector<HTMLElement>(
        `[name="${firstKey}"]`,
      );
      firstInvalid?.focus();
      return;
    }

    setSaving(true);
    try {
      const savedProduct = product
        ? await productService.update(product.id, toPayload(form))
        : await productService.create(toPayload(form));
      messagePopup.success(
        product
          ? "Product updated successfully."
          : "Product created successfully.",
      );
      onSaved?.(savedProduct);
      onClose();
    } catch (error) {
      messagePopup.error(getErrorMessage(error, "Failed to save product."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={product ? "Edit Product" : "New Product"}
      maxWidth={860}
      onClose={onClose}
      dismissible={!saving}
      footer={
        <>
          <button
            type="button"
            className="product-form__button product-form__button--secondary"
            onClick={onClose}
            disabled={saving}
          >
            Cancel
          </button>
          <button
            type="submit"
            form="product-form"
            className="product-form__button product-form__button--primary"
            disabled={saving}
          >
            {saving ? (
              <>
                <Spinner white size={14} />
                Saving...
              </>
            ) : (
              "Save Product"
            )}
          </button>
        </>
      }
    >
      <form
        id="product-form"
        className="product-form"
        onSubmit={handleSubmit}
        noValidate
      >
        {/* ── Basic Info ─────────────────────────────────────────── */}
        <section className="product-form__section">
          <h3>Basic Information</h3>
          <div className="product-form__grid">
            <label
              className={fieldClass(
                "name",
                "product-form__field--wide product-form__field--required",
              )}
            >
              <span>Product Name</span>
              <input
                name="name"
                value={form.name}
                onChange={handleChange}
                maxLength={255}
                autoFocus
                required
              />
              {fieldError("name")}
            </label>

            <label
              className={`product-form__field product-form__field--wide product-form__field--required${errors.type ? " product-form__field--invalid" : ""}`}
            >
              <span>Product Type</span>
              <CustomSelect
                value={form.type}
                onChange={handleTypeChange}
                options={PRODUCT_TYPE_OPTIONS}
                placeholder="Select type…"
                name="type"
              />
              {fieldError("type")}
            </label>

            <label className={fieldClass("sku")}>
              <span>SKU</span>
              <input
                name="sku"
                value={form.sku}
                onChange={handleChange}
                maxLength={100}
                spellCheck={false}
              />
            </label>

            <label className={fieldClass("barcode")}>
              <span>Barcode</span>
              <input
                name="barcode"
                value={form.barcode}
                onChange={handleChange}
                maxLength={100}
                spellCheck={false}
              />
            </label>

            <label className={fieldClass("hsnCode")}>
              <span>HSN Code</span>
              <input
                name="hsnCode"
                value={form.hsnCode}
                onChange={handleChange}
                maxLength={20}
                spellCheck={false}
              />
            </label>

            <label
              className={fieldClass("description", "product-form__field--wide")}
            >
              <span>Description</span>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                maxLength={1000}
                rows={3}
              />
              <small>{form.description.length.toLocaleString()} / 1,000</small>
            </label>
          </div>
        </section>

        {/* ── Pricing ────────────────────────────────────────────── */}
        <section className="product-form__section">
          <h3>Pricing</h3>
          <div className="product-form__grid">
            <label className={fieldClass("salePrice")}>
              <span>Sale Price</span>
              <input
                name="salePrice"
                value={form.salePrice}
                onChange={handleChange}
                inputMode="decimal"
                placeholder="0.00"
              />
              {fieldError("salePrice")}
            </label>

            <label className={fieldClass("purchaseCost")}>
              <span>Purchase Cost</span>
              <input
                name="purchaseCost"
                value={form.purchaseCost}
                onChange={handleChange}
                inputMode="decimal"
                placeholder="0.00"
              />
              {fieldError("purchaseCost")}
            </label>
          </div>
        </section>

        {/* ── Inventory ─────────────────────────────────────────── */}
        {isInventory && (
          <section className="product-form__section">
            <h3>Inventory Tracking</h3>
            <div className="product-form__grid">
              <label className="product-form__field product-form__field--wide product-form__checkbox">
                <input
                  type="checkbox"
                  name="trackInventory"
                  checked={form.trackInventory}
                  onChange={handleChange}
                />
                <span>Track quantity on hand</span>
              </label>

              <label className={fieldClass("openingQuantity")}>
                <span>Opening Quantity</span>
                <input
                  name="openingQuantity"
                  value={form.openingQuantity}
                  onChange={handleChange}
                  inputMode="decimal"
                  placeholder="0"
                />
                {fieldError("openingQuantity")}
              </label>

              <label className={fieldClass("asOfDate")}>
                <span>As of Date</span>
                <input
                  type="date"
                  name="asOfDate"
                  value={form.asOfDate}
                  onChange={handleChange}
                />
                {fieldError("asOfDate")}
              </label>

              <label className={fieldClass("alertQuantity")}>
                <span>Alert Quantity</span>
                <input
                  name="alertQuantity"
                  value={form.alertQuantity}
                  onChange={handleChange}
                  inputMode="decimal"
                  placeholder="0"
                />
                {fieldError("alertQuantity")}
              </label>

              <label className={fieldClass("reorderQuantity")}>
                <span>Reorder Quantity</span>
                <input
                  name="reorderQuantity"
                  value={form.reorderQuantity}
                  onChange={handleChange}
                  inputMode="decimal"
                  placeholder="0"
                />
                {fieldError("reorderQuantity")}
              </label>
            </div>
          </section>
        )}

        {/* ── Tax ───────────────────────────────────────────────── */}
        <section className="product-form__section">
          <h3>Tax</h3>
          <div className="product-form__grid">
            <label className={fieldClass("saleTaxId")}>
              <span>Sale Tax</span>
              <input
                name="saleTaxId"
                value={form.saleTaxId}
                onChange={handleChange}
                placeholder="Tax UUID"
                spellCheck={false}
              />
            </label>

            <label className={fieldClass("purchaseTaxId")}>
              <span>Purchase Tax</span>
              <input
                name="purchaseTaxId"
                value={form.purchaseTaxId}
                onChange={handleChange}
                placeholder="Tax UUID"
                spellCheck={false}
              />
            </label>
          </div>
        </section>
      </form>
    </Modal>
  );
}
