import {
    useEffect,
    useMemo,
    useState,
    type ChangeEvent,
    type FormEvent,
} from 'react';
import { ApiError } from '@/lib/apiClient';
import {
    accountingService,
    type AccountType,
    type Ledger,
    type LedgerPayload,
} from '@/services/accountingService';
import { Modal } from '@/components/ui/Modal';
import { CustomSelect } from '@/components/ui/CustomSelect';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { Spinner } from '@/components/ui/Spinner';
import './AccountFormModal.css';

type AccountFormState = {
    name: string;
    accountTypeId: string;
    openingBalance: string;
    openingBalanceAsOfDate: string;
    description: string;
};

export type AccountFormModalProps = {
    account?: Ledger;
    initialValues?: Partial<LedgerPayload>;
    onClose: () => void;
    onSaved?: (account: Ledger) => void;
};

function localDateValue(): string {
    const date = new Date();
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function createFormState(
    account?: Ledger,
    initialValues?: Partial<LedgerPayload>,
): AccountFormState {
    return {
        name: account?.name ?? initialValues?.name ?? '',
        accountTypeId: String(account?.accountTypeId ?? initialValues?.accountTypeId ?? ''),
        openingBalance: String(account?.openingBalance ?? initialValues?.openingBalance ?? 0),
        openingBalanceAsOfDate:
            account?.openingBalanceAsOfDate
            ?? initialValues?.openingBalanceAsOfDate
            ?? localDateValue(),
        description: account?.description ?? initialValues?.description ?? '',
    };
}

function toPayload(form: AccountFormState): LedgerPayload {
    return {
        name: form.name.trim(),
        accountTypeId: Number(form.accountTypeId),
        openingBalance: Number(form.openingBalance),
        openingBalanceAsOfDate: form.openingBalanceAsOfDate,
        description: form.description.trim() || null,
    };
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof ApiError ? error.message : fallback;
}

export function AccountFormModal({
    account,
    initialValues,
    onClose,
    onSaved,
}: AccountFormModalProps) {
    const messagePopup = useMessagePopup();
    const [form, setForm] = useState<AccountFormState>(() => (
        createFormState(account, initialValues)
    ));
    const [accountTypes, setAccountTypes] = useState<AccountType[]>([]);
    const [loadingAccountTypes, setLoadingAccountTypes] = useState(true);
    const [saving, setSaving] = useState(false);

    const groupedAccountTypes = useMemo(() => {
        const groups = new Map<string, AccountType[]>();
        for (const accountType of accountTypes.filter((item) => item.active)) {
            const items = groups.get(accountType.accountGroupName) ?? [];
            items.push(accountType);
            groups.set(accountType.accountGroupName, items);
        }
        return Array.from(groups.entries());
    }, [accountTypes]);

    useEffect(() => {
        const controller = new AbortController();

        async function loadAccountTypes() {
            try {
                setAccountTypes(await accountingService.listAccountTypes(controller.signal));
            } catch (error) {
                if (!controller.signal.aborted) {
                    messagePopup.error(
                        getErrorMessage(error, 'Failed to load account types.'),
                    );
                }
            } finally {
                if (!controller.signal.aborted) setLoadingAccountTypes(false);
            }
        }

        void loadAccountTypes();
        return () => controller.abort();
    }, [messagePopup]);

    function handleChange(
        event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
    ) {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const openingBalance = Number(form.openingBalance);

        if (!form.name.trim() || !form.accountTypeId || !form.openingBalanceAsOfDate) {
            messagePopup.error(
                'Name, account type, opening balance, and date are required.',
            );
            return;
        }

        if (!Number.isFinite(openingBalance) || openingBalance < 0) {
            messagePopup.error('Opening balance must be zero or greater.');
            return;
        }

        setSaving(true);
        try {
            const savedAccount = account
                ? await accountingService.updateLedger(account.id, toPayload(form))
                : await accountingService.createLedger(toPayload(form));

            messagePopup.success(
                account
                    ? 'Account updated successfully.'
                    : 'Account created successfully.',
            );
            onSaved?.(savedAccount);
            onClose();
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to save account.'));
        } finally {
            setSaving(false);
        }
    }

    return (
        <Modal
            title={account ? 'Edit Account' : 'New Account'}
            maxWidth={620}
            onClose={onClose}
            dismissible={!saving}
            footer={(
                <>
                    <button
                        type="button"
                        className="account-form-modal__btn account-form-modal__btn--secondary"
                        onClick={onClose}
                        disabled={saving}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        form="account-form"
                        className="account-form-modal__btn account-form-modal__btn--primary"
                        disabled={saving || loadingAccountTypes}
                    >
                        {saving ? (
                            <>
                                <Spinner white size={14} />
                                Saving...
                            </>
                        ) : (
                            'Save Account'
                        )}
                    </button>
                </>
            )}
        >
            <form
                id="account-form"
                className="account-form-modal__form"
                onSubmit={handleSubmit}
                noValidate
            >
                <label className="account-form-modal__field account-form-modal__field--required">
                    <span>Name</span>
                    <input
                        name="name"
                        value={form.name}
                        onChange={handleChange}
                        maxLength={180}
                        placeholder="Petty Cash"
                        autoFocus
                    />
                </label>

                <div className="account-form-modal__field account-form-modal__field--required">
                    <span id="account-type-label">Account Type</span>
                    <CustomSelect
                        name="accountTypeId"
                        value={form.accountTypeId}
                        onChange={(accountTypeId) => {
                            setForm((current) => ({ ...current, accountTypeId }));
                        }}
                        groups={groupedAccountTypes.map(([label, types]) => ({
                            label,
                            options: types.map((accountType) => ({
                                value: String(accountType.id),
                                label: accountType.name,
                            })),
                        }))}
                        placeholder={loadingAccountTypes
                            ? 'Loading account types...'
                            : 'Select account type'}
                        ariaLabelledBy="account-type-label"
                        disabled={loadingAccountTypes}
                        maxMenuHeight={260}
                    />
                </div>

                <div className="account-form-modal__row">
                    <label className="account-form-modal__field account-form-modal__field--required">
                        <span>Opening Balance</span>
                        <input
                            type="number"
                            name="openingBalance"
                            value={form.openingBalance}
                            onChange={handleChange}
                            min="0"
                            step="0.01"
                            inputMode="decimal"
                        />
                    </label>

                    <label className="account-form-modal__field account-form-modal__field--required">
                        <span>As of Date</span>
                        <input
                            type="date"
                            name="openingBalanceAsOfDate"
                            value={form.openingBalanceAsOfDate}
                            onChange={handleChange}
                        />
                    </label>
                </div>

                <label className="account-form-modal__field">
                    <span>Description</span>
                    <textarea
                        name="description"
                        value={form.description}
                        onChange={handleChange}
                        maxLength={1000}
                        rows={4}
                        placeholder="Optional account description"
                    />
                </label>
            </form>
        </Modal>
    );
}
