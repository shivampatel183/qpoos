import { apiClient } from '@/lib/apiClient';
import { API } from '@/lib/apiConfig';
import type { Company } from '@/types';

// ─── Request / Response types ─────────────────────────────────────────────────

export interface CreateCompanyRequest {
    name: string;
    logo?: string;
    passwordOn?: boolean;
    password?: string;
    financialYearStart?: string;
    financialYearEnd?: string;
    addressLine1?: string;
    addressLine2?: string;
    city?: string;
    state?: string;
    country?: string;
    pincode?: string;
    phoneNumber?: string;
    email?: string;
    website?: string;
}

export interface CompanyDetail extends Company {
    logo?: string;
    passwordOn: boolean;
    financialYearStart?: string;
    financialYearEnd?: string;
    addressLine1?: string;
    addressLine2?: string;
    city?: string;
    state?: string;
    country?: string;
    pincode?: string;
    phoneNumber?: string;
    email?: string;
    website?: string;
    isActive: boolean;
    lastLoginAt?: string;
    createdAt: string;
    updatedAt: string;
}

interface CompanyAuthResponse {
    accessToken: string;
    expiresInSeconds: number;
}

// ─── Service ──────────────────────────────────────────────────────────────────

export const companyService = {
    async list(): Promise<Company[]> {
        return apiClient.get<Company[]>(API.company.list);
    },

    async create(payload: CreateCompanyRequest): Promise<CompanyDetail> {
        return apiClient.post<CompanyDetail>(API.company.create, payload);
    },

    async switchCompany(companyId: string): Promise<CompanyAuthResponse> {
        return apiClient.post<CompanyAuthResponse>(API.company.switchCompany(companyId));
    },
};
