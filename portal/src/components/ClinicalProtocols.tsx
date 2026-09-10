import React from "react";
import { Heart, Activity, AlertOctagon } from "lucide-react";

export const ClinicalProtocols: React.FC = () => {
  return (
    <div style={{ padding: "0 24px 32px 24px", maxWidth: "1200px" }}>
      <div style={{ marginBottom: "24px" }}>
        <h2 style={{ fontSize: "1.25rem", fontWeight: 800, color: "#ffffff" }}>
          MoHFW & WHO Maternal High-Risk Pregnancy (HRP) Protocols
        </h2>
        <p style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
          Standardized deterministic clinical rules (Rule pack: <code>mohfw-hrp-v1.0</code>)
        </p>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "20px" }}>
        
        {/* RED Danger Signs */}
        <div className="glass-panel" style={{ padding: "20px", borderLeft: "4px solid var(--risk-red)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "14px" }}>
            <AlertOctagon size={22} color="var(--risk-red)" />
            <h3 style={{ fontSize: "1.1rem", fontWeight: 700, color: "#ffffff" }}>
              Critical Emergency (RED Triage)
            </h3>
          </div>
          <p style={{ fontSize: "0.8rem", color: "var(--text-secondary)", marginBottom: "14px" }}>
            Triggers immediate 108 emergency ambulance dispatch and SMS/push escalation to Medical Officer.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px", fontSize: "0.8rem" }}>
            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fca5a5" }}>
              <strong>Antepartum Hemorrhage:</strong> Any vaginal bleeding during 2nd/3rd trimester.
            </div>
            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fca5a5" }}>
              <strong>Severe Hypertensive Crisis:</strong> Systolic BP ≥ 160 mmHg or Diastolic BP ≥ 110 mmHg.
            </div>
            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fca5a5" }}>
              <strong>Severe Maternal Anemia:</strong> Haemoglobin &lt; 7.0 g/dL (Blood bank alert).
            </div>
            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fca5a5" }}>
              <strong>Eclampsia / Convulsions:</strong> Fits, unconsciousness, or sudden visual disturbances.
            </div>
          </div>
        </div>

        {/* AMBER Danger Signs */}
        <div className="glass-panel" style={{ padding: "20px", borderLeft: "4px solid var(--risk-amber)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "14px" }}>
            <Activity size={22} color="var(--risk-amber)" />
            <h3 style={{ fontSize: "1.1rem", fontWeight: 700, color: "#ffffff" }}>
              Moderate High-Risk (AMBER Triage)
            </h3>
          </div>
          <p style={{ fontSize: "0.8rem", color: "var(--text-secondary)", marginBottom: "14px" }}>
            Requires primary health centre physician assessment within 24 hours.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px", fontSize: "0.8rem" }}>
            <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fcd34d" }}>
              <strong>Stage 1 Gestational Hypertension:</strong> SBP 140–159 mmHg or DBP 90–109 mmHg.
            </div>
            <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fcd34d" }}>
              <strong>Moderate Anemia:</strong> Haemoglobin 7.0–9.9 g/dL.
            </div>
            <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fcd34d" }}>
              <strong>Maternal Fever:</strong> Temperature ≥ 38.0°C (Malaria rapid test indicated).
            </div>
            <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "8px 12px", borderRadius: "6px", color: "#fcd34d" }}>
              <strong>Reduced Fetal Movement:</strong> Decreased kick count in 3rd trimester.
            </div>
          </div>
        </div>

        {/* Frontline Safe Actions Policy */}
        <div className="glass-panel" style={{ padding: "20px", gridColumn: "1 / -1" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
            <Heart size={20} color="#38bdf8" />
            <h3 style={{ fontSize: "1.05rem", fontWeight: 700, color: "#ffffff" }}>
              Safety Guardrails: ASHA Frontline Scope vs. Clinician-Only Orders
            </h3>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: "14px", marginTop: "12px" }}>
            <div style={{ background: "rgba(16, 185, 129, 0.05)", border: "1px solid rgba(16, 185, 129, 0.2)", borderRadius: "8px", padding: "14px" }}>
              <h4 style={{ fontSize: "0.85rem", color: "#6ee7b7", fontWeight: 700, marginBottom: "8px" }}>
                ✓ ASHA Approved First-Response Actions (Safe)
              </h4>
              <ul style={{ fontSize: "0.75rem", color: "var(--text-secondary)", lineHeight: 1.7, paddingLeft: "18px" }}>
                <li>Call 108 emergency ambulance without delay</li>
                <li>Position mother in left lateral tilt position</li>
                <li>Keep airway clear; never place objects into mouth if fitting</li>
                <li>Keep mother calm and warm; avoid exertion</li>
                <li>Accompany mother to the referral health centre</li>
              </ul>
            </div>

            <div style={{ background: "rgba(239, 68, 68, 0.05)", border: "1px solid rgba(239, 68, 68, 0.2)", borderRadius: "8px", padding: "14px" }}>
              <h4 style={{ fontSize: "0.85rem", color: "#fca5a5", fontWeight: 700, marginBottom: "8px" }}>
                ⚠️ Restricted: Requires Authorized Clinician Order Only
              </h4>
              <ul style={{ fontSize: "0.75rem", color: "var(--text-secondary)", lineHeight: 1.7, paddingLeft: "18px" }}>
                <li>IV cannulation and IV fluid administration</li>
                <li>Magnesium Sulphate (MgSO4) loading or maintenance doses</li>
                <li>Antihypertensive administration (IV Labetalol, Oral Nifedipine)</li>
                <li>Blood product cross-matching and transfusion initiation</li>
                <li>Oxytocics administration prior to delivery of infant</li>
              </ul>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};
