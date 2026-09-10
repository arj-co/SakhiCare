import React from "react";
import type { UserProfile } from "../api";
import { 
  HeartHandshake, 
  Radio, 
  ShieldAlert, 
  Hospital, 
  Users, 
  BookOpen, 
  UserCheck,
  Truck,
  Bell
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
}) => {
  return (
    <header className="glass-panel" style={{ margin: "16px 24px", padding: "12px 24px", position: "sticky", top: "16px", zIndex: 100 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "16px" }}>
        
        {/* Brand & Mission */}
        <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
          <div style={{ 
            width: "44px", 
            height: "44px", 
            borderRadius: "12px", 
            background: "linear-gradient(135deg, #ef4444 0%, #dc2626 100%)",
            display: "flex", 
            alignItems: "center", 
            justifyContent: "center",
            boxShadow: "0 4px 16px rgba(239, 68, 68, 0.4)"
          }}>
            <HeartHandshake size={26} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <h1 style={{ fontSize: "1.35rem", fontWeight: 800, color: "#ffffff", letterSpacing: "-0.02em" }}>
                SakhiCare
              </h1>
              <span style={{ 
                fontSize: "0.7rem", 
                background: "rgba(14, 165, 233, 0.15)", 
                color: "#38bdf8", 
                padding: "2px 8px", 
                borderRadius: "999px",
                border: "1px solid rgba(56, 189, 248, 0.3)",
                fontWeight: 600
              }}>
                Care Desk Hub
              </span>
            </div>
            <p style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "2px" }}>
              Maternal Danger-Sign Triage & Emergency Response System • MoHFW v1.0
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav style={{ display: "flex", alignItems: "center", gap: "6px", background: "rgba(0, 0, 0, 0.25)", padding: "4px", borderRadius: "10px", border: "1px solid var(--border-subtle)" }}>
          <button
            onClick={() => onSelectTab("queue")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "queue" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "queue" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "queue" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <ShieldAlert size={16} color={criticalCount > 0 ? "var(--risk-red)" : "inherit"} />
            <span>Urgent Queue</span>
            {criticalCount > 0 && (
              <span style={{
                background: "var(--risk-red)",
                color: "#ffffff",
                fontSize: "0.7rem",
                fontWeight: 800,
                padding: "1px 6px",
                borderRadius: "999px",
                boxShadow: "0 0 8px rgba(239, 68, 68, 0.6)"
              }}>
                {criticalCount}
              </span>
            )}
          </button>

          <button
            onClick={() => onSelectTab("facilities")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "facilities" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "facilities" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "facilities" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Hospital size={16} />
            <span>Facilities</span>
          </button>

          <button
            onClick={() => onSelectTab("workers")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "workers" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "workers" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "workers" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Users size={16} />
            <span>ASHA Roster</span>
          </button>

          <button
            onClick={() => onSelectTab("protocols")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "protocols" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "protocols" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "protocols" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <BookOpen size={16} />
            <span>Protocols</span>
          </button>

          <button
            onClick={() => onSelectTab("transport")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "transport" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "transport" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "transport" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Truck size={16} color={currentTab === "transport" ? "#38bdf8" : "inherit"} />
            <span>108 Transport</span>
          </button>

          <button
            onClick={() => onSelectTab("notifications")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "none",
              background: currentTab === "notifications" ? "var(--bg-surface-elevated)" : "transparent",
              color: currentTab === "notifications" ? "#ffffff" : "var(--text-secondary)",
              fontWeight: currentTab === "notifications" ? 600 : 500,
              fontSize: "0.85rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Bell size={16} color={currentTab === "notifications" ? "#fbbf24" : "inherit"} />
            <span>Escalations & SMS</span>
          </button>
        </nav>

        {/* Live SSE Indicator & Role Switcher */}
        <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
          
          {/* Live Status Beacon */}
          <div style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            background: isLive ? "rgba(16, 185, 129, 0.1)" : "rgba(239, 68, 68, 0.1)",
            border: `1px solid ${isLive ? "rgba(16, 185, 129, 0.3)" : "rgba(239, 68, 68, 0.3)"}`,
            padding: "5px 12px",
            borderRadius: "999px",
            fontSize: "0.75rem",
            fontWeight: 600,
            color: isLive ? "#6ee7b7" : "#fca5a5"
          }}>
            <span style={{
              width: "8px",
              height: "8px",
              borderRadius: "50%",
              backgroundColor: isLive ? "#10b981" : "#ef4444",
              display: "inline-block"
            }} className={isLive ? "animate-pulse-beacon" : ""} />
            <Radio size={14} />
            <span>{isLive ? "Live Stream Active" : "Reconnecting..."}</span>
          </div>

          {/* Active Operator Role Switcher */}
          <div style={{ 
            display: "flex", 
            alignItems: "center", 
            gap: "8px", 
            background: "rgba(255, 255, 255, 0.04)", 
            padding: "4px 8px", 
            borderRadius: "10px",
            border: "1px solid var(--border-subtle)" 
          }}>
            <UserCheck size={16} color="#38bdf8" />
            <div style={{ textAlign: "right" }}>
              <div style={{ fontSize: "0.75rem", fontWeight: 700, color: "#ffffff" }}>
                {currentUser.full_name}
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "4px", justifyContent: "flex-end" }}>
                <span style={{ fontSize: "0.65rem", color: "var(--text-muted)" }}>Role:</span>
                <select
                  value={currentUser.role}
                  onChange={(e) => onChangeRole(e.target.value as UserProfile["role"])}
                  style={{
                    background: "transparent",
                    border: "none",
                    color: "#38bdf8",
                    fontSize: "0.7rem",
                    fontWeight: 700,
                    padding: "0",
                    cursor: "pointer"
                  }}
                >
                  <option value="MEDICAL_OFFICER" style={{ background: "#0f172a" }}>Medical Officer (Dr. Sharma)</option>
                  <option value="SUPERVISOR" style={{ background: "#0f172a" }}>Supervisor (Anita Kumari)</option>
                  <option value="DISPATCHER" style={{ background: "#0f172a" }}>108 Dispatcher (Vikram Singh)</option>
                  <option value="ADMIN" style={{ background: "#0f172a" }}>System Administrator</option>
                </select>
              </div>
            </div>
          </div>

        </div>

      </div>
    </header>
  );
};
