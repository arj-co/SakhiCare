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
  RefreshCw
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
        {/* Eyra Brand Header */}
        <div className="brand-section" onClick={() => onSelectTab("queue")}>
          <div className="flex items-center gap-2">
            <span className="brand-title">SakhiCare</span>
            <span className="brand-badge">Care Desk</span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="nav-links">
          <button
            className={`nav-btn ${currentTab === "queue" ? "active" : ""}`}
            onClick={() => onSelectTab("queue")}
          >
            <ShieldAlert size={15} />
            <span>Patients Queue</span>
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

        {/* Status & Role Controls */}
        <div className="nav-right">
          {/* Eyra-style Live Status Indicator */}
          <div className="live-indicator">
            <span className="live-dot"></span>
            <span>{isLive ? "Live Sync: Ready" : "Syncing..."}</span>
          </div>

          <button
            className="btn btn-secondary btn-sm"
            onClick={onRefresh}
            title="Refresh active patient records"
            style={{ padding: "6px 12px" }}
          >
            <RefreshCw size={13} />
          </button>

          {/* Authenticated profile */}
          <div className="role-badge">
            <UserCheck size={14} color="var(--medical-teal)" />
            <span>{currentUser.full_name || currentUser.username}</span>
            <span className="role-select">{currentUser.role.replaceAll("_", " ")}</span>
            <button className="btn btn-secondary btn-sm" onClick={onLogout}>Sign out</button>
          </div>
        </div>
      </div>
    </header>
  );
};
