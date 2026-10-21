import {
    useState,
    type ChangeEvent,
    type FormEvent,
} from 'react';
import { Modal } from '@/components/ui/Modal';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { Spinner } from '@/components/ui/Spinner';
import { ApiError } from '@/lib/apiClient';
import {
    vendorService,
    type Vendor,
    type VendorPayload,
} from '@/services/vendorService';
import './VendorFormModal.css';

type VendorFormState = {
    companyName: string;
    firstName: string;
    lastName: string;
    displayName: string;
    email: string;
    mobileNo: string;
    gstNo: string;
    streetAddress1: string;
    streetAddress2: string;
    city: string;
    state: string;
    country: string;
    pinCode: string;
    accountHolderName: string;
    accountNumber: string;
    ifscCode: string;
    openingBalance: string;
    notes: string;
};

type VendorFormErrors = Partial<Record<keyof VendorFormState, string>>;

export type VendorFormModalProps = {
    vendor?: Vendor;
    onClose: () => void;
    onSaved?: (vendor: Vendor) => void;
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const GST_PATTERN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;
const IFSC_PATTERN = /^[A-Z]{4}0[A-Z0-9]{6}$/;
const MONEY_PATTERN = /^\d+(?:\.\d{1,2})?$/;

function createFormState(vendor?: Vendor): VendorFormState {
    return {
        companyName: vendor?.companyName ?? '',
        firstName: vendor?.firstName ?? '',
        lastName: vendor?.lastName ?? '',
        displayName: vendor?.displayName ?? '',
        email: vendor?.email ?? '',
        mobileNo: vendor?.mobileNo ?? '',
        gstNo: vendor?.gstNo ?? '',
        streetAddress1: vendor?.streetAddress1 ?? '',
        streetAddress2: vendor?.streetAddress2 ?? '',
        city: vendor?.city ?? '',
        state: vendor?.state ?? '',
        country: vendor?.country ?? '',
        pinCode: vendor?.pinCode ?? '',
        accountHolderName: vendor?.accountHolderName ?? '',
        accountNumber: vendor?.accountNumber ?? '',
        ifscCode: vendor?.ifscCode ?? '',
        openingBalance: String(vendor?.openingBalance ?? 0),
        notes: vendor?.notes ?? '',
    };
}

function optionalValue(value: string): string | null {
    return value.trim() || null;
}

function toPayload(form: VendorFormState): VendorPayload {
    return {
        companyName: optionalValue(form.companyName),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        displayName: form.displayName.trim(),
        email: optionalValue(form.email),
        mobileNo: optionalValue(form.mobileNo),
        gstNo: optionalValue(form.gstNo.toUpperCase()),
        streetAddress1: optionalValue(form.streetAddress1),
        streetAddress2: optionalValue(form.streetAddress2),
        city: optionalValue(form.city),
        state: optionalValue(form.state),
        country: optionalValue(form.country),
        pinCode: optionalValue(form.pinCode),
        accountHolderName: optionalValue(form.accountHolderName),
        accountNumber: optionalValue(form.accountNumber),
        ifscCode: optionalValue(form.ifscCode.toUpperCase()),
        openingBalance: Number(form.openingBalance),
        notes: optionalValue(form.notes),
    };
}

function validateForm(form: VendorFormState): VendorFormErrors {
    const errors: VendorFormErrors = {};
    if (!form.firstName.trim()) errors.firstName = 'This field is required.';
    if (!form.lastName.trim()) errors.lastName = 'This field is required.';
    if (!form.displayName.trim()) errors.displayName = 'This field is required.';
    if (form.email.trim() && !EMAIL_PATTERN.test(form.email.trim())) {
        errors.email = 'Enter a valid email address.';
    }
    if (form.gstNo.trim() && !GST_PATTERN.test(form.gstNo.trim().toUpperCase())) {
        errors.gstNo = 'Enter a valid 15-character GST number.';
    }
    if (form.ifscCode.trim() && !IFSC_PATTERN.test(form.ifscCode.trim().toUpperCase())) {
        errors.ifscCode = 'Enter a valid 11-character IFSC code.';
    }
    if (!MONEY_PATTERN.test(form.openingBalance.trim())
        || !Number.isFinite(Number(form.openingBalance))
        || Number(form.openingBalance) < 0) {
        errors.openingBalance = 'Enter zero or a positive amount with up to two decimals.';
    }
    return errors;
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof ApiError ? error.message : fallback;
}

export function VendorFormModal({
    vendor,
    onClose,
    onSaved,
}: VendorFormModalProps) {
    const messagePopup = useMessagePopup();
    const [form, setForm] = useState<VendorFormState>(() => createFormState(vendor));
    const [errors, setErrors] = useState<VendorFormErrors>({});
    const [saving, setSaving] = useState(false);

    function handleChange(
        event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
    ) {
        const { name, value } = event.target;
        const normalizedValue = name === 'gstNo' || name === 'ifscCode'
            ? value.toUpperCase()
            : value;
        setForm((current) => ({ ...current, [name]: normalizedValue }));
        setErrors((current) => {
            if (!current[name as keyof VendorFormState]) return current;
            const next = { ...current };
            delete next[name as keyof VendorFormState];
            return next;
        });
    }

    function fieldClass(name: keyof VendorFormState, extra = ''): string {
        return [
            'vendor-form__field',
            extra,
            errors[name] ? 'vendor-form__field--invalid' : '',
        ].filter(Boolean).join(' ');
    }

    function fieldError(name: keyof VendorFormState) {
        return errors[name]
            ? <small className="vendor-form__error">{errors[name]}</small>
            : null;
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const validationErrors = validateForm(form);
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            const firstInvalid = event.currentTarget.querySelector<HTMLInputElement>(
                `[name="${Object.keys(validationErrors)[0]}"]`,
            );
            firstInvalid?.focus();
            return;
        }

        setSaving(true);
        try {
            const savedVendor = vendor
                ? await vendorService.update(vendor.id, toPayload(form))
                : await vendorService.create(toPayload(form));
            messagePopup.success(
                vendor ? 'Vendor updated successfully.' : 'Vendor created successfully.',
            );
            onSaved?.(savedVendor);
            onClose();
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to save vendor.'));
        } finally {
            setSaving(false);
        }
    }

    return (
        <Modal
            title={vendor ? 'Edit Vendor' : 'New Vendor'}
            maxWidth={840}
            onClose={onClose}
            dismissible={!saving}
            footer={(
                <>
                    <button
                        type="button"
                        className="vendor-form__button vendor-form__button--secondary"
                        onClick={onClose}
                        disabled={saving}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        form="vendor-form"
                        className="vendor-form__button vendor-form__button--primary"
                        disabled={saving}
                    >
                        {saving ? (
                            <>
                                <Spinner white size={14} />
                                Saving...
                            </>
                        ) : (
                            'Save Vendor'
                        )}
                    </button>
                </>
            )}
        >
            <form id="vendor-form" className="vendor-form" onSubmit={handleSubmit} noValidate>
                <section className="vendor-form__section">
                    <h3>Name and Contact</h3>
                    <div className="vendor-form__grid">
                        <label className="vendor-form__field vendor-form__field--wide">
                            <span>Company Name</span>
                            <input
                                name="companyName"
                                value={form.companyName}
                                onChange={handleChange}
                                maxLength={180}
                                autoFocus
                            />
                        </label>
                        <label className={fieldClass('firstName', 'vendor-form__field--required')}>
                            <span>First Name</span>
                            <input
                                name="firstName"
                                value={form.firstName}
                                onChange={handleChange}
                                maxLength={100}
                                required
                            />
                            {fieldError('firstName')}
                        </label>
                        <label className={fieldClass('lastName', 'vendor-form__field--required')}>
                            <span>Last Name</span>
                            <input
                                name="lastName"
                                value={form.lastName}
                                onChange={handleChange}
                                maxLength={100}
                                required
                            />
                            {fieldError('lastName')}
                        </label>
                        <label className={fieldClass('displayName', 'vendor-form__field--wide vendor-form__field--required')}>
                            <span>Display Name</span>
                            <input
                                name="displayName"
                                value={form.displayName}
                                onChange={handleChange}
                                maxLength={180}
                                required
                            />
                            {fieldError('displayName')}
                        </label>
                        <label className={fieldClass('email')}>
                            <span>Email</span>
                            <input
                                type="email"
                                name="email"
                                value={form.email}
                                onChange={handleChange}
                                inputMode="email"
                            />
                            {fieldError('email')}
                        </label>
                        <label className="vendor-form__field">
                            <span>Mobile No</span>
                            <input
                                type="tel"
                                name="mobileNo"
                                value={form.mobileNo}
                                onChange={handleChange}
                                maxLength={20}
                            />
                        </label>
                        <label className={fieldClass('gstNo', 'vendor-form__field--wide')}>
                            <span>GST No</span>
                            <input
                                name="gstNo"
                                value={form.gstNo}
                                onChange={handleChange}
                                maxLength={15}
                                spellCheck={false}
                            />
                            {fieldError('gstNo')}
                        </label>
                    </div>
                </section>

                <section className="vendor-form__section">
                    <h3>Address</h3>
                    <div className="vendor-form__grid">
                        <label className="vendor-form__field vendor-form__field--wide">
                            <span>Street Address 1</span>
                            <input name="streetAddress1" value={form.streetAddress1} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field vendor-form__field--wide">
                            <span>Street Address 2</span>
                            <input name="streetAddress2" value={form.streetAddress2} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field">
                            <span>City</span>
                            <input name="city" value={form.city} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field">
                            <span>State</span>
                            <input name="state" value={form.state} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field">
                            <span>Country</span>
                            <input name="country" value={form.country} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field">
                            <span>PIN Code</span>
                            <input
                                name="pinCode"
                                value={form.pinCode}
                                onChange={handleChange}
                                maxLength={12}
                            />
                        </label>
                    </div>
                </section>

                <section className="vendor-form__section">
                    <h3>Bank Details</h3>
                    <div className="vendor-form__grid">
                        <label className="vendor-form__field">
                            <span>Account Holder Name</span>
                            <input name="accountHolderName" value={form.accountHolderName} onChange={handleChange} />
                        </label>
                        <label className="vendor-form__field">
                            <span>Account Number</span>
                            <input name="accountNumber" value={form.accountNumber} onChange={handleChange} />
                        </label>
                        <label className={fieldClass('ifscCode')}>
                            <span>IFSC Code</span>
                            <input
                                name="ifscCode"
                                value={form.ifscCode}
                                onChange={handleChange}
                                maxLength={11}
                                spellCheck={false}
                            />
                            {fieldError('ifscCode')}
                        </label>
                        <label className={fieldClass('openingBalance')}>
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
                            {fieldError('openingBalance')}
                        </label>
                    </div>
                </section>

                <section className="vendor-form__section">
                    <h3>Other</h3>
                    <label className="vendor-form__field">
                        <span>Notes</span>
                        <textarea
                            name="notes"
                            value={form.notes}
                            onChange={handleChange}
                            maxLength={4000}
                            rows={5}
                        />
                        <small>{form.notes.length.toLocaleString()} / 4,000</small>
                    </label>
                </section>
            </form>
        </Modal>
    );
}
