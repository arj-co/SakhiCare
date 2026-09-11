import React from "react";
import { Heart, Activity, AlertOctagon, ShieldCheck, Stethoscope, ArrowRight, BookOpen } from "lucide-react";

export const ClinicalProtocols: React.FC = () => {
  return (
    <div>
      {/* Eyra Section Label & Page Title */}
      <div className="page-title-row">
        <div>
          <div className="eyra-section-label">Clinical Governance</div>
          <h1 className="page-title">MoHFW & WHO Maternal Clinical Protocols</h1>
          <p className="page-subtitle">
            Standardized clinical triage rules (Rule pack: <code style={{ fontFamily: "var(--font-mono)", background: "var(--bg-card-warm)", padding: "2px 8px", borderRadius: "4px", border: "1px solid var(--border-subtle)" }}>mohfw-hrp-v1.0</code>). 
            Evaluated on-device via local deterministic logic without remote API latency.
          </p>
        </div>
      </div>

      {/* Protocols Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px", marginBottom: "28px" }}>
        
        {/* RED Emergency Triage Card */}
        <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderLeft: "5px solid var(--red-primary)", borderRadius: "var(--radius-lg)", padding: "26px", boxShadow: "var(--shadow-subtle)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "38px", height: "38px", borderRadius: "var(--radius-pill)", background: "var(--red-bg)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--red-primary)" }}>
                <AlertOctagon size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: "1.2rem", margin: 0, fontFamily: "var(--font-serif)" }}>Critical Emergency (RED Triage)</h3>
                <span style={{ fontSize: "0.75rem", color: "var(--red-text)", fontWeight: 600 }}>Immediate 108 Ambulance Dispatch</span>
              </div>
            </div>
            <span className="badge badge-red">P1 CRITICAL</span>
          </div>

          <p style={{ fontSize: "0.875rem", color: "var(--text-muted)", marginBottom: "16px", lineHeight: 1.6 }}>
            Autonomous deterministic trigger requiring immediate 108 emergency ambulance dispatch, automated SMS escalation to the Medical Officer, and referral to a CEmOC / Blood Storage centre.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Antepartum Hemorrhage (APH):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Any vaginal bleeding occurring during the second or third trimester. Suspect placenta praevia or abruption.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Severe Hypertensive Crisis / Severe Pre-Eclampsia:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Systolic BP ≥ 160 mmHg OR Diastolic BP ≥ 110 mmHg with severe headache, scotoma, or epigastric pain.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Severe Maternal Anemia (Hb &lt; 7.0 g/dL):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Marked pallor, resting breathlessness, and tachycardia. Blood bank requisition and intravenous therapy alert.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Eclampsia & Convulsive Seizures:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Generalized convulsions or loss of consciousness; requires airway clearance, lateral tilt, and physician MgSO4 protocol.
              </div>
            </div>
          </div>
        </div>

        {/* AMBER Moderate High-Risk Card */}
        <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderLeft: "5px solid var(--amber-primary)", borderRadius: "var(--radius-lg)", padding: "26px", boxShadow: "var(--shadow-subtle)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "38px", height: "38px", borderRadius: "var(--radius-pill)", background: "var(--amber-bg)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--amber-primary)" }}>
                <Activity size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: "1.2rem", margin: 0, fontFamily: "var(--font-serif)" }}>Moderate High-Risk (AMBER Triage)</h3>
                <span style={{ fontSize: "0.75rem", color: "var(--amber-text)", fontWeight: 600 }}>PHC Assessment Within 24 Hours</span>
              </div>
            </div>
            <span className="badge badge-amber">P2 MODERATE</span>
          </div>

          <p style={{ fontSize: "0.875rem", color: "var(--text-muted)", marginBottom: "16px", lineHeight: 1.6 }}>
            Requires primary health centre physician assessment within 24 hours to prevent maternal-fetal decompensation.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Gestational Hypertension (Stage 1):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Systolic BP 140–159 mmHg OR Diastolic BP 90–109 mmHg on two repeat measurements.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Moderate Maternal Anemia (Hb 7.0–9.9 g/dL):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Oral iron-folic acid escalation or injectable iron sucrose scheduling at nearest PHC.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Maternal Pyrexia / Fever (≥ 38.0°C):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Endemic malaria or urinary tract infection indication; rapid diagnostic test (RDT) advised.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "12px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Reduced Fetal Movement:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.825rem", marginTop: "2px" }}>
                Decreased kick count in 3rd trimester; non-stress test (NST) and ultrasound Doppler indicated.
              </div>
            </div>
          </div>
        </div>

      </div>

      {/* Safety Guardrails Banner */}
      <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-lg)", padding: "26px", marginBottom: "28px", boxShadow: "var(--shadow-subtle)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
          <ShieldCheck size={20} color="var(--medical-teal)" />
          <h3 style={{ margin: 0, fontSize: "1.25rem", fontFamily: "var(--font-serif)" }}>
            Frontline Scope of Practice & Safety Guardrails
          </h3>
        </div>
        <p style={{ fontSize: "0.875rem", color: "var(--text-muted)", marginBottom: "18px" }}>
          SakhiCare strictly divides responsibilities between community ASHA workers and authorized medical officers to protect patient safety.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "16px" }}>
          <div style={{ background: "var(--green-bg)", border: "1px solid var(--green-border)", borderRadius: "var(--radius-md)", padding: "18px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
              <Heart size={16} color="var(--green-text)" />
              <h4 style={{ margin: 0, fontSize: "0.95rem", color: "var(--green-text)" }}>
                Approved ASHA Frontline Actions (Safe Scope)
              </h4>
            </div>
            <ul style={{ margin: 0, paddingLeft: "18px", fontSize: "0.8125rem", color: "var(--text-body)", lineHeight: 1.8 }}>
              <li><strong>Dial 108 Emergency Ambulance:</strong> Call immediately and record ticket number.</li>
              <li><strong>Left Lateral Tilt Position:</strong> Relieve aortocaval compression to improve placental perfusion.</li>
              <li><strong>Airway & Convulsion Care:</strong> Keep airway clear; do not insert spoons or fingers into mouth.</li>
              <li><strong>Thermal Care & Comfort:</strong> Keep patient warm and calm; avoid physical exertion.</li>
              <li><strong>Physical Escort:</strong> Accompany mother and family in the transport vehicle to referral hospital.</li>
            </ul>
          </div>

          <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "18px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
              <Stethoscope size={16} color="var(--red-text)" />
              <h4 style={{ margin: 0, fontSize: "0.95rem", color: "var(--red-text)" }}>
                Restricted: Clinician Orders Only
              </h4>
            </div>
            <ul style={{ margin: 0, paddingLeft: "18px", fontSize: "0.8125rem", color: "var(--text-body)", lineHeight: 1.8 }}>
              <li><strong>Magnesium Sulphate (MgSO4):</strong> Loading and maintenance doses require physician prescription.</li>
              <li><strong>Antihypertensive Administration:</strong> Oral Labetalol or Nifedipine strictly clinician-ordered.</li>
              <li><strong>IV Cannulation & Fluid Resuscitation:</strong> Intravenous lines and Ringer's Lactate infusion.</li>
              <li><strong>Blood Cross-Match & Transfusion:</strong> Blood storage centre authorization and cross-matching.</li>
              <li><strong>Uterotonics Prior to Delivery:</strong> Strictly contraindicated prior to active infant delivery.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Offline Architecture Flowchart */}
      <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-lg)", padding: "26px", boxShadow: "var(--shadow-subtle)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
          <BookOpen size={20} color="var(--medical-teal)" />
          <h3 style={{ margin: 0, fontSize: "1.25rem", fontFamily: "var(--font-serif)" }}>
            Deterministic Engine Architecture: Zero Cloud Dependence
          </h3>
        </div>
        <p style={{ fontSize: "0.875rem", color: "var(--text-muted)", marginBottom: "20px" }}>
          In remote villages with zero network coverage, clinical triage is executed locally on-device via deterministic rule sets.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "16px", alignItems: "center" }}>
          <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "18px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--medical-teal)", fontWeight: 700 }}>STEP 1</div>
            <div style={{ fontWeight: 600, color: "var(--text-primary)", marginTop: "4px" }}>Vitals Observation</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>BP, Hb, Temp captured offline</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "18px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--medical-teal)", fontWeight: 700 }}>STEP 2</div>
            <div style={{ fontWeight: 600, color: "var(--text-primary)", marginTop: "4px" }}>Local Deterministic Engine</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Evaluated in SQLCipher DB</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "18px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--medical-teal)", fontWeight: 700 }}>STEP 3</div>
            <div style={{ fontWeight: 600, color: "var(--text-primary)", marginTop: "4px" }}>Immediate Guidance</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>RED/AMBER guidance shown</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "18px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--medical-teal)", fontWeight: 700 }}>STEP 4</div>
            <div style={{ fontWeight: 600, color: "var(--text-primary)", marginTop: "4px" }}>Sync to Care Desk</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Syncs & alerts 108 on signal</div>
          </div>
        </div>
      </div>

    </div>
  );
};
