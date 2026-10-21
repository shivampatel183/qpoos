import { API } from '@/lib/apiConfig';
import { apiClient } from '@/lib/apiClient';

export interface Vendor {
    id: number;
    companyId?: string;
    companyName?: string | null;
    firstName: string;
    lastName: string;
    displayName: string;
    email?: string | null;
    mobileNo?: string | null;
    gstNo?: string | null;
    streetAddress1?: string | null;
    streetAddress2?: string | null;
    city?: string | null;
    state?: string | null;
    country?: string | null;
    pinCode?: string | null;
    accountHolderName?: string | null;
    accountNumber?: string | null;
    ifscCode?: string | null;
    openingBalance: number;
    notes?: string | null;
}

export interface VendorPayload {
    companyName: string | null;
    firstName: string;
    lastName: string;
    displayName: string;
    email: string | null;
    mobileNo: string | null;
    gstNo: string | null;
    streetAddress1: string | null;
    streetAddress2: string | null;
    city: string | null;
    state: string | null;
    country: string | null;
    pinCode: string | null;
    accountHolderName: string | null;
    accountNumber: string | null;
    ifscCode: string | null;
    openingBalance: number;
    notes: string | null;
}

export const vendorService = {
    list(signal?: AbortSignal): Promise<Vendor[]> {
        return apiClient.get<Vendor[]>(API.vendors.list, { signal });
    },

    get(id: number, signal?: AbortSignal): Promise<Vendor> {
        return apiClient.get<Vendor>(API.vendors.detail(id), { signal });
    },

    create(payload: VendorPayload): Promise<Vendor> {
        return apiClient.post<Vendor>(API.vendors.create, payload);
    },

    update(id: number, payload: VendorPayload): Promise<Vendor> {
        return apiClient.put<Vendor>(API.vendors.update(id), payload);
    },

    delete(id: number): Promise<void> {
        return apiClient.delete<void>(API.vendors.delete(id));
    },
};
