import { API } from "@/lib/apiConfig";
import { apiClient } from "@/lib/apiClient";

export type ProductType = "INVENTORY" | "NON_INVENTORY" | "SERVICE";

export interface Product {
  id: string;
  name: string;
  description: string | null;
  type: ProductType;
  sku: string | null;
  barcode: string | null;
  hsnCode: string | null;
  trackInventory: boolean;
  quantityOnHand: number | null;
  openingQuantity: number | null;
  asOfDate: string | null;
  alertQuantity: number | null;
  reorderQuantity: number | null;
  salePrice: string | null;
  purchaseCost: string | null;
  unitId: string | null;
  categoryId: string | null;
  saleTaxId: string | null;
  purchaseTaxId: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPayload {
  name: string;
  description: string | null;
  type: ProductType;
  sku: string | null;
  barcode: string | null;
  hsnCode: string | null;
  trackInventory: boolean;
  openingQuantity: number | null;
  asOfDate: string | null;
  alertQuantity: number | null;
  reorderQuantity: number | null;
  salePrice: string | null;
  purchaseCost: string | null;
  unitId: string | null;
  categoryId: string | null;
  saleTaxId: string | null;
  purchaseTaxId: string | null;
}

export const productService = {
  list(signal?: AbortSignal): Promise<Product[]> {
    return apiClient.get<Product[]>(API.products.list, { signal });
  },

  get(id: string, signal?: AbortSignal): Promise<Product> {
    return apiClient.get<Product>(API.products.detail(id), { signal });
  },

  create(payload: ProductPayload): Promise<Product> {
    return apiClient.post<Product>(API.products.create, payload);
  },

  update(id: string, payload: ProductPayload): Promise<Product> {
    return apiClient.put<Product>(API.products.update(id), payload);
  },

  delete(id: string): Promise<void> {
    return apiClient.delete<void>(API.products.delete(id));
  },
};
