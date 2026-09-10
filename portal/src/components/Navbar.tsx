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
    <header style={{
      margin: "20px 28px 0 28px",
      padding: "16px 24px",
      background: "var(--panel-white)",
      border: "1px solid var(--rule-muted)",
      borderRadius: "var(--radius-lg)",
      boxShadow: "var(--shadow-sm)",
      position: "sticky",
      top: "16px",
      zIndex: 100
    }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "18px" }}>
        
        {/* Brand & Editorial Position */}
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ 
            width: "42px", 
            height: "42px", 
            borderRadius: "12px", 
            background: "var(--navy-deep)",
            display: "flex", 
            alignItems: "center", 
            justifyContent: "center",
            boxShadow: "0 3px 10px rgba(12, 30, 105, 0.15)"
          }}>
            <HeartHandshake size={22} color="#A8D4CF" />
          </div>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <span style={{ fontSize: "1.3rem", fontWeight: 600, color: "var(--navy-deep)", letterSpacing: "-0.02em" }}>
                SakhiCare
              </span>
              <span style={{ 
                fontSize: "0.68rem", 
                fontFamily: "var(--font-mono)",
                background: "var(--panel-pale)", 
                color: "var(--navy-deep)", 
                padding: "3px 10px", 
                borderRadius: "var(--radius-pill)",
                border: "1px solid var(--rule-muted)",
                letterSpacing: "0.14em",
                fontWeight: 600
              }}>
                CARE DESK · MOHFW V1.0
              </span>
            </div>
            <p style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "2px" }}>
              Maternal Danger-Sign Triage & Rural Emergency Response System
            </p>
          </div>
        </div>

        {/* Editorial Navigation Tabs */}
        <nav style={{ 
          display: "flex", 
          alignItems: "center", 
          gap: "4px", 
          background: "var(--panel-pale)", 
          padding: "4px", 
          borderRadius: "var(--radius-pill)", 
          border: "1px solid var(--rule-muted)" 
        }}>
          <button
            onClick={() => onSelectTab("queue")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "queue" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "queue" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "queue" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <ShieldAlert size={15} color={currentTab === "queue" ? "#A8D4CF" : (criticalCount > 0 ? "var(--coral-dark)" : "inherit")} />
            <span>Urgent Queue</span>
            {criticalCount > 0 && (
              <span style={{
                background: currentTab === "queue" ? "var(--coral-accent)" : "var(--coral-dark)",
                color: "#FFFFFF",
                fontSize: "0.68rem",
                fontWeight: 700,
                padding: "1px 7px",
                borderRadius: "999px"
              }}>
                {criticalCount}
              </span>
            )}
          </button>

          <button
            onClick={() => onSelectTab("transport")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "transport" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "transport" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "transport" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Truck size={15} color={currentTab === "transport" ? "#A8D4CF" : "inherit"} />
            <span>108 Transport</span>
          </button>

          <button
            onClick={() => onSelectTab("notifications")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "notifications" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "notifications" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "notifications" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Bell size={15} color={currentTab === "notifications" ? "#A8D4CF" : "inherit"} />
            <span>Escalations & SMS</span>
          </button>

          <button
            onClick={() => onSelectTab("facilities")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "facilities" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "facilities" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "facilities" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Hospital size={15} color={currentTab === "facilities" ? "#A8D4CF" : "inherit"} />
            <span>Facilities</span>
          </button>

          <button
            onClick={() => onSelectTab("workers")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "workers" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "workers" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "workers" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <Users size={15} color={currentTab === "workers" ? "#A8D4CF" : "inherit"} />
            <span>ASHA Roster</span>
          </button>

          <button
            onClick={() => onSelectTab("protocols")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "7px 16px",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: currentTab === "protocols" ? "var(--navy-deep)" : "transparent",
              color: currentTab === "protocols" ? "#FFFFFF" : "var(--navy-deep)",
              fontWeight: currentTab === "protocols" ? 600 : 500,
              fontSize: "0.825rem",
              cursor: "pointer",
              transition: "all 0.2s ease"
            }}
          >
            <BookOpen size={15} color={currentTab === "protocols" ? "#A8D4CF" : "inherit"} />
            <span>Protocols</span>
          </button>
        </nav>

        {/* Live SSE Status & Role Chip */}
        <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
          
          {/* Calm Signal Indicator */}
          <div style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            background: isLive ? "var(--seafoam-bg)" : "var(--coral-bg)",
            border: `1px solid ${isLive ? "rgba(31, 95, 88, 0.3)" : "rgba(226, 123, 112, 0.4)"}`,
            padding: "6px 14px",
            borderRadius: "var(--radius-pill)",
            fontSize: "0.75rem",
            fontWeight: 600,
            color: isLive ? "var(--seafoam-dark)" : "var(--coral-dark)"
          }}>
            <span style={{
              width: "7px",
              height: "7px",
              borderRadius: "50%",
              backgroundColor: isLive ? "var(--seafoam-dark)" : "var(--coral-dark)",
              display: "inline-block"
            }} className="animate-pulse-beacon" />
            <Radio size={13} />
            <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", letterSpacing: "0.06em" }}>
              {isLive ? "LIVE SIGNAL" : "RECONNECTING"}
            </span>
          </div>

          {/* Active Role Selector Chip */}
          <div style={{ 
            display: "flex", 
            alignItems: "center", 
            gap: "8px", 
            background: "var(--panel-pale)", 
            padding: "5px 12px", 
            borderRadius: "var(--radius-pill)",
            border: "1px solid var(--rule-muted)" 
          }}>
            <UserCheck size={15} color="var(--navy-deep)" />
            <div style={{ textAlign: "right" }}>
              <select
                value={currentUser.role}
                onChange={(e) => onChangeRole(e.target.value as UserProfile["role"])}
                style={{
                  background: "transparent",
                  border: "none",
                  color: "var(--navy-deep)",
                  fontSize: "0.75rem",
                  fontWeight: 600,
                  padding: "0",
                  cursor: "pointer",
                  fontFamily: "var(--font-main)"
                }}
              >
                <option value="MEDICAL_OFFICER">Dr. Rajiv Sharma (MO)</option>
                <option value="SUPERVISOR">Anita Kumari (Supervisor)</option>
                <option value="DISPATCHER">Vikram Singh (108 Dispatch)</option>
                <option value="ADMIN">System Administrator</option>
              </select>
            </div>
          </div>

        </div>

      </div>
    </header>
  );
};
