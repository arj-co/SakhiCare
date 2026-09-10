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
    <div className="section-container">
      <div className="header-row">
        <div>
          <div className="technical-label" style={{ marginBottom: "4px" }}>
            <span>04 / REFERRAL DIRECTORY · MATERNAL CARE INFRASTRUCTURE</span>
          </div>
          <h2 className="title-primary">
            Referral Health <span className="editorial-italic">Facilities Directory</span>
          </h2>
          <p className="subtitle">
            Primary Health Centres (PHCs) and First Referral Units (CHCs / FRUs) with verified 24x7 obstetric capabilities.
          </p>
        </div>
        <button onClick={loadData} className="btn-outline" style={{ fontSize: "0.8rem", padding: "7px 16px" }}>
          <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
          <span>Refresh Directory</span>
        </button>
      </div>

      {loading ? (
        <div className="loading-state">
          Loading referral health facilities...
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "16px" }}>
          {facilities.map((f) => (
            <div
              key={f.id}
              style={{
                background: "var(--panel-white)",
                border: "1px solid var(--rule-muted)",
                borderRadius: "var(--radius-lg)",
                padding: "20px 24px",
                borderLeft: f.type === "CHC" ? "4px solid var(--navy-deep)" : "4px solid var(--seafoam-dark)",
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
                    borderRadius: "10px",
                    background: "var(--panel-pale)",
                    border: "1px solid var(--rule-muted)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                  }}>
                    <Hospital size={18} color="var(--navy-deep)" />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.05rem", fontWeight: 600, color: "var(--navy-deep)", margin: 0 }}>{f.name}</h3>
                    <span className="mono-badge">{f.id}</span>
                  </div>
                </div>
                <span className={f.type === "CHC" ? "badge badge-blue" : "badge badge-green"}>
                  {f.type}
                </span>
              </div>

              <div style={{ fontSize: "0.825rem", color: "var(--text-body)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--text-muted)" />
                  <span>Catchment: <strong>{f.catchment_area || "District Rural Catchment"}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--text-muted)" />
                  <span>Desk Line: <strong className="mono">{f.contact_phone || "0612-2554401"}</strong></span>
                </div>
              </div>

              <div style={{
                background: "var(--panel-pale)",
                border: "1px solid var(--rule-soft)",
                padding: "8px 12px",
                borderRadius: "var(--radius-sm)",
                fontSize: "0.75rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center"
              }}>
                <span>Emergency Obstetric Service:</span>
                <span style={{ color: "var(--seafoam-dark)", fontWeight: 700 }}>24x7 BEmOC / C-Section OT</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
