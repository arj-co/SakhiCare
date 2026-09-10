import React, { useEffect, useState } from "react";
import type { Worker } from "../api";
import { fetchWorkers } from "../api";
import { Users, Phone, MapPin, Radio, RefreshCw } from "lucide-react";

export const WorkerRoster: React.FC = () => {
  const [workers, setWorkers] = useState<Worker[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await fetchWorkers();
      setWorkers(res.workers);
    } catch (err) {
      console.error("Failed to fetch workers", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="section-container">
      <div className="header-row">
        <div>
          <div className="technical-label" style={{ marginBottom: "4px" }}>
            <span>05 / FRONTLINE HUMAN INFRASTRUCTURE · COMMUNITY ASHA CORPS</span>
          </div>
          <h2 className="title-primary">
            Frontline ASHA Worker <span className="editorial-italic">Roster</span>
          </h2>
          <p className="subtitle">
            Accredited Social Health Activists equipped with offline-first SQLCipher Android apps for rural danger-sign screening.
          </p>
        </div>
        <button onClick={loadData} className="btn-outline" style={{ fontSize: "0.8rem", padding: "7px 16px" }}>
          <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
          <span>Refresh Workers</span>
        </button>
      </div>

      {loading ? (
        <div className="loading-state">
          Loading ASHA worker roster...
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "16px" }}>
          {workers.map((w) => (
            <div
              key={w.id}
              style={{
                background: "var(--panel-white)",
                border: "1px solid var(--rule-muted)",
                borderRadius: "var(--radius-lg)",
                padding: "20px 24px",
                display: "flex",
                flexDirection: "column",
                gap: "14px",
                boxShadow: "var(--shadow-sm)"
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                  <div style={{
                    width: "38px",
                    height: "38px",
                    borderRadius: "50%",
                    background: "var(--panel-pale)",
                    border: "1px solid var(--rule-muted)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                  }}>
                    <Users size={18} color="var(--navy-deep)" />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.05rem", fontWeight: 600, color: "var(--navy-deep)", margin: 0 }}>{w.name}</h3>
                    <span className="mono-badge">{w.id}</span>
                  </div>
                </div>

                <span className="badge badge-green">
                  <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "var(--seafoam-dark)" }} />
                  {w.status}
                </span>
              </div>

              <div style={{ fontSize: "0.825rem", color: "var(--text-body)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--text-muted)" />
                  <span>Mobile: <strong className="mono">+91 {w.phone}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--text-muted)" />
                  <span>Assigned Facility: <strong className="mono">{w.facility_id}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Radio size={14} color="var(--text-muted)" />
                  <span>Locale: <strong>{w.locale} (Hindi / Indic Dialect)</strong></span>
                </div>
              </div>

              <div style={{
                background: "var(--panel-pale)",
                border: "1px solid var(--rule-soft)",
                padding: "8px 12px",
                borderRadius: "var(--radius-sm)",
                fontSize: "0.72rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center"
              }}>
                <span>Offline Storage:</span>
                <span style={{ color: "var(--navy-deep)", fontWeight: 600, fontFamily: "var(--font-mono)" }}>SQLCipher AES-256</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
