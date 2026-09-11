import React from "react";
import type { UserProfile } from "../api";
import {
  ShieldAlert,
  Hospital,
  Users,
  BookOpen,
  Truck,
  Bell,
  RefreshCw,
  LogOut,
  HeartPulse,
} from "lucide-react";
import type { PortalLanguage } from "../i18n";
import { portalCopy } from "../i18n";

interface NavbarProps {
  currentTab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications";
  onSelectTab: (tab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications") => void;
  currentUser: UserProfile;
  onLogout: () => void;
  criticalCount: number;
  isLive: boolean;
  onRefresh: () => void;
  language: PortalLanguage;
  onLanguageChange: (language: PortalLanguage) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onSelectTab,
  currentUser,
  onLogout,
  criticalCount,
  isLive,
  onRefresh,
  language,
  onLanguageChange,
}) => {
  const t = portalCopy[language];
  return (
    <header className="navbar">
      <div className="navbar-top">
        <div className="brand-section" onClick={() => onSelectTab("queue")} role="button" tabIndex={0}>
          <span className="brand-mark"><HeartPulse size={19} strokeWidth={2.4} /></span>
          <span className="brand-title">SakhiCare</span>
          <span className="brand-context">Care Desk <span className="brand-context-separator">/</span> Bihar</span>
        </div>

        <div className="nav-right">
          <div className="live-indicator">
            <span className={`live-dot ${isLive ? "active" : "syncing"}`}></span>
            <span>{isLive ? t.live : t.connecting}</span>
          </div>
          <select className="language-select" value={language} onChange={(e) => onLanguageChange(e.target.value as PortalLanguage)} aria-label="Language"><option value="en">EN · English</option><option value="hi">हि · हिन्दी</option><option value="mr">म · मराठी</option></select>

          <button
            className="icon-refresh-btn"
            onClick={onRefresh}
            title="Refresh active records"
            aria-label="Refresh active records"
          >
            <RefreshCw size={14} />
          </button>

          <div className="user-profile-pill">
            <span className="user-avatar">{(currentUser.full_name || currentUser.username).charAt(0).toUpperCase()}</span>
            <span className="user-details">
              <span className="user-name">{currentUser.full_name || currentUser.username}</span>
              <span className="user-role-tag">{currentUser.role.replaceAll("_", " ")}</span>
            </span>
            <button className="logout-icon-btn" onClick={onLogout} title="Sign Out" aria-label="Sign out">
              <LogOut size={13} />
            </button>
          </div>
        </div>
      </div>

      <div className="navbar-nav">
        <nav className="nav-links" aria-label="Workspace navigation">
          <span className="nav-group-label">{t.workspace}</span>
          <button
            className={`nav-btn ${currentTab === "queue" ? "active" : ""}`}
            onClick={() => onSelectTab("queue")}
          >
            <ShieldAlert size={15} />
            <span>{t.queue}</span>
            {criticalCount > 0 && (
              <span className="nav-badge-red">
                {criticalCount}
              </span>
            )}
          </button>

          <button
            className={`nav-btn ${currentTab === "transport" ? "active" : ""}`}
            onClick={() => onSelectTab("transport")}
          >
            <Truck size={15} />
            <span>{t.transport}</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "notifications" ? "active" : ""}`}
            onClick={() => onSelectTab("notifications")}
          >
            <Bell size={15} />
            <span>{t.notifications}</span>
          </button>
        </nav>

        <nav className="nav-links nav-links-admin" aria-label="Directory and guidance navigation">
          <span className="nav-group-label">{t.directory}</span>
          <button
            className={`nav-btn ${currentTab === "facilities" ? "active" : ""}`}
            onClick={() => onSelectTab("facilities")}
          >
            <Hospital size={15} />
            <span>{t.facilities}</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "workers" ? "active" : ""}`}
            onClick={() => onSelectTab("workers")}
          >
            <Users size={15} />
            <span>{t.workers}</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "protocols" ? "active" : ""}`}
            onClick={() => onSelectTab("protocols")}
          >
            <BookOpen size={15} />
            <span>{t.protocols}</span>
          </button>
        </nav>
        <div className="nav-purpose"><span className="nav-purpose-dot" /> Triage workspace</div>
      </div>
    </header>
  );
};
