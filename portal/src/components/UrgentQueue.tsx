import React, { useState, useMemo } from "react";
import type { PregnancyCase } from "../api";
import { 
  AlertOctagon, 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  MapPin, 
  Mic, 
  Truck, 
  Search, 
  RefreshCw, 
  ChevronRight, 
  FileCheck,
  Stethoscope
} from "lucide-react";

interface UrgentQueueProps {
  cases: PregnancyCase[];
  loading: boolean;
  onRefresh: () => void;
  onSelectCase: (caseItem: PregnancyCase) => void;
}

export const UrgentQueue: React.FC<UrgentQueueProps> = ({
  cases,
  loading,
  onRefresh,
  onSelectCase,
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [riskFilter, setRiskFilter] = useState<"ALL" | "RED" | "AMBER" | "GREEN">("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  // Format relative elapsed time
  const formatTimeElapsed = (timestamp?: number) => {
    if (!timestamp) return "Recent";
    const now = Math.floor(Date.now() / 1000);
    const diff = Math.max(0, now - timestamp);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  // Compute KPI metrics
  const stats = useMemo(() => {
    let red = 0;
    let amber = 0;
    let green = 0;
    let dispatched = 0;

    cases.forEach((c) => {
      const risk = c.assessment?.risk_level || "GREEN";
      if (risk === "RED") red++;
      else if (risk === "AMBER") amber++;
      else green++;

      if (c.ambulance_status) dispatched++;
    });

    return { total: cases.length, red, amber, green, dispatched };
  }, [cases]);

  // Filtered cases
  const filteredCases = useMemo(() => {
    return cases.filter((c) => {
      const risk = c.assessment?.risk_level || "GREEN";
      if (riskFilter !== "ALL" && risk !== riskFilter) return false;

      if (statusFilter === "PENDING" && (c.doctor_advisory || c.ambulance_status)) return false;
      if (statusFilter === "ACKNOWLEDGED" && !c.doctor_advisory) return false;
      if (statusFilter === "DISPATCHED" && !c.ambulance_status) return false;

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchName = c.patient_name.toLowerCase().includes(q);
        const matchVillage = c.village.toLowerCase().includes(q);
        const matchId = (c.case_id || c.patient_id || "").toLowerCase().includes(q);
        const matchWorker = (c.worker_id || "").toLowerCase().includes(q);
        if (!matchName && !matchVillage && !matchId && !matchWorker) return false;
      }

      return true;
    });
  }, [cases, riskFilter, statusFilter, searchQuery]);

  return (
    <div style={{ padding: "0 24px 32px 24px" }}>
      
      {/* KPI Metric Summary Row */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "16px", marginBottom: "24px" }}>
        
        {/* Critical RED Cases */}
        <div className="glass-panel" style={{ 
          padding: "16px 20px", 
          borderLeft: "4px solid var(--risk-red)", 
          boxShadow: stats.red > 0 ? "var(--shadow-red-glow)" : "var(--shadow-sm)",
          position: "relative",
          overflow: "hidden"
        }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: "0.8rem", color: "#fca5a5", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Critical Emergencies
            </span>
            <div style={{ 
              width: "28px", 
              height: "28px", 
              borderRadius: "50%", 
              background: "rgba(239, 68, 68, 0.2)", 
              display: "flex", 
              alignItems: "center", 
              justifyContent: "center" 
            }}>
              <AlertOctagon size={16} color="var(--risk-red)" className={stats.red > 0 ? "animate-pulse-red" : ""} />
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "8px", marginTop: "8px" }}>
            <span style={{ fontSize: "2rem", fontWeight: 800, color: "#ffffff", fontFamily: "var(--font-display)" }}>
              {stats.red}
            </span>
            <span style={{ fontSize: "0.75rem", color: "#f87171" }}>
              Requires Immediate 108 Transfer
            </span>
          </div>
        </div>

        {/* Moderate AMBER Cases */}
        <div className="glass-panel" style={{ padding: "16px 20px", borderLeft: "4px solid var(--risk-amber)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: "0.8rem", color: "#fcd34d", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Moderate High-Risk
            </span>
            <div style={{ width: "28px", height: "28px", borderRadius: "50%", background: "rgba(245, 158, 11, 0.2)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <AlertTriangle size={16} color="var(--risk-amber)" />
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "8px", marginTop: "8px" }}>
            <span style={{ fontSize: "2rem", fontWeight: 800, color: "#ffffff", fontFamily: "var(--font-display)" }}>
              {stats.amber}
            </span>
            <span style={{ fontSize: "0.75rem", color: "#fbbf24" }}>
              PHC Doctor Review within 24h
            </span>
          </div>
        </div>

        {/* Normal GREEN Cases */}
        <div className="glass-panel" style={{ padding: "16px 20px", borderLeft: "4px solid var(--risk-green)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: "0.8rem", color: "#6ee7b7", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Routine ANC Care
            </span>
            <div style={{ width: "28px", height: "28px", borderRadius: "50%", background: "rgba(16, 185, 129, 0.2)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <CheckCircle2 size={16} color="var(--risk-green)" />
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "8px", marginTop: "8px" }}>
            <span style={{ fontSize: "2rem", fontWeight: 800, color: "#ffffff", fontFamily: "var(--font-display)" }}>
              {stats.green}
            </span>
            <span style={{ fontSize: "0.75rem", color: "#34d399" }}>
              Stable / Routine Monitoring
            </span>
          </div>
        </div>

        {/* 108 Emergency Transport */}
        <div className="glass-panel" style={{ padding: "16px 20px", borderLeft: "4px solid #38bdf8" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: "0.8rem", color: "#38bdf8", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.05em" }}>
              108 Ambulances Dispatched
            </span>
            <div style={{ width: "28px", height: "28px", borderRadius: "50%", background: "rgba(56, 189, 248, 0.2)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Truck size={16} color="#38bdf8" />
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "8px", marginTop: "8px" }}>
            <span style={{ fontSize: "2rem", fontWeight: 800, color: "#ffffff", fontFamily: "var(--font-display)" }}>
              {stats.dispatched}
            </span>
            <span style={{ fontSize: "0.75rem", color: "#7dd3fc" }}>
              Active Patient Transports
            </span>
          </div>
        </div>

      </div>

      {/* Filter & Controls Bar */}
      <div className="glass-panel" style={{ padding: "14px 20px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "14px" }}>
          
          {/* Search Field */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flex: "1 1 300px", position: "relative" }}>
            <Search size={18} color="var(--text-muted)" style={{ position: "absolute", left: "12px" }} />
            <input
              type="text"
              placeholder="Search by mother's name, village, case ID, or ASHA..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: "100%", paddingLeft: "38px" }}
            />
          </div>

          {/* Risk Level Filter Pills */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", marginRight: "4px" }}>Triage:</span>
            <button
              onClick={() => setRiskFilter("ALL")}
              style={{
                padding: "6px 12px",
                borderRadius: "999px",
                border: "none",
                background: riskFilter === "ALL" ? "var(--bg-surface-elevated)" : "transparent",
                color: riskFilter === "ALL" ? "#ffffff" : "var(--text-muted)",
                fontWeight: 600,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              All ({cases.length})
            </button>
            <button
              onClick={() => setRiskFilter("RED")}
              className={riskFilter === "RED" ? "badge-red" : ""}
              style={{
                padding: "6px 12px",
                borderRadius: "999px",
                border: riskFilter === "RED" ? "1px solid var(--risk-red-border)" : "none",
                background: riskFilter === "RED" ? "var(--risk-red-bg)" : "transparent",
                color: riskFilter === "RED" ? "#fca5a5" : "var(--text-muted)",
                fontWeight: 700,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              RED ({stats.red})
            </button>
            <button
              onClick={() => setRiskFilter("AMBER")}
              style={{
                padding: "6px 12px",
                borderRadius: "999px",
                border: riskFilter === "AMBER" ? "1px solid var(--risk-amber-border)" : "none",
                background: riskFilter === "AMBER" ? "var(--risk-amber-bg)" : "transparent",
                color: riskFilter === "AMBER" ? "#fcd34d" : "var(--text-muted)",
                fontWeight: 700,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              AMBER ({stats.amber})
            </button>
            <button
              onClick={() => setRiskFilter("GREEN")}
              style={{
                padding: "6px 12px",
                borderRadius: "999px",
                border: riskFilter === "GREEN" ? "1px solid var(--risk-green-border)" : "none",
                background: riskFilter === "GREEN" ? "var(--risk-green-bg)" : "transparent",
                color: riskFilter === "GREEN" ? "#6ee7b7" : "var(--text-muted)",
                fontWeight: 700,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              GREEN ({stats.green})
            </button>
          </div>

          {/* Action Status Dropdown & Refresh */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ padding: "6px 12px", fontSize: "0.8rem" }}
            >
              <option value="ALL">All Statuses</option>
              <option value="PENDING">Pending Action</option>
              <option value="ACKNOWLEDGED">Doctor Advisory Issued</option>
              <option value="DISPATCHED">108 Dispatched</option>
            </select>

            <button
              onClick={onRefresh}
              disabled={loading}
              className="btn-outline"
              style={{ padding: "6px 12px", fontSize: "0.8rem" }}
              title="Refresh queue"
            >
              <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
              <span>Refresh</span>
            </button>
          </div>

        </div>
      </div>

      {/* Cases List */}
      <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        {filteredCases.length === 0 ? (
          <div className="glass-panel" style={{ padding: "48px 24px", textAlign: "center" }}>
            <FileCheck size={48} color="var(--text-muted)" style={{ margin: "0 auto 12px auto" }} />
            <h3 style={{ fontSize: "1.1rem", color: "#ffffff", marginBottom: "6px" }}>No Cases Found</h3>
            <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
              There are no cases matching the selected filters or search query.
            </p>
          </div>
        ) : (
          filteredCases.map((c) => {
            const risk = c.assessment?.risk_level || "GREEN";
            const bp = c.assessment?.blood_pressure || "—";
            const hb = c.assessment?.haemoglobin ? `${c.assessment.haemoglobin} g/dL` : "—";
            const isSevereBp = bp.includes("160") || bp.includes("165") || bp.includes("170") || bp.includes("110");
            const isSevereHb = c.assessment?.haemoglobin && c.assessment.haemoglobin < 7.0;
            const dangerSigns = c.assessment?.danger_signs || {};
            const dangerCount = Object.values(dangerSigns).filter(Boolean).length;
            const hasAudio = !!c.audio_artifact;

            return (
              <div
                key={c.case_id || c.patient_id}
                className="glass-panel"
                onClick={() => onSelectCase(c)}
                style={{
                  padding: "18px 22px",
                  cursor: "pointer",
                  transition: "all 0.2s ease",
                  borderLeft: risk === "RED" 
                    ? "5px solid var(--risk-red)" 
                    : (risk === "AMBER" ? "5px solid var(--risk-amber)" : "5px solid var(--risk-green)"),
                  background: risk === "RED" ? "rgba(239, 68, 68, 0.03)" : undefined,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "16px",
                  flexWrap: "wrap"
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-2px)";
                  e.currentTarget.style.boxShadow = "var(--shadow-lg)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "none";
                  e.currentTarget.style.boxShadow = "var(--shadow-md)";
                }}
              >
                {/* Left: Triage Badge & Patient Basic Info */}
                <div style={{ display: "flex", alignItems: "flex-start", gap: "16px", flex: "1 1 320px" }}>
                  
                  {/* Triage Urgency Indicator Pill */}
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "6px" }}>
                    <span className={risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")}>
                      {risk === "RED" && <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "var(--risk-red)" }} className="animate-pulse-red" />}
                      {risk}
                    </span>
                    <span style={{ fontSize: "0.7rem", color: "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                      <Clock size={12} />
                      {formatTimeElapsed(c.created_at)}
                    </span>
                  </div>

                  {/* Patient Name, Age, Gestation, Village */}
                  <div>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                      <h2 style={{ fontSize: "1.15rem", fontWeight: 700, color: "#ffffff" }}>
                        {c.patient_name}
                      </h2>
                      <span className="mono" style={{ fontSize: "0.75rem", color: "var(--text-muted)", background: "rgba(255, 255, 255, 0.05)", padding: "2px 6px", borderRadius: "4px" }}>
                        {c.case_id || c.patient_id}
                      </span>
                      {c.is_demo && (
                        <span style={{ fontSize: "0.65rem", background: "rgba(168, 85, 247, 0.15)", color: "#c084fc", border: "1px solid rgba(168, 85, 247, 0.3)", padding: "1px 6px", borderRadius: "4px", fontWeight: 600 }}>
                          Demo Mode
                        </span>
                      )}
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "14px", marginTop: "6px", fontSize: "0.8rem", color: "var(--text-secondary)", flexWrap: "wrap" }}>
                      <span style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                        <MapPin size={14} color="#38bdf8" />
                        <strong>{c.village}</strong>
                      </span>
                      {c.age_years && (
                        <span>Age: {c.age_years}y</span>
                      )}
                      {c.gestational_age_weeks && (
                        <span>Gestation: <strong>{c.gestational_age_weeks} weeks</strong></span>
                      )}
                      {c.gravida !== null && c.gravida !== undefined && (
                        <span>G{c.gravida}P{c.para ?? 0}</span>
                      )}
                    </div>

                    {c.travel_constraints && (
                      <div style={{ fontSize: "0.75rem", color: "#fcd34d", marginTop: "4px", display: "flex", alignItems: "center", gap: "4px" }}>
                        <span>⚠️ Access constraint: {c.travel_constraints}</span>
                      </div>
                    )}
                  </div>

                </div>

                {/* Middle: Clinical Metrics & Danger Signs */}
                <div style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
                  
                  {/* BP Pill */}
                  <div style={{
                    padding: "6px 12px",
                    borderRadius: "8px",
                    background: isSevereBp ? "rgba(239, 68, 68, 0.15)" : "rgba(255, 255, 255, 0.05)",
                    border: isSevereBp ? "1px solid rgba(239, 68, 68, 0.4)" : "1px solid var(--border-subtle)",
                    textAlign: "center"
                  }}>
                    <span style={{ fontSize: "0.65rem", color: "var(--text-muted)", display: "block", textTransform: "uppercase" }}>Blood Pressure</span>
                    <strong style={{ fontSize: "0.95rem", color: isSevereBp ? "#fca5a5" : "#ffffff" }}>{bp}</strong>
                  </div>

                  {/* Hb Pill */}
                  <div style={{
                    padding: "6px 12px",
                    borderRadius: "8px",
                    background: isSevereHb ? "rgba(239, 68, 68, 0.15)" : "rgba(255, 255, 255, 0.05)",
                    border: isSevereHb ? "1px solid rgba(239, 68, 68, 0.4)" : "1px solid var(--border-subtle)",
                    textAlign: "center"
                  }}>
                    <span style={{ fontSize: "0.65rem", color: "var(--text-muted)", display: "block", textTransform: "uppercase" }}>Haemoglobin</span>
                    <strong style={{ fontSize: "0.95rem", color: isSevereHb ? "#fca5a5" : "#ffffff" }}>{hb}</strong>
                  </div>

                  {/* Danger Signs Count Badge */}
                  {dangerCount > 0 && (
                    <div style={{
                      padding: "6px 12px",
                      borderRadius: "8px",
                      background: "rgba(239, 68, 68, 0.12)",
                      border: "1px solid rgba(239, 68, 68, 0.3)",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px"
                    }}>
                      <AlertTriangle size={14} color="var(--risk-red)" />
                      <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "#fca5a5" }}>
                        {dangerCount} Danger Sign{dangerCount > 1 ? "s" : ""}
                      </span>
                    </div>
                  )}

                  {/* Voice Note Artifact Badge */}
                  {hasAudio && (
                    <div style={{
                      padding: "6px 10px",
                      borderRadius: "8px",
                      background: "rgba(14, 165, 233, 0.12)",
                      border: "1px solid rgba(14, 165, 233, 0.3)",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      fontSize: "0.75rem",
                      color: "#38bdf8",
                      fontWeight: 600
                    }}>
                      <Mic size={14} color="#38bdf8" />
                      <span>Audio Note Attached</span>
                    </div>
                  )}

                </div>

                {/* Right: Operational Status & Console Button */}
                <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                  
                  {/* Action Status Pill */}
                  <div style={{ textAlign: "right" }}>
                    {c.ambulance_status ? (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#38bdf8", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Truck size={14} />
                        <span>108 Dispatched</span>
                      </div>
                    ) : c.doctor_advisory ? (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#34d399", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Stethoscope size={14} />
                        <span>Advisory Issued</span>
                      </div>
                    ) : (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#fcd34d", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Clock size={14} />
                        <span>Pending Review</span>
                      </div>
                    )}
                    <span style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>
                      Worker: {c.worker_id || "ASHA Frontline"}
                    </span>
                  </div>

                  {/* Open Case CTA */}
                  <button
                    className="btn-primary"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectCase(c);
                    }}
                    style={{ padding: "8px 14px", fontSize: "0.8rem" }}
                  >
                    <span>Console</span>
                    <ChevronRight size={16} />
                  </button>

                </div>

              </div>
            );
          })
        )}
      </div>

    </div>
  );
};
