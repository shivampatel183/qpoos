import { useState, type FormEvent, type ChangeEvent } from 'react';
import { X } from 'lucide-react';
import { companyService, type CreateCompanyRequest } from '@/services/companyService';
import { ApiError } from '@/lib/apiClient';
import { Spinner } from '@/components/ui/Spinner';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import type { Company } from '@/types';
import './CreateCompanyModal.css';

interface Props {
    onClose: () => void;
    onCreated: (company: Company) => void;
}

const EMPTY_FORM: CreateCompanyRequest = {
    name: '',
    email: '',
    phoneNumber: '',
    website: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    country: '',
    pincode: '',
    financialYearStart: '',
    financialYearEnd: '',
    passwordOn: false,
    password: '',
    logo: '',
};

export function CreateCompanyModal({ onClose, onCreated }: Props) {
    const messagePopup = useMessagePopup();
    const [form, setForm] = useState<CreateCompanyRequest>(EMPTY_FORM);
    const [loading, setLoading] = useState(false);
    const [nameError, setNameError] = useState<string | null>(null);

    function handleChange(e: ChangeEvent<HTMLInputElement>) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
        if (name === 'name' && nameError) setNameError(null);
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();

        if (!form.name.trim()) {
            setNameError('Company name is required');
            return;
        }

        // Strip empty optional strings so the API gets clean data
        const payload: CreateCompanyRequest = Object.fromEntries(
            Object.entries(form).filter(([, v]) => v !== '' && v !== undefined)
        ) as CreateCompanyRequest;

        // Don't send password field if passwordOn is false
        if (!payload.passwordOn) delete payload.password;

        setLoading(true);
        try {
            const created = await companyService.create(payload);
            onCreated({ id: created.id, name: created.name });
        } catch (err) {
            messagePopup.error(
                err instanceof ApiError
                    ? err.message
                    : 'Failed to create company. Please try again.',
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div className="modal-panel" onClick={e => e.stopPropagation()}>

                {/* Header */}
                <div className="modal-header">
                    <h2 className="modal-title">Create Company</h2>
                    <button className="modal-close" onClick={onClose} aria-label="Close">
                        <X size={18} />
                    </button>
                </div>

                <form className="modal-body" onSubmit={handleSubmit} noValidate>
                    {/* ── Basic Info ────────────────────────────────────── */}
                    <p className="form-section-title">Basic Info</p>

                    <div className="form-row">
                        <div className="form-field form-field--required">
                            <label htmlFor="cc-name">Company Name</label>
                            <input
                                id="cc-name"
                                name="name"
                                type="text"
                                placeholder="Acme Corp"
                                value={form.name}
                                autoFocus
                                onChange={handleChange}
                                className={nameError ? 'has-error' : ''}
                            />
                            {nameError && <p className="field-error">{nameError}</p>}
                        </div>

                        <div className="form-field">
                            <label htmlFor="cc-email">Email</label>
                            <input
                                id="cc-email"
                                name="email"
                                type="email"
                                placeholder="contact@acme.com"
                                value={form.email}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-field">
                            <label htmlFor="cc-phone">Phone Number</label>
                            <input
                                id="cc-phone"
                                name="phoneNumber"
                                type="tel"
                                placeholder="+91 98765 43210"
                                value={form.phoneNumber}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="cc-website">Website</label>
                            <input
                                id="cc-website"
                                name="website"
                                type="url"
                                placeholder="https://acme.com"
                                value={form.website}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    {/* ── Address ───────────────────────────────────────── */}
                    <p className="form-section-title">Address</p>

                    <div className="form-field">
                        <label htmlFor="cc-addr1">Address Line 1</label>
                        <input
                            id="cc-addr1"
                            name="addressLine1"
                            type="text"
                            placeholder="123 Main Street"
                            value={form.addressLine1}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="cc-addr2">Address Line 2</label>
                        <input
                            id="cc-addr2"
                            name="addressLine2"
                            type="text"
                            placeholder="Suite 4B"
                            value={form.addressLine2}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-row form-row--three">
                        <div className="form-field">
                            <label htmlFor="cc-city">City</label>
                            <input
                                id="cc-city"
                                name="city"
                                type="text"
                                placeholder="Mumbai"
                                value={form.city}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="cc-state">State</label>
                            <input
                                id="cc-state"
                                name="state"
                                type="text"
                                placeholder="Maharashtra"
                                value={form.state}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="cc-pincode">Pincode</label>
                            <input
                                id="cc-pincode"
                                name="pincode"
                                type="text"
                                placeholder="400001"
                                value={form.pincode}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="form-field">
                        <label htmlFor="cc-country">Country</label>
                        <input
                            id="cc-country"
                            name="country"
                            type="text"
                            placeholder="India"
                            value={form.country}
                            onChange={handleChange}
                        />
                    </div>

                    {/* ── Financial Year ────────────────────────────────── */}
                    <p className="form-section-title">Financial Year</p>

                    <div className="form-row">
                        <div className="form-field">
                            <label htmlFor="cc-fy-start">Start Date</label>
                            <input
                                id="cc-fy-start"
                                name="financialYearStart"
                                type="date"
                                value={form.financialYearStart}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="cc-fy-end">End Date</label>
                            <input
                                id="cc-fy-end"
                                name="financialYearEnd"
                                type="date"
                                value={form.financialYearEnd}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    {/* ── Security ──────────────────────────────────────── */}
                    <p className="form-section-title">Security</p>

                    <label className="form-toggle">
                        <input
                            type="checkbox"
                            name="passwordOn"
                            checked={form.passwordOn}
                            onChange={handleChange}
                        />
                        <span>Require password to access this company</span>
                    </label>

                    {form.passwordOn && (
                        <div className="form-field">
                            <label htmlFor="cc-password">Company Password</label>
                            <input
                                id="cc-password"
                                name="password"
                                type="password"
                                placeholder="Set a password"
                                value={form.password}
                                onChange={handleChange}
                            />
                        </div>
                    )}

                    {/* ── Actions ───────────────────────────────────────── */}
                    <div className="modal-footer">
                        <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={loading}>
                            {loading ? <><Spinner white size={14} />Creating…</> : 'Create Company'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
