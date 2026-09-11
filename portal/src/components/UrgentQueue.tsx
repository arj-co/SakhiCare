import React, { useState, useMemo } from "react";
import type { PregnancyCase } from "../api";
import { 
  AlertOctagon, 
  AlertTriangle, 
  CheckCircle, 
  Clock, 
  MapPin, 
  Mic, 
  Truck, 
  Search, 
  ChevronRight, 
  Stethoscope,
  Activity,
  HeartHandshake
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

  const formatTimeElapsed = (timestamp?: number) => {
    if (!timestamp) return "Recent";
    const now = Math.floor(Date.now() / 1000);
    const diff = Math.max(0, now - timestamp);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

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
    <div>
      {/* Eyra Section Label & Page Title */}
      <div className="page-title-row">
        <div>
          <div className="eyra-section-label">Point-of-Care Encounters</div>
          <h1 className="page-title">Patient Triage & Screening Queue</h1>
          <p className="page-subtitle">
            Community maternal health encounters captured offline by ASHA workers. Evaluated deterministically to ensure timely, dignified clinical care.
          </p>
        </div>
        <button onClick={onRefresh} className="btn btn-secondary btn-sm queue-refresh-button">
          <Activity size={13} />
          <span>Refresh queue</span>
        </button>
      </div>

      {/* Toolbar: Search & Filter Pills */}
      <div className="toolbar">
        {/* Search */}
        <div className="search-input-wrapper">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            className="search-input"
            placeholder="Search by mother's name, village, or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Triage Filter Pills */}
        <div className="filter-group">
          <button
            className={`filter-pill ${riskFilter === "ALL" ? "active" : ""}`}
            onClick={() => setRiskFilter("ALL")}
          >
            All Patients ({cases.length})
          </button>
          <button
            className={`filter-pill ${riskFilter === "RED" ? "active-red" : ""}`}
            onClick={() => setRiskFilter("RED")}
          >
            <AlertOctagon size={13} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Critical RED ({cases.filter((c) => c.assessment?.risk_level === "RED").length})
          </button>
          <button
            className={`filter-pill ${riskFilter === "AMBER" ? "active-amber" : ""}`}
            onClick={() => setRiskFilter("AMBER")}
          >
            <AlertTriangle size={13} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Moderate AMBER ({cases.filter((c) => c.assessment?.risk_level === "AMBER").length})
          </button>
          <button
            className={`filter-pill ${riskFilter === "GREEN" ? "active-green" : ""}`}
            onClick={() => setRiskFilter("GREEN")}
          >
            <CheckCircle size={13} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Routine GREEN ({cases.filter((c) => c.assessment?.risk_level === "GREEN").length})
          </button>
        </div>

        {/* Status Select */}
        <div>
          <select
            className="form-control"
            style={{ width: "auto", fontSize: "0.8125rem", padding: "8px 14px", borderRadius: "var(--radius-pill)" }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Care States</option>
            <option value="PENDING">Pending Physician Guidance</option>
            <option value="ACKNOWLEDGED">Advisory Formulated</option>
            <option value="DISPATCHED">108 Ambulance Dispatched</option>
          </select>
        </div>
        <div className="queue-result-count">Showing <strong>{filteredCases.length}</strong> of {cases.length} encounters</div>
      </div>

      {/* Patient Cards List */}
      {loading ? (
        <div className="empty-state">
          <Activity size={36} className="empty-state-icon" style={{ animation: "pulse-dot 1.5s infinite", color: "var(--medical-teal)" }} />
          <h3 style={{ fontFamily: "var(--font-serif)" }}>Loading Patient Encounters...</h3>
          <p>Synchronizing offline encrypted records from SQLite database...</p>
        </div>
      ) : filteredCases.length === 0 ? (
        <div className="empty-state">
          <HeartHandshake size={42} className="empty-state-icon" color="var(--medical-teal)" />
          <h3 style={{ fontFamily: "var(--font-serif)" }}>No patient encounters found</h3>
          <p>No screening records matched your active search or risk filters.</p>
        </div>
      ) : (
        <div className="case-grid">
          {filteredCases.map((c) => {
            const risk = c.assessment?.risk_level || "GREEN";
            const bp = c.assessment?.blood_pressure || "—";
            const hb = c.assessment?.haemoglobin ? `${c.assessment.haemoglobin} g/dL` : "—";
            const isSevereBp = bp.includes("160") || bp.includes("165") || bp.includes("170") || bp.includes("110");
            const isSevereHb = c.assessment?.haemoglobin && c.assessment.haemoglobin < 7.0;
            const dangerSigns = c.assessment?.danger_signs || {};
            const activeDangerSigns = Object.entries(dangerSigns)
              .filter((entry) => Boolean(entry[1]))
              .map(([sign]) => sign.replace(/_/g, " "));
            const hasAudio = Boolean(c.audio_artifact);

            return (
              <div
                key={c.case_id || c.patient_id}
                className={`case-card ${risk === "RED" ? "card-red" : (risk === "AMBER" ? "card-amber" : "card-green")}`}
                onClick={() => onSelectCase(c)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") onSelectCase(c);
                }}
                role="button"
                tabIndex={0}
                aria-label={`Review ${c.patient_name}, ${risk} triage`}
                style={{ cursor: "pointer" }}
              >
                {/* Header: Patient Name in DM Serif Display + Triage Urgency Badge */}
                <div className="case-card-header">
                  <div className="patient-identity">
                    <span className="patient-name">{c.patient_name}</span>
                    <span className="case-id-code">{c.case_id || c.patient_id}</span>
                    <span className={`badge ${risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")}`}>
                      {risk === "RED" ? <AlertOctagon size={13} /> : (risk === "AMBER" ? <AlertTriangle size={13} /> : <CheckCircle size={13} />)}
                      {risk} TRIAGE
                    </span>
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                    <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                      <Clock size={13} />
                      {formatTimeElapsed(c.created_at)}
                    </span>
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectCase(c);
                      }}
                    >
                      <span>Review Patient</span>
                      <ChevronRight size={14} />
                    </button>
                  </div>
                </div>

                {/* Patient Context & Location Row */}
                <div style={{ display: "flex", alignItems: "center", gap: "16px", fontSize: "0.875rem", color: "var(--text-muted)", flexWrap: "wrap" }}>
                  <span style={{ display: "flex", alignItems: "center", gap: "5px", color: "var(--text-primary)", fontWeight: 600 }}>
                    <MapPin size={15} color="var(--medical-teal)" />
                    {c.village}
                  </span>
                  {c.age_years && (
                    <span>Age: <strong>{c.age_years} yrs</strong></span>
                  )}
                  {c.gestational_age_weeks && (
                    <span>Gestation: <strong>{c.gestational_age_weeks} weeks</strong></span>
                  )}
                  {c.gravida !== null && c.gravida !== undefined && (
                    <span>Gravida: <strong>G{c.gravida}P{c.para ?? 0}</strong></span>
                  )}
                  {hasAudio && (
                    <span className="badge badge-teal">
                      <Mic size={12} />
                      Voice Note Captured
                    </span>
                  )}
                </div>

                {/* Clinical Vitals Row */}
                <div className="vitals-row">
                  <div className="vital-chip">
                    <span className="vital-title">Blood Pressure</span>
                    <span className={`vital-value ${isSevereBp ? "vital-danger" : ""}`}>
                      {bp}
                    </span>
                  </div>

                  <div className="vital-chip">
                    <span className="vital-title">Haemoglobin</span>
                    <span className={`vital-value ${isSevereHb ? "vital-danger" : ""}`}>
                      {hb}
                    </span>
                  </div>

                  {/* Danger Signs Tags */}
                  {activeDangerSigns.length > 0 && (
                    <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap", marginLeft: "4px" }}>
                      {activeDangerSigns.map((s, idx) => (
                        <span key={idx} className="danger-tag">
                          ⚠ {s}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                {/* Clinical Rationale Note */}
                {c.assessment?.clinical_rationale && (
                  <div className="case-rationale">
                    <strong style={{ color: "var(--text-primary)" }}>Clinical Evaluation:</strong> {c.assessment.clinical_rationale}
                  </div>
                )}

                {/* Card Bottom: ASHA Attribution and Live State */}
                <div className="case-actions-bar">
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "0.8125rem", color: "var(--text-muted)" }}>
                    <span>Care Provider:</span>
                    <strong style={{ color: "var(--text-primary)" }}>{c.worker_id || "Community ASHA"}</strong>
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                    {c.ambulance_status ? (
                      <span className="badge badge-teal">
                        <Truck size={13} />
                        108 Status: {c.ambulance_status}
                      </span>
                    ) : c.doctor_advisory ? (
                      <span className="badge badge-green">
                        <Stethoscope size={13} />
                        Advisory Formulated
                      </span>
                    ) : (
                      <span className="badge badge-amber">
                        <Clock size={13} />
                        Pending Clinical Guidance
                      </span>
                    )}
                  </div>
                </div>

              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
