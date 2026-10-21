import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AccountFormModal } from './AccountFormModal';
import type { AccountType, Ledger } from '@/services/accountingService';

const mocks = vi.hoisted(() => ({
    listAccountTypes: vi.fn(),
    createLedger: vi.fn(),
    updateLedger: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
}));

vi.mock('@/services/accountingService', () => ({
    accountingService: {
        listAccountTypes: mocks.listAccountTypes,
        createLedger: mocks.createLedger,
        updateLedger: mocks.updateLedger,
    },
}));

vi.mock('@/components/ui/MessagePopup', () => ({
    useMessagePopup: () => ({
        success: mocks.success,
        error: mocks.error,
    }),
}));

const accountType: AccountType = {
    id: 4,
    code: 'CASH',
    name: 'Cash',
    accountGroupId: 2,
    accountGroupCode: 'ASSET',
    accountGroupName: 'Assets',
    normalBalance: 'DEBIT',
    defaultLedgerType: 'CASH',
    active: true,
    systemDefined: true,
};

const createdAccount: Ledger = {
    id: 12,
    companyId: 'company-1',
    accountGroupId: 2,
    accountGroupCode: 'ASSET',
    accountGroupName: 'Assets',
    accountTypeId: 4,
    accountTypeCode: 'CASH',
    accountTypeName: 'Cash',
    code: 'CASH-12',
    name: 'Counter Cash',
    description: null,
    openingBalance: 250,
    openingBalanceAsOfDate: '2026-07-19',
    balance: 250,
    ledgerType: 'CASH',
    normalBalance: 'DEBIT',
    active: true,
    systemDefined: false,
};

describe('AccountFormModal', () => {
    beforeEach(() => {
        mocks.listAccountTypes.mockResolvedValue([accountType]);
        mocks.createLedger.mockResolvedValue(createdAccount);
        mocks.updateLedger.mockResolvedValue(createdAccount);
        vi.clearAllMocks();
    });

    it('creates an account and returns it to the calling page', async () => {
        const onClose = vi.fn();
        const onSaved = vi.fn();

        render(
            <AccountFormModal
                initialValues={{
                    name: 'Counter Cash',
                    accountTypeId: 4,
                    openingBalance: 250,
                    openingBalanceAsOfDate: '2026-07-19',
                }}
                onClose={onClose}
                onSaved={onSaved}
            />,
        );

        await waitFor(() => {
            expect(screen.getByRole('combobox', { name: 'Account Type' }))
                .toHaveTextContent('Cash');
        });
        fireEvent.click(screen.getByRole('button', { name: 'Save Account' }));

        await waitFor(() => {
            expect(mocks.createLedger).toHaveBeenCalledWith({
                name: 'Counter Cash',
                accountTypeId: 4,
                openingBalance: 250,
                openingBalanceAsOfDate: '2026-07-19',
                description: null,
            });
        });
        expect(onSaved).toHaveBeenCalledWith(createdAccount);
        expect(onClose).toHaveBeenCalled();
        expect(mocks.success).toHaveBeenCalledWith('Account created successfully.');
    });
});
