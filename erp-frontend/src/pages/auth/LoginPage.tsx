import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { authService, ApiError } from '@/services/authService';
import { safeInternalPath } from '@/lib/navigation';
import { Spinner } from '@/components/ui/Spinner';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import './auth.css';

function isValidEmail(email: string) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { replaceToken } = useAuth();
    const messagePopup = useMessagePopup();
    const registrationMessageShown = useRef(false);

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [loading, setLoading] = useState(false);
    const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
    const justRegistered = location.state?.registered === true;
    const from = safeInternalPath(location.state?.from);

    useEffect(() => {
        if (justRegistered && !registrationMessageShown.current) {
            registrationMessageShown.current = true;
            messagePopup.success(
                'Registration successful. Please check your email to verify your account.',
                'Account created',
            );
        }
    }, [justRegistered, messagePopup]);

    function validate() {
        const e: typeof errors = {};
        if (!email.trim()) e.email = 'Email is required';
        else if (!isValidEmail(email)) e.email = 'Enter a valid email';
        if (!password) e.password = 'Password is required';
        setErrors(e);
        return Object.keys(e).length === 0;
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!validate()) return;
        setLoading(true);
        try {
            const data = await authService.login({ email, password, rememberMe });
            replaceToken(data.token, data.user);
            navigate('/company-selection', { replace: true, state: from ? { from } : undefined });
        } catch (err) {
            messagePopup.error(
                err instanceof ApiError
                    ? err.message
                    : 'Unable to connect to the server. Please try again.',
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <img className="auth-logo" src="/qpoos-logo.png" alt="Qpoos" />
                <h1 className="auth-title">Sign in</h1>
                <p className="auth-subtitle">Welcome back to QpoosERP</p>

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    {/* Email */}
                    <div className="auth-field">
                        <label className="auth-label" htmlFor="login-email">Email</label>
                        <div className="auth-input-wrap">
                            <input
                                id="login-email"
                                type="email"
                                className={`auth-input${errors.email ? ' has-error' : ''}`}
                                placeholder="you@company.com"
                                value={email}
                                autoComplete="email"
                                autoFocus
                                onChange={e => {
                                    setEmail(e.target.value);
                                    if (errors.email) setErrors(p => ({ ...p, email: undefined }));
                                }}
                            />
                        </div>
                        {errors.email && <p className="auth-field-error">{errors.email}</p>}
                    </div>

                    {/* Password */}
                    <div className="auth-field">
                        <label className="auth-label" htmlFor="login-password">Password</label>
                        <div className="auth-input-wrap">
                            <input
                                id="login-password"
                                type={showPassword ? 'text' : 'password'}
                                className={`auth-input${errors.password ? ' has-error' : ''}`}
                                placeholder="••••••••"
                                value={password}
                                autoComplete="current-password"
                                onChange={e => {
                                    setPassword(e.target.value);
                                    if (errors.password) setErrors(p => ({ ...p, password: undefined }));
                                }}
                            />
                            <button
                                type="button"
                                className="auth-input-icon"
                                onClick={() => setShowPassword(p => !p)}
                                aria-label={showPassword ? 'Hide password' : 'Show password'}
                            >
                                {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                            </button>
                        </div>
                        {errors.password && <p className="auth-field-error">{errors.password}</p>}
                    </div>

                    {/* Remember me + Forgot password */}
                    <div className="auth-row">
                        <label className="auth-checkbox-label">
                            <input
                                type="checkbox"
                                checked={rememberMe}
                                onChange={e => setRememberMe(e.target.checked)}
                            />
                            Remember me
                        </label>
                        <Link to="/forgot-password" className="auth-link">Forgot password?</Link>
                    </div>

                    <button type="submit" className="auth-submit-btn" disabled={loading}>
                        {loading ? <><Spinner white />Signing in…</> : 'Sign in'}
                    </button>
                </form>

                <p className="auth-footer">
                    Don't have an account?{' '}
                    <Link to="/register" className="auth-link">Create one</Link>
                </p>
            </div>
        </div>
    );
}
