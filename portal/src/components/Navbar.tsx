import React from "react";
import type { UserProfile } from "../api";
import { 
  HeartHandshake, 
  ShieldAlert, 
  Hospital, 
  Users, 
  BookOpen, 
  UserCheck,
  Truck,
  Bell,
  RefreshCw
} from "lucide-react";

interface NavbarProps {
  currentTab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications";
  onSelectTab: (tab: "queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications") => void;
  currentUser: UserProfile;
  onChangeRole: (role: UserProfile["role"]) => void;
  criticalCount: number;
  totalCases: number;
  isLive: boolean;
  onRefresh: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onSelectTab,
  currentUser,
  onChangeRole,
  criticalCount,
  isLive,
  onRefresh,
}) => {
  return (
    <header className="navbar">
      <div className="navbar-inner">
        {/* Brand */}
        <div className="brand-section" onClick={() => onSelectTab("queue")}>
          <div className="brand-icon">
            <HeartHandshake size={24} />
          </div>
          <div>
            <div style={{ display: "flex", alignItems: "center" }}>
              <span className="brand-title">SakhiCare</span>
              <span className="brand-tag">Care Desk</span>
            </div>
            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "1px" }}>
              Maternal Health Triage & 108 Dispatch Hub
            </div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="nav-links">
          <button
            className={`nav-btn ${currentTab === "queue" ? "active" : ""}`}
            onClick={() => onSelectTab("queue")}
          >
            <ShieldAlert size={16} />
            <span>Urgent Queue</span>
            {criticalCount > 0 && (
              <span className="badge badge-red" style={{ padding: "1px 6px", fontSize: "0.7rem" }}>
                {criticalCount}
              </span>
            )}
          </button>

          <button
            className={`nav-btn ${currentTab === "transport" ? "active" : ""}`}
            onClick={() => onSelectTab("transport")}
          >
            <Truck size={16} />
            <span>108 Transport</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "notifications" ? "active" : ""}`}
            onClick={() => onSelectTab("notifications")}
          >
            <Bell size={16} />
            <span>Notifications</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "facilities" ? "active" : ""}`}
            onClick={() => onSelectTab("facilities")}
          >
            <Hospital size={16} />
            <span>Facilities</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "workers" ? "active" : ""}`}
            onClick={() => onSelectTab("workers")}
          >
            <Users size={16} />
            <span>ASHA Workers</span>
          </button>

          <button
            className={`nav-btn ${currentTab === "protocols" ? "active" : ""}`}
            onClick={() => onSelectTab("protocols")}
          >
            <BookOpen size={16} />
            <span>Protocols</span>
          </button>
        </nav>

        {/* Status & Controls */}
        <div className="nav-right">
          <div className="live-indicator">
            <span className="live-dot"></span>
            <span>{isLive ? "Live Sync" : "Connecting..."}</span>
          </div>

          <button
            className="btn btn-secondary btn-sm"
            onClick={onRefresh}
            title="Refresh active data"
            style={{ padding: "6px 10px" }}
          >
            <RefreshCw size={14} />
          </button>

          <div className="role-badge">
            <UserCheck size={14} color="var(--primary-blue)" />
            <select
              value={currentUser.role}
              onChange={(e) => onChangeRole(e.target.value as UserProfile["role"])}
              className="role-select"
              title="Switch user role"
            >
              <option value="MEDICAL_OFFICER">Medical Officer</option>
              <option value="SUPERVISOR">Block Supervisor</option>
              <option value="DISPATCHER">108 Dispatcher</option>
              <option value="ADMIN">System Admin</option>
            </select>
          </div>
        </div>
      </div>
    </header>
  );
};
