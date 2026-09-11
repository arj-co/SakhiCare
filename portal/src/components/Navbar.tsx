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

interface NavbarProps {
  currentTab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications";
  onSelectTab: (tab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications") => void;
  currentUser: UserProfile;
  onLogout: () => void;
  criticalCount: number;
  isLive: boolean;
  onRefresh: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onSelectTab,
  currentUser,
  onLogout,
  criticalCount,
  isLive,
  onRefresh,
}) => {
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
            <span>{isLive ? "Live sync" : "Connecting"}</span>
          </div>

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
          <span className="nav-group-label">Workspace</span>
          <button
            className={`nav-btn ${currentTab === "queue" ? "active" : ""}`}
            onClick={() => onSelectTab("queue")}
          >
            <ShieldAlert size={15} />
            <span>Patient queue</span>
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
            <span>108 transport</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "notifications" ? "active" : ""}`}
            onClick={() => onSelectTab("notifications")}
          >
            <Bell size={15} />
            <span>Notifications</span>
          </button>
        </nav>

        <nav className="nav-links nav-links-admin" aria-label="Directory and guidance navigation">
          <span className="nav-group-label">Directory &amp; guidance</span>
          <button
            className={`nav-btn ${currentTab === "facilities" ? "active" : ""}`}
            onClick={() => onSelectTab("facilities")}
          >
            <Hospital size={15} />
            <span>Facilities</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "workers" ? "active" : ""}`}
            onClick={() => onSelectTab("workers")}
          >
            <Users size={15} />
            <span>ASHA workers</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "protocols" ? "active" : ""}`}
            onClick={() => onSelectTab("protocols")}
          >
            <BookOpen size={15} />
            <span>Protocols</span>
          </button>
        </nav>
        <div className="nav-purpose"><span className="nav-purpose-dot" /> Triage workspace</div>
      </div>
    </header>
  );
};
