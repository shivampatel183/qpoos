import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  SlidersHorizontal,
  ChevronDown,
  Gem,
  PanelLeftOpen,
  PanelLeftClose,
} from "lucide-react";
import { primaryTopItems, secondaryNavItems } from "./sidebarData";
import { Header } from "../header/header";
import "./sidebar.css";

export function Sidebar({ children }: { children?: React.ReactNode }) {
  const [isSecondaryOpen, setIsSecondaryOpen] = useState(false);
  const [expandedIds, setExpandedIds] = useState<string[]>(["accounting"]);
  const navigate = useNavigate();
  const location = useLocation();

  function toggleSecondaryItem(id: string) {
    setExpandedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    );
  }

  const primaryActions: Record<string, () => void> = {
    TOGGLE_APPS: () => setIsSecondaryOpen((prev) => !prev),
    CREATE_ACTION: () => {
      /* TODO: open create modal */
    },
    GOTO_BOOKMARKS: () => {
      /* TODO: navigate to bookmarks */
    },
    GOTO_HOME: () => navigate("/"),
    GOTO_REPORTS: () => navigate("/reports"),
  };

  function isPrimaryActive(id: string): boolean {
    if (id === "home") return location.pathname === "/";
    if (id === "reports") return location.pathname.startsWith("/reports");
    if (id === "apps") return isSecondaryOpen;
    return false;
  }

  function isSecondaryActive(
    itemPath: string,
    childPaths: string[] = [],
  ): boolean {
    return (
      location.pathname === itemPath ||
      childPaths.some((path) => location.pathname === path)
    );
  }

  return (
    <div className="sidebar-container">
      {/* Primary sidebar — narrow icon strip */}
      <nav className="sidebar-primary">
        <img
          className="primary-logo"
          src="/qpoos-logo.png"
          alt="Qpoos"
        />

        <div className="primary-nav">
          {primaryTopItems.map((item) => (
            <button
              key={item.id}
              className={[
                "primary-nav-item",
                item.isButton ? "primary-nav-item--action" : "",
                item.id === "create" ? "primary-nav-item--neutral" : "",
                isPrimaryActive(item.id) ? "active" : "",
              ]
                .filter(Boolean)
                .join(" ")}
              onClick={
                item.actionName ? primaryActions[item.actionName] : undefined
              }
            >
              <div className="icon-wrapper">{item.icon}</div>
              <span>{item.label}</span>
            </button>
          ))}
        </div>

        <div className="primary-nav-divider" />
        <div className="primary-nav-section-title">PINNED</div>

        <div className="primary-bottom-section">
          {/* <button className="primary-nav-item">
                        <div className="more-button">
                            <MoreHorizontal size={24} />
                        </div>
                        <span>More</span>
                    </button> */}

          <button className="primary-nav-item primary-nav-item--neutral primary-nav-item--last">
            <div className="icon-wrapper">
              <SlidersHorizontal size={22} strokeWidth={1.5} />
            </div>
            <span>Customize</span>
          </button>
        </div>
      </nav>

      {/* Main area: header + secondary sidebar + page content */}
      <div className="sidebar-body">
        <Header />

        <div className="sidebar-body-inner">
          {/* Secondary sidebar — expandable nav panel */}
          <nav
            className={`sidebar-secondary${!isSecondaryOpen ? " collapsed" : ""}`}
          >
            <div className="secondary-nav-header">
              <span>ALL APPS</span>
              <button
                className="secondary-toggle-btn"
                onClick={() => setIsSecondaryOpen(false)}
                title="Collapse sidebar"
              >
                <PanelLeftClose size={18} />
              </button>
            </div>

            <ul className="secondary-nav-list">
              {secondaryNavItems.map((item) => {
                const hasChildren = !!item.children?.length;
                const isExpanded = expandedIds.includes(item.id);
                const childPaths =
                  item.children?.map((child) => child.path) ?? [];
                const isActive = isSecondaryActive(item.path, childPaths);
                return (
                  <li key={item.id}>
                    <div
                      className={`secondary-nav-item${isActive ? " active" : ""}`}
                      onClick={() =>
                        hasChildren
                          ? toggleSecondaryItem(item.id)
                          : navigate(item.path)
                      }
                    >
                      <div className="secondary-item-icon">{item.icon}</div>
                      <span className="secondary-item-label">{item.label}</span>
                      <div className="secondary-item-trailing">
                        {hasChildren && (
                          <ChevronDown
                            size={16}
                            className={`submenu-chevron${isExpanded ? " rotated" : ""}`}
                          />
                        )}
                        {item.isPremium && <Gem size={14} />}
                      </div>
                    </div>

                    {hasChildren && (
                      <ul
                        className={`submenu-list${isExpanded ? " open" : ""}`}
                      >
                        {item.children!.map((child) => (
                          <li
                            key={child.id}
                            className={`submenu-item${location.pathname === child.path ? " active" : ""}`}
                            onClick={() => navigate(child.path)}
                          >
                            {child.label}
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                );
              })}
            </ul>
          </nav>

          {/* Page content */}
          <main className="sidebar-main">
            {!isSecondaryOpen && (
              <button
                className="expand-sidebar-btn"
                onClick={() => setIsSecondaryOpen(true)}
                title="Expand sidebar"
              >
                <PanelLeftOpen size={18} />
              </button>
            )}
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}
