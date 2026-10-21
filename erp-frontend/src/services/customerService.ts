import { API } from '@/lib/apiConfig';
import { apiClient } from '@/lib/apiClient';

export interface Customer {
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

export interface CustomerPayload {
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

export const customerService = {
    list(signal?: AbortSignal): Promise<Customer[]> {
        return apiClient.get<Customer[]>(API.customers.list, { signal });
    },

    get(id: number, signal?: AbortSignal): Promise<Customer> {
        return apiClient.get<Customer>(API.customers.detail(id), { signal });
    },

    create(payload: CustomerPayload): Promise<Customer> {
        return apiClient.post<Customer>(API.customers.create, payload);
    },

    update(id: number, payload: CustomerPayload): Promise<Customer> {
        return apiClient.put<Customer>(API.customers.update(id), payload);
    },

    delete(id: number): Promise<void> {
        return apiClient.delete<void>(API.customers.delete(id));
    },
};
