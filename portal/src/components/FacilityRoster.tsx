import React, { useEffect, useState } from "react";
import type { Facility } from "../api";
import { fetchFacilities } from "../api";
import { Hospital, Phone, MapPin, RefreshCw } from "lucide-react";

export const FacilityRoster: React.FC = () => {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await fetchFacilities();
      setFacilities(res.facilities);
    } catch (err) {
      console.error("Failed to fetch facilities", err);
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
            Referral Health Facilities Directory
          </h2>
          <p style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
            Primary Health Centres (PHCs) and Community Health Centres (CHCs) in active district catchment
          </p>
        </div>
        <button onClick={loadData} className="btn-outline" style={{ fontSize: "0.8rem", padding: "6px 12px" }}>
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          <span>Refresh Roster</span>
        </button>
      </div>

      {loading ? (
        <div style={{ padding: "48px", textAlign: "center", color: "var(--text-muted)" }}>
          Loading facilities...
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "16px" }}>
          {facilities.map((f) => (
            <div
              key={f.id}
              className="glass-panel"
              style={{
                padding: "20px",
                borderLeft: f.type === "CHC" ? "4px solid #38bdf8" : "4px solid #10b981",
                display: "flex",
                flexDirection: "column",
                gap: "14px"
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <div style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "8px",
                    background: f.type === "CHC" ? "rgba(56, 189, 248, 0.15)" : "rgba(16, 185, 129, 0.15)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                  }}>
                    <Hospital size={20} color={f.type === "CHC" ? "#38bdf8" : "#10b981"} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.05rem", fontWeight: 700, color: "#ffffff" }}>{f.name}</h3>
                    <span className="mono" style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>{f.id}</span>
                  </div>
                </div>
                <span style={{
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  padding: "2px 8px",
                  borderRadius: "4px",
                  background: f.type === "CHC" ? "rgba(56, 189, 248, 0.2)" : "rgba(16, 185, 129, 0.2)",
                  color: f.type === "CHC" ? "#7dd3fc" : "#6ee7b7"
                }}>
                  {f.type}
                </span>
              </div>

              <div style={{ fontSize: "0.8rem", color: "var(--text-secondary)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--text-muted)" />
                  <span>Catchment: <strong>{f.catchment_area || "General District"}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--text-muted)" />
                  <span>Emergency Desk: <strong>{f.contact_phone || "108"}</strong></span>
                </div>
              </div>

              <div style={{
                background: "rgba(255, 255, 255, 0.02)",
                padding: "8px 12px",
                borderRadius: "6px",
                fontSize: "0.75rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between"
              }}>
                <span>Emergency Obstetric Care</span>
                <span style={{ color: "#34d399", fontWeight: 600 }}>24x7 Available</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
