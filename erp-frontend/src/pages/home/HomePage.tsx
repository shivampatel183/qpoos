import { TrendingUp, Users, DollarSign, ShoppingBag } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useCompany } from "@/context/CompanyContext";
import "./HomePage.css";

const STATS = [
  { label: "Total Revenue", value: "—", icon: DollarSign, color: "#2563eb" },
  { label: "Total Expenses", value: "—", icon: TrendingUp, color: "#dc2626" },
  { label: "Customers", value: "—", icon: Users, color: "#059669" },
  { label: "Orders", value: "—", icon: ShoppingBag, color: "#d97706" },
] as const;

export function HomePage() {
  const { user } = useAuth();
  const { currentCompany } = useCompany();

  return (
    <div className="home-page">
      <div className="home-header">
        <h1 className="home-title">Welcome back, {user?.name ?? "there"}</h1>
        <p className="home-subtitle">{currentCompany?.name}</p>
      </div>

      <div className="home-stats">
        {STATS.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="stat-card">
            <div className="stat-icon" style={{ background: color }}>
              <Icon size={20} color="white" />
            </div>
            <div className="stat-body">
              <p className="stat-label">{label}</p>
              <p className="stat-value">{value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="home-empty">Connect your backend to see live data.</div>
    </div>
  );
}
