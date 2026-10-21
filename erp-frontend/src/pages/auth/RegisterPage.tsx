import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, CheckCircle } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { authService, ApiError } from "@/services/authService";
import { Spinner } from "@/components/ui/Spinner";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import "./auth.css";

interface FormFields {
  email: string;
  password: string;
  confirmPassword: string;
  mobile: string;
}
type FormErrors = Partial<FormFields>;

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidMobile(mobile: string) {
  return /^[0-9+\- ]{7,15}$/.test(mobile.trim());
}

const STRENGTH_LEVELS = [
  { label: "Weak", color: "#ef4444" },
  { label: "Fair", color: "#f59e0b" },
  { label: "Good", color: "#3b82f6" },
  { label: "Strong", color: "#10b981" },
] as const;

function getPasswordStrength(pwd: string) {
  let score = 0;
  if (pwd.length >= 8) score++;
  if (pwd.length >= 12) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  if (score <= 1) return STRENGTH_LEVELS[0];
  if (score === 2) return STRENGTH_LEVELS[1];
  if (score === 3) return STRENGTH_LEVELS[2];
  return STRENGTH_LEVELS[3];
}

export function RegisterPage() {
  const navigate = useNavigate();
  const { replaceToken } = useAuth();
  const messagePopup = useMessagePopup();

  const [fields, setFields] = useState<FormFields>({
    email: "",
    password: "",
    confirmPassword: "",
    mobile: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});

  const strength = fields.password
    ? getPasswordStrength(fields.password)
    : null;
  const passwordsMatch =
    !!fields.confirmPassword && fields.password === fields.confirmPassword;

  function update(key: keyof FormFields, value: string) {
    setFields((prev) => ({ ...prev, [key]: value }));
    if (errors[key]) setErrors((prev) => ({ ...prev, [key]: undefined }));
  }

  function validate(): boolean {
    const e: FormErrors = {};
    if (!fields.email.trim()) e.email = "Email is required";
    else if (!isValidEmail(fields.email)) e.email = "Enter a valid email";
    if (!fields.mobile.trim()) e.mobile = "Mobile number is required";
    else if (!isValidMobile(fields.mobile))
      e.mobile = "Enter a valid mobile number";
    if (!fields.password) e.password = "Password is required";
    else if (fields.password.length < 8) e.password = "Minimum 8 characters";
    if (!fields.confirmPassword)
      e.confirmPassword = "Please confirm your password";
    else if (fields.password !== fields.confirmPassword)
      e.confirmPassword = "Passwords do not match";
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const data = await authService.register({
        email: fields.email.trim(),
        password: fields.password,
        mobile: fields.mobile.trim(),
      });
      if (data.token && data.user) {
        replaceToken(data.token, data.user);
        navigate("/", { replace: true });
      } else {
        navigate("/login", { state: { registered: true } });
      }
    } catch (err) {
      messagePopup.error(
        err instanceof ApiError
          ? err.message
          : "Unable to connect to the server. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <img className="auth-logo" src="/qpoos-logo.png" alt="Qpoos" />
        <h1 className="auth-title">Create account</h1>
        <p className="auth-subtitle">Get started with QpoosERP</p>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          {/* Email */}
          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-email">
              Email
            </label>
            <div className="auth-input-wrap">
              <input
                id="reg-email"
                type="email"
                className={`auth-input${errors.email ? " has-error" : ""}`}
                placeholder="you@company.com"
                value={fields.email}
                autoComplete="email"
                autoFocus
                onChange={(e) => update("email", e.target.value)}
              />
            </div>
            {errors.email && <p className="auth-field-error">{errors.email}</p>}
          </div>

          {/* Mobile */}
          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-mobile">
              Mobile number
            </label>
            <div className="auth-input-wrap">
              <input
                id="reg-mobile"
                type="tel"
                className={`auth-input${errors.mobile ? " has-error" : ""}`}
                placeholder="+91 98765 43210"
                value={fields.mobile}
                autoComplete="tel"
                onChange={(e) => update("mobile", e.target.value)}
              />
            </div>
            {errors.mobile && (
              <p className="auth-field-error">{errors.mobile}</p>
            )}
          </div>

          {/* Password */}
          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-password">
              Password
            </label>
            <div className="auth-input-wrap">
              <input
                id="reg-password"
                type={showPassword ? "text" : "password"}
                className={`auth-input${errors.password ? " has-error" : ""}`}
                placeholder="Min. 8 characters"
                value={fields.password}
                autoComplete="new-password"
                onChange={(e) => update("password", e.target.value)}
              />
              <button
                type="button"
                className="auth-input-icon"
                onClick={() => setShowPassword((p) => !p)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>

            {fields.password && strength && (
              <div className="strength-bar-wrap">
                <div className="strength-bars">
                  {STRENGTH_LEVELS.map((s, i) => (
                    <div
                      key={s.label}
                      className="strength-bar-seg"
                      style={{
                        background:
                          STRENGTH_LEVELS.indexOf(strength) >= i
                            ? strength.color
                            : undefined,
                      }}
                    />
                  ))}
                </div>
                <span
                  className="strength-label"
                  style={{ color: strength.color }}
                >
                  {strength.label} password
                </span>
              </div>
            )}

            {errors.password && (
              <p className="auth-field-error">{errors.password}</p>
            )}
          </div>

          {/* Confirm password */}
          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-confirm">
              Confirm password
            </label>
            <div className="auth-input-wrap">
              <input
                id="reg-confirm"
                type={showConfirm ? "text" : "password"}
                className={`auth-input${errors.confirmPassword ? " has-error" : ""}`}
                placeholder="Re-enter your password"
                value={fields.confirmPassword}
                autoComplete="new-password"
                onChange={(e) => update("confirmPassword", e.target.value)}
              />
              <button
                type="button"
                className="auth-input-icon"
                onClick={() => setShowConfirm((p) => !p)}
                aria-label={
                  showConfirm
                    ? "Hide confirm password"
                    : "Show confirm password"
                }
              >
                {passwordsMatch ? (
                  <CheckCircle size={15} style={{ color: "#10b981" }} />
                ) : showConfirm ? (
                  <EyeOff size={15} />
                ) : (
                  <Eye size={15} />
                )}
              </button>
            </div>
            {errors.confirmPassword && (
              <p className="auth-field-error">{errors.confirmPassword}</p>
            )}
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading}>
            {loading ? (
              <>
                <Spinner white />
                Creating account…
              </>
            ) : (
              "Create account"
            )}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account?{" "}
          <Link to="/login" className="auth-link">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
