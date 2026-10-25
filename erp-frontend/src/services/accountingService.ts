import { apiClient } from '@/lib/apiClient';
import { API } from '@/lib/apiConfig';

export type LedgerType =
    | 'GENERAL'
    | 'CASH'
    | 'BANK'
    | 'CUSTOMER'
    | 'SUPPLIER'
    | 'TAX'
    | 'CONTROL';

export type NormalBalance = 'DEBIT' | 'CREDIT';

export interface Ledger {
    id: number;
    companyId: string;
    accountGroupId: number;
    accountGroupCode: string;
    accountGroupName: string;
    accountTypeId: number;
    accountTypeCode: string;
    accountTypeName: string;
    code: string;
    name: string;
    description?: string | null;
    openingBalance: number;
    openingBalanceAsOfDate: string;
    balance: number;
    ledgerType: LedgerType;
    normalBalance: NormalBalance;
    active: boolean;
    postingAllowed?: boolean;
    systemDefined: boolean;
}

export interface LedgerPayload {
    name: string;
    accountTypeId: number;
    openingBalance: number;
    openingBalanceAsOfDate: string;
    description?: string | null;
}

export interface AccountType {
    id: number;
    code: string;
    name: string;
    accountGroupId: number;
    accountGroupCode: string;
    accountGroupName: string;
    normalBalance: NormalBalance;
    defaultLedgerType: LedgerType;
    displayOrder?: number | null;
    active: boolean;
    systemDefined: boolean;
}

export interface JournalLine {
    id: number;
    lineNo: number;
    ledgerId: number;
    ledgerCode: string;
    ledgerName: string;
    debitAmount: number;
    creditAmount: number;
    description: string | null;
}

export interface JournalEntry {
    id: number;
    companyId: string;
    journalNumber: string;
    accountingDate: string;
    sourceType: 'PURCHASE_BILL' | 'EXPENSE';
    sourceDocumentId: number;
    entryKind: 'ORIGINAL' | 'REVERSAL';
    narration: string;
    status: 'POSTED';
    createdBy: string;
    postedBy: string;
    createdAt: string;
    postedAt: string;
    lines: JournalLine[];
}

export const accountingService = {
    listLedgers(signal?: AbortSignal): Promise<Ledger[]> {
        return apiClient.get<Ledger[]>(API.accounting.ledgers, { signal });
    },

    createLedger(payload: LedgerPayload): Promise<Ledger> {
        return apiClient.post<Ledger>(API.accounting.ledgers, payload);
    },

    updateLedger(id: number, payload: LedgerPayload): Promise<Ledger> {
        return apiClient.put<Ledger>(API.accounting.ledger(id), payload);
    },

    deleteLedger(id: number): Promise<void> {
        return apiClient.delete<void>(API.accounting.ledger(id));
    },

    listAccountTypes(signal?: AbortSignal): Promise<AccountType[]> {
        return apiClient.get<AccountType[]>(API.accounting.accountTypes, { signal });
    },

    listJournalEntries(signal?: AbortSignal): Promise<JournalEntry[]> {
        return apiClient.get<JournalEntry[]>(API.accounting.journals, { signal });
    },

    getJournalEntry(id: number, signal?: AbortSignal): Promise<JournalEntry> {
        return apiClient.get<JournalEntry>(API.accounting.journal(id), { signal });
    },
};
