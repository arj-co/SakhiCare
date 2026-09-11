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
    <div>
      {/* Eyra Section Label & Page Title */}
      <div className="page-title-row">
        <div>
          <div className="eyra-section-label">Referral Infrastructure</div>
          <h1 className="page-title">Referral Health Facilities Directory</h1>
          <p className="page-subtitle">
            Primary Health Centres (PHCs) and Community Health Centres (CHCs) with verified 24x7 emergency obstetric and blood storage capacity.
          </p>
        </div>
        <button onClick={loadData} className="btn btn-secondary btn-sm">
          <RefreshCw size={13} />
          <span>Refresh</span>
        </button>
      </div>

      {loading ? (
        <div className="empty-state">Loading facilities directory...</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "18px" }}>
          {facilities.map((f) => (
            <div
              key={f.id}
              style={{
                background: "#FFFFFF",
                border: "1px solid var(--border-color)",
                borderLeft: f.type === "CHC" ? "5px solid var(--medical-teal)" : "5px solid var(--teal-vivid)",
                borderRadius: "var(--radius-lg)",
                padding: "22px 24px",
                display: "flex",
                flexDirection: "column",
                gap: "14px",
                boxShadow: "var(--shadow-subtle)"
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                  <div style={{
                    width: "42px",
                    height: "42px",
                    borderRadius: "var(--radius-pill)",
                    background: "var(--teal-light)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--medical-teal)"
                  }}>
                    <Hospital size={20} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: "1.15rem", fontFamily: "var(--font-serif)", color: "var(--text-primary)", margin: 0 }}>
                      {f.name}
                    </h3>
                    <span className="case-id-code">{f.id}</span>
                  </div>
                </div>
                <span className={f.type === "CHC" ? "badge badge-teal" : "badge badge-green"}>
                  {f.type}
                </span>
              </div>

              <div style={{ fontSize: "0.85rem", color: "var(--text-body)", display: "flex", flexDirection: "column", gap: "6px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <MapPin size={14} color="var(--medical-teal)" />
                  <span>Catchment Area: <strong>{f.catchment_area || "District Rural Catchment"}</strong></span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Phone size={14} color="var(--medical-teal)" />
                  <span>Direct Line: <strong style={{ fontFamily: "var(--font-mono)" }}>{f.contact_phone || "0612-2554401"}</strong></span>
                </div>
              </div>

              <div style={{
                background: "var(--bg-card-warm)",
                border: "1px solid var(--border-subtle)",
                padding: "8px 14px",
                borderRadius: "var(--radius-sm)",
                fontSize: "0.78rem",
                color: "var(--text-muted)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center"
              }}>
                <span>Obstetric Capacity:</span>
                <strong style={{ color: "var(--medical-teal)" }}>24x7 BEmOC / C-Section OT</strong>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
