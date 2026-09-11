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
    <div>
      {/* Title */}
      <div className="page-title-row">
        <div>
          <h1 className="page-title">Frontline ASHA Worker Roster</h1>
          <p className="page-subtitle">
            Accredited Social Health Activists (ASHAs) equipped with offline Android applications for rural field triage.
          </p>
        </div>
        <button onClick={loadData} className="btn btn-secondary btn-sm">
          <RefreshCw size={14} />
          <span>Refresh</span>
        </button>
      </div>

      {loading ? (
        <div className="empty-state">Loading worker roster...</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "16px" }}>
          {workers.map((w) => (
            <div
              key={w.id}
              style={{
                background: "#ffffff",
                border: "1px solid var(--border-color)",
                borderRadius: "var(--radius-lg)",
                padding: "20px",
                display: "flex",
                flexDirection: "column",
                gap: "14px",
                boxShadow: "var(--shadow-xs)"
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                  <div style={{
                    width: "40px",
                    height: "40px",
                    borderRadius: "50%",
                    background: "var(--bg-muted)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--primary-blue)"
                  }}>
                    <Users size={20} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.05rem", fontWeight: 600, color: "var(--text-main)", margin: 0 }}>
                      {w.name}
                    </h3>
                    <span className="case-id-code">{w.id}</span>
                  </div>
                </div>

                <span className="badge badge-green">
                  <span className="live-dot" style={{ width: "6px", height: "6px" }} />
                  {w.status}
                </span>
              </div>

              <div style={{ fontSize: "0.85rem", color: "var(--text-body)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--text-muted)" />
                  <span>Phone: <strong>+91 {w.phone}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--text-muted)" />
                  <span>Facility Link: <strong>{w.facility_id}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Radio size={14} color="var(--text-muted)" />
                  <span>Language / Dialect: <strong>{w.locale} (Hindi)</strong></span>
                </div>
              </div>

              <div style={{
                background: "var(--bg-muted)",
                border: "1px solid var(--border-color)",
                padding: "8px 12px",
                borderRadius: "var(--radius-sm)",
                fontSize: "0.78rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center"
              }}>
                <span>Encryption:</span>
                <span className="case-id-code" style={{ fontSize: "0.72rem" }}>SQLCipher AES-256</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
