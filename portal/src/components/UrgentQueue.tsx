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
    <div className="section-container">
      
      {/* Section Header */}
      <div className="header-row">
        <div>
          <div className="technical-label" style={{ marginBottom: "4px" }}>
            <span>01 / TRIAGE QUEUE · ACTIVE CLINICAL CASENOTES</span>
          </div>
          <h2 className="title-primary">
            Frontline Maternal Danger-Sign <span className="editorial-italic">Screening Queue</span>
          </h2>
          <p className="subtitle">
            Point-of-care encounters synced from offline ASHA workers. Ordered deterministically by MoHFW clinical risk severity.
          </p>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <button
            onClick={onRefresh}
            disabled={loading}
            className="btn-outline"
            style={{ fontSize: "0.8rem", padding: "7px 16px" }}
          >
            <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
            <span>Sync Live Queue</span>
          </button>
        </div>
      </div>

      {/* KPI Metric Summary Row */}
      <div className="kpi-grid">
        
        {/* Critical RED Cases */}
        <div className="kpi-card border-red">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span className="kpi-label" style={{ color: "var(--coral-dark)" }}>
              Critical Emergencies
            </span>
            <AlertOctagon size={16} color="var(--coral-dark)" />
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "10px", marginTop: "8px" }}>
            <span className="kpi-value text-red">
              {stats.red}
            </span>
            <span style={{ fontSize: "0.75rem", color: "var(--coral-dark)", fontWeight: 500 }}>
              Immediate 108 transfer indicated
            </span>
          </div>
        </div>

        {/* Moderate AMBER Cases */}
        <div className="kpi-card border-amber">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span className="kpi-label" style={{ color: "var(--amber-dark)" }}>
              Moderate High-Risk
            </span>
            <AlertTriangle size={16} color="var(--amber-dark)" />
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "10px", marginTop: "8px" }}>
            <span className="kpi-value text-amber">
              {stats.amber}
            </span>
            <span style={{ fontSize: "0.75rem", color: "var(--amber-dark)", fontWeight: 500 }}>
              PHC Doctor review within 24h
            </span>
          </div>
        </div>

        {/* Normal GREEN Cases */}
        <div className="kpi-card border-green">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span className="kpi-label" style={{ color: "var(--seafoam-dark)" }}>
              Routine ANC Care
            </span>
            <CheckCircle2 size={16} color="var(--seafoam-dark)" />
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "10px", marginTop: "8px" }}>
            <span className="kpi-value text-green">
              {stats.green}
            </span>
            <span style={{ fontSize: "0.75rem", color: "var(--seafoam-dark)", fontWeight: 500 }}>
              Stable / Routine ANC monitoring
            </span>
          </div>
        </div>

        {/* 108 Emergency Transport */}
        <div className="kpi-card border-blue">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span className="kpi-label" style={{ color: "var(--navy-deep)" }}>
              108 Transports
            </span>
            <Truck size={16} color="var(--navy-deep)" />
          </div>
          <div style={{ display: "flex", alignItems: "baseline", gap: "10px", marginTop: "8px" }}>
            <span className="kpi-value text-blue">
              {stats.dispatched}
            </span>
            <span style={{ fontSize: "0.75rem", color: "var(--navy-deep)", fontWeight: 500 }}>
              Active patient transfers
            </span>
          </div>
        </div>

      </div>

      {/* Filter & Controls Bar */}
      <div style={{ 
        background: "var(--panel-white)", 
        border: "1px solid var(--rule-muted)", 
        borderRadius: "var(--radius-lg)", 
        padding: "16px 20px", 
        marginBottom: "20px",
        boxShadow: "var(--shadow-sm)"
      }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "16px" }}>
          
          {/* Search Field */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flex: "1 1 280px", position: "relative" }}>
            <Search size={16} color="var(--text-muted)" style={{ position: "absolute", left: "12px" }} />
            <input
              type="text"
              placeholder="Search by mother's name, village, case ID, or ASHA worker..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: "100%", paddingLeft: "36px", fontSize: "0.85rem" }}
            />
          </div>

          {/* Risk Level Filter Pills */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginRight: "4px", fontFamily: "var(--font-mono)" }}>TRIAGE:</span>
            <button
              onClick={() => setRiskFilter("ALL")}
              style={{
                padding: "5px 12px",
                borderRadius: "var(--radius-pill)",
                border: "1px solid var(--rule-muted)",
                background: riskFilter === "ALL" ? "var(--navy-deep)" : "transparent",
                color: riskFilter === "ALL" ? "#FFFFFF" : "var(--navy-deep)",
                fontWeight: 600,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              All ({cases.length})
            </button>
            <button
              onClick={() => setRiskFilter("RED")}
              style={{
                padding: "5px 12px",
                borderRadius: "var(--radius-pill)",
                border: "1px solid var(--risk-red-border)",
                background: riskFilter === "RED" ? "var(--coral-dark)" : "var(--coral-bg)",
                color: riskFilter === "RED" ? "#FFFFFF" : "var(--coral-dark)",
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
                padding: "5px 12px",
                borderRadius: "var(--radius-pill)",
                border: "1px solid var(--risk-amber-border)",
                background: riskFilter === "AMBER" ? "var(--amber-dark)" : "var(--amber-bg)",
                color: riskFilter === "AMBER" ? "#FFFFFF" : "var(--amber-dark)",
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
                padding: "5px 12px",
                borderRadius: "var(--radius-pill)",
                border: "1px solid var(--risk-green-border)",
                background: riskFilter === "GREEN" ? "var(--seafoam-dark)" : "var(--seafoam-bg)",
                color: riskFilter === "GREEN" ? "#FFFFFF" : "var(--seafoam-dark)",
                fontWeight: 700,
                fontSize: "0.75rem",
                cursor: "pointer"
              }}
            >
              GREEN ({stats.green})
            </button>
          </div>

          {/* Action Status Dropdown */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ padding: "6px 14px", fontSize: "0.8rem", borderRadius: "var(--radius-pill)" }}
            >
              <option value="ALL">All Coordination States</option>
              <option value="PENDING">Pending Clinical Review</option>
              <option value="ACKNOWLEDGED">Doctor Advisory Issued</option>
              <option value="DISPATCHED">108 Transport Active</option>
            </select>
          </div>

        </div>
      </div>

      {/* Cases List */}
      <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
        {filteredCases.length === 0 ? (
          <div className="empty-state">
            <FileCheck size={42} color="var(--text-muted)" style={{ margin: "0 auto 12px auto" }} />
            <h3 style={{ fontSize: "1.1rem", color: "var(--text-navy)", marginBottom: "6px", fontWeight: 500 }}>No Cases Found</h3>
            <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
              There are no patient encounters matching the selected filters.
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
                onClick={() => onSelectCase(c)}
                style={{
                  padding: "20px 24px",
                  cursor: "pointer",
                  transition: "all 0.25s cubic-bezier(0.16, 1, 0.3, 1)",
                  borderRadius: "var(--radius-lg)",
                  background: "var(--panel-white)",
                  border: "1px solid var(--rule-muted)",
                  borderLeft: risk === "RED" 
                    ? "5px solid var(--coral-accent)" 
                    : (risk === "AMBER" ? "5px solid var(--amber-accent)" : "5px solid var(--seafoam-dark)"),
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "16px",
                  flexWrap: "wrap",
                  boxShadow: "var(--shadow-sm)"
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-2px)";
                  e.currentTarget.style.boxShadow = "var(--shadow-md)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "none";
                  e.currentTarget.style.boxShadow = "var(--shadow-sm)";
                }}
              >
                {/* Left: Triage Badge & Patient Basic Info */}
                <div style={{ display: "flex", alignItems: "flex-start", gap: "16px", flex: "1 1 320px" }}>
                  
                  {/* Triage Urgency Indicator Pill */}
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "6px" }}>
                    <span className={risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")} style={{ minWidth: "64px", justifyContent: "center" }}>
                      {risk}
                    </span>
                    <span style={{ fontSize: "0.7rem", color: "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                      <Clock size={11} />
                      {formatTimeElapsed(c.created_at)}
                    </span>
                  </div>

                  {/* Patient Name, Age, Gestation, Village */}
                  <div>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                      <h3 style={{ fontSize: "1.15rem", fontWeight: 600, color: "var(--navy-deep)", margin: 0 }}>
                        {c.patient_name}
                      </h3>
                      <span className="mono-badge">
                        {c.case_id || c.patient_id}
                      </span>
                      {c.is_demo && (
                        <span style={{ fontSize: "0.65rem", background: "var(--panel-pale)", color: "var(--navy-deep)", border: "1px solid var(--rule-muted)", padding: "1px 6px", borderRadius: "4px", fontWeight: 600 }}>
                          DEMO
                        </span>
                      )}
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "14px", marginTop: "6px", fontSize: "0.825rem", color: "var(--text-muted)", flexWrap: "wrap" }}>
                      <span style={{ display: "flex", alignItems: "center", gap: "4px", color: "var(--text-body)" }}>
                        <MapPin size={13} color="var(--navy-deep)" />
                        <strong>{c.village}</strong>
                      </span>
                      {c.age_years && (
                        <span>Age: {c.age_years}y</span>
                      )}
                      {c.gestational_age_weeks && (
                        <span>Gestation: <strong>{c.gestational_age_weeks}w</strong></span>
                      )}
                      {c.gravida !== null && c.gravida !== undefined && (
                        <span>G{c.gravida}P{c.para ?? 0}</span>
                      )}
                    </div>

                    {c.travel_constraints && (
                      <div style={{ fontSize: "0.75rem", color: "var(--amber-dark)", marginTop: "4px", display: "flex", alignItems: "center", gap: "4px" }}>
                        <span>⚠️ Access: {c.travel_constraints}</span>
                      </div>
                    )}
                  </div>

                </div>

                {/* Middle: Clinical Metrics & Danger Signs */}
                <div style={{ display: "flex", alignItems: "center", gap: "14px", flexWrap: "wrap" }}>
                  
                  {/* BP Pill */}
                  <div style={{
                    padding: "6px 12px",
                    borderRadius: "var(--radius-md)",
                    background: isSevereBp ? "var(--coral-bg)" : "var(--panel-pale)",
                    border: isSevereBp ? "1px solid rgba(226, 123, 112, 0.5)" : "1px solid var(--rule-muted)",
                    textAlign: "center"
                  }}>
                    <span style={{ fontSize: "0.625rem", color: "var(--text-muted)", display: "block", textTransform: "uppercase", fontFamily: "var(--font-mono)" }}>Blood Pressure</span>
                    <strong style={{ fontSize: "0.95rem", color: isSevereBp ? "var(--coral-dark)" : "var(--navy-deep)", fontFamily: "var(--font-mono)" }}>{bp}</strong>
                  </div>

                  {/* Hb Pill */}
                  <div style={{
                    padding: "6px 12px",
                    borderRadius: "var(--radius-md)",
                    background: isSevereHb ? "var(--coral-bg)" : "var(--panel-pale)",
                    border: isSevereHb ? "1px solid rgba(226, 123, 112, 0.5)" : "1px solid var(--rule-muted)",
                    textAlign: "center"
                  }}>
                    <span style={{ fontSize: "0.625rem", color: "var(--text-muted)", display: "block", textTransform: "uppercase", fontFamily: "var(--font-mono)" }}>Haemoglobin</span>
                    <strong style={{ fontSize: "0.95rem", color: isSevereHb ? "var(--coral-dark)" : "var(--navy-deep)", fontFamily: "var(--font-mono)" }}>{hb}</strong>
                  </div>

                  {/* Danger Signs Count Badge */}
                  {dangerCount > 0 && (
                    <div style={{
                      padding: "6px 12px",
                      borderRadius: "var(--radius-md)",
                      background: "var(--coral-bg)",
                      border: "1px solid rgba(226, 123, 112, 0.4)",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px"
                    }}>
                      <AlertTriangle size={13} color="var(--coral-dark)" />
                      <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--coral-dark)" }}>
                        {dangerCount} Danger Sign{dangerCount > 1 ? "s" : ""}
                      </span>
                    </div>
                  )}

                  {/* Voice Note Artifact Badge */}
                  {hasAudio && (
                    <div style={{
                      padding: "6px 10px",
                      borderRadius: "var(--radius-md)",
                      background: "var(--panel-pale)",
                      border: "1px solid var(--rule-muted)",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      fontSize: "0.75rem",
                      color: "var(--navy-deep)",
                      fontWeight: 600
                    }}>
                      <Mic size={13} color="var(--navy-deep)" />
                      <span>Voice Note</span>
                    </div>
                  )}

                </div>

                {/* Right: Operational Status & Console Button */}
                <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                  
                  {/* Action Status Pill */}
                  <div style={{ textAlign: "right" }}>
                    {c.ambulance_status ? (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "var(--navy-deep)", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Truck size={14} />
                        <span>108 Dispatched</span>
                      </div>
                    ) : c.doctor_advisory ? (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "var(--seafoam-dark)", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Stethoscope size={14} />
                        <span>Advisory Issued</span>
                      </div>
                    ) : (
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "var(--amber-dark)", fontSize: "0.8rem", fontWeight: 600 }}>
                        <Clock size={14} />
                        <span>Pending Review</span>
                      </div>
                    )}
                    <span style={{ fontSize: "0.7rem", color: "var(--text-muted)", fontFamily: "var(--font-mono)" }}>
                      {c.worker_id || "ASHA"}
                    </span>
                  </div>

                  {/* Open Case CTA */}
                  <button
                    className="btn-primary"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectCase(c);
                    }}
                    style={{ padding: "8px 16px", fontSize: "0.825rem" }}
                  >
                    <span>Review</span>
                    <ChevronRight size={14} />
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
