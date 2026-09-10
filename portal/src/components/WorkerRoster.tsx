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
    <div style={{ padding: "0 24px 32px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 800, color: "#ffffff" }}>
            Frontline ASHA Worker Roster
          </h2>
          <p style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
            Frontline community health workers conducting offline maternal danger-sign screening
          </p>
        </div>
        <button onClick={loadData} className="btn-outline" style={{ fontSize: "0.8rem", padding: "6px 12px" }}>
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          <span>Refresh Workers</span>
        </button>
      </div>

      {loading ? (
        <div style={{ padding: "48px", textAlign: "center", color: "var(--text-muted)" }}>
          Loading worker roster...
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "16px" }}>
          {workers.map((w) => (
            <div
              key={w.id}
              className="glass-panel"
              style={{
                padding: "18px",
                display: "flex",
                flexDirection: "column",
                gap: "12px"
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <div style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    background: "rgba(244, 63, 94, 0.15)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                  }}>
                    <Users size={18} color="#f43f5e" />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.05rem", fontWeight: 700, color: "#ffffff" }}>{w.name}</h3>
                    <span className="mono" style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>{w.id}</span>
                  </div>
                </div>

                <span style={{
                  fontSize: "0.7rem",
                  fontWeight: 700,
                  padding: "2px 8px",
                  borderRadius: "999px",
                  background: "rgba(16, 185, 129, 0.15)",
                  color: "#6ee7b7",
                  border: "1px solid rgba(16, 185, 129, 0.3)",
                  display: "flex",
                  alignItems: "center",
                  gap: "4px"
                }}>
                  <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#10b981" }} />
                  {w.status}
                </span>
              </div>

              <div style={{ fontSize: "0.8rem", color: "var(--text-secondary)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--text-muted)" />
                  <span>Mobile: <strong>+91 {w.phone}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--text-muted)" />
                  <span>Assigned Facility: <strong>{w.facility_id}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Radio size={14} color="var(--text-muted)" />
                  <span>Locale: <strong>{w.locale} (Hindi / Indic)</strong></span>
                </div>
              </div>

              <div style={{
                background: "rgba(255, 255, 255, 0.02)",
                padding: "6px 10px",
                borderRadius: "6px",
                fontSize: "0.7rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between"
              }}>
                <span>Android App Storage</span>
                <span style={{ color: "#38bdf8" }}>SQLCipher Encrypted (Room v2)</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
