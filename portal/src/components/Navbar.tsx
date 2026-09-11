import React from "react";
import type { UserProfile } from "../api";
import { 
  ShieldAlert, 
  Hospital, 
  Users, 
  BookOpen, 
  UserCheck,
  Truck,
  Bell,
  RefreshCw,
  LogOut
} from "lucide-react";

interface NavbarProps {
  currentTab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications";
  onSelectTab: (tab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications") => void;
  currentUser: UserProfile;
  onLogout: () => void;
  criticalCount: number;
  totalCases: number;
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
      <div className="navbar-inner">
        {/* Brand Header */}
        <div className="brand-section" onClick={() => onSelectTab("queue")}>
          <span className="brand-title">SakhiCare</span>
          <span className="brand-badge">Care Desk</span>
        </div>

        {/* Navigation Links */}
        <nav className="nav-links">
          <button
            className={`nav-btn ${currentTab === "queue" ? "active" : ""}`}
            onClick={() => onSelectTab("queue")}
          >
            <ShieldAlert size={15} />
            <span>Patients Queue</span>
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
            <span>108 Transport</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "notifications" ? "active" : ""}`}
            onClick={() => onSelectTab("notifications")}
          >
            <Bell size={15} />
            <span>Notifications</span>
          </button>

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
            <span>ASHA Workers</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "protocols" ? "active" : ""}`}
            onClick={() => onSelectTab("protocols")}
          >
            <BookOpen size={15} />
            <span>Protocols</span>
          </button>
        </nav>

        {/* Status & Profile Controls */}
        <div className="nav-right">
          <div className="live-indicator">
            <span className={`live-dot ${isLive ? "active" : "syncing"}`}></span>
            <span>{isLive ? "Live Sync" : "Syncing"}</span>
          </div>

          <button
            className="icon-refresh-btn"
            onClick={onRefresh}
            title="Refresh active records"
          >
            <RefreshCw size={13} />
          </button>

          <div className="user-profile-pill">
            <UserCheck size={14} className="user-icon" />
            <span className="user-name">{currentUser.full_name || currentUser.username}</span>
            <span className="user-role-tag">{currentUser.role.replaceAll("_", " ")}</span>
            <button className="logout-icon-btn" onClick={onLogout} title="Sign Out">
              <LogOut size={13} />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
