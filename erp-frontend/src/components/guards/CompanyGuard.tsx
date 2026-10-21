import { useEffect } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useCompany } from "@/context/CompanyContext";
import { getTokenCompanyId, isTokenExpired } from "@/lib/jwt";
import { Spinner } from "@/components/ui/Spinner";

/**
 * Requires an active company selection (token claim + stored metadata in
 * agreement). Sits inside AuthGuard, so auth is already settled by the time
 * this renders — it only decides between the app and /company-selection.
 */
export function CompanyGuard() {
  const { token, status, logout } = useAuth();
  const { currentCompany, selectCompany } = useCompany();
  const location = useLocation();
  const companyId = getTokenCompanyId(token);
  const tokenInvalid = token !== null && isTokenExpired(token);
  const companyMismatch =
    companyId !== null &&
    currentCompany !== null &&
    currentCompany.id !== companyId;

  useEffect(() => {
    if (tokenInvalid) logout();
  }, [logout, tokenInvalid]);

  useEffect(() => {
    if (companyMismatch) selectCompany(null);
  }, [companyMismatch, selectCompany]);

  if (status === "initializing") {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "100vh",
        }}
      >
        <Spinner size={24} />
      </div>
    );
  }
  if (!token) return <Outlet />;
  if (tokenInvalid) return <Navigate to="/login" replace />;
  if (!companyId || !currentCompany || companyMismatch) {
    return (
      <Navigate
        to="/company-selection"
        replace
        state={{ from: location.pathname + location.search }}
      />
    );
  }

  return <Outlet />;
}
