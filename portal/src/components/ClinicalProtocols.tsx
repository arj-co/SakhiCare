import React from "react";
import { Heart, Activity, AlertOctagon, ShieldCheck, Stethoscope, ArrowRight, BookOpen } from "lucide-react";

export const ClinicalProtocols: React.FC = () => {
  return (
    <div>
      {/* Title */}
      <div className="page-title-row">
        <div>
          <h1 className="page-title">MoHFW & WHO Maternal Clinical Protocols</h1>
          <p className="page-subtitle">
            Deterministic rule pack: <code style={{ fontFamily: "var(--font-mono)", background: "var(--bg-muted)", padding: "2px 6px", borderRadius: "4px" }}>mohfw-hrp-v1.0</code>. 
            Standardized triage criteria running locally on ASHA Android devices without cloud dependency.
          </p>
        </div>
      </div>

      {/* Protocols Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px", marginBottom: "24px" }}>
        
        {/* RED Emergency Triage Card */}
        <div style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderLeft: "4px solid var(--red-primary)", borderRadius: "var(--radius-lg)", padding: "24px", boxShadow: "var(--shadow-xs)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "36px", height: "36px", borderRadius: "var(--radius-md)", background: "var(--red-bg)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--red-primary)" }}>
                <AlertOctagon size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: "1.1rem", margin: 0, color: "var(--text-main)" }}>Critical Emergency (RED Triage)</h3>
                <span style={{ fontSize: "0.75rem", color: "var(--red-text)", fontWeight: 600 }}>Immediate 108 Ambulance Dispatch</span>
              </div>
            </div>
            <span className="badge badge-red">P1 CRITICAL</span>
          </div>

          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginBottom: "16px", lineHeight: 1.5 }}>
            Triggers automatic emergency ambulance coordination, SMS escalation to the on-duty Medical Officer, and referral to a CEmOC / Blood Storage facility.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Antepartum Hemorrhage (APH):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Any vaginal bleeding occurring during the 2nd or 3rd trimester. Immediate CEmOC transfer indicated.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Severe Hypertensive Crisis / Eclampsia:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Systolic BP ≥ 160 mmHg OR Diastolic BP ≥ 110 mmHg with severe headache, blurred vision, or seizures.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Severe Maternal Anemia (Hb &lt; 7.0 g/dL):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Extreme pallor and resting breathlessness. Blood bank requisition and intravenous iron or blood product alert.
              </div>
            </div>

            <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--red-text)" }}>Convulsions / Fits / Unconsciousness:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Active or recent eclamptic seizures; requires airway protection, left lateral tilt, and physician MgSO4 loading.
              </div>
            </div>
          </div>
        </div>

        {/* AMBER Moderate High-Risk Card */}
        <div style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderLeft: "4px solid var(--amber-primary)", borderRadius: "var(--radius-lg)", padding: "24px", boxShadow: "var(--shadow-xs)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "36px", height: "36px", borderRadius: "var(--radius-md)", background: "var(--amber-bg)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--amber-primary)" }}>
                <Activity size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: "1.1rem", margin: 0, color: "var(--text-main)" }}>Moderate High-Risk (AMBER Triage)</h3>
                <span style={{ fontSize: "0.75rem", color: "var(--amber-text)", fontWeight: 600 }}>PHC Assessment Within 24 Hours</span>
              </div>
            </div>
            <span className="badge badge-amber">P2 MODERATE</span>
          </div>

          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginBottom: "16px", lineHeight: 1.5 }}>
            Requires primary health centre physician assessment within 24 hours to prevent maternal-fetal decompensation.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Gestational Hypertension (Stage 1):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Systolic BP 140–159 mmHg OR Diastolic BP 90–109 mmHg on two repeat measurements.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Moderate Anemia (Hb 7.0–9.9 g/dL):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Oral iron-folic acid escalation or injectable iron sucrose scheduling at nearest PHC.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Maternal Pyrexia / Fever (≥ 38.0°C):</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Rapid diagnostic test (RDT) for malaria and urine screening indicated.
              </div>
            </div>

            <div style={{ background: "var(--amber-bg)", border: "1px solid var(--amber-border)", borderRadius: "var(--radius-md)", padding: "10px 14px", fontSize: "0.85rem" }}>
              <strong style={{ color: "var(--amber-text)" }}>Reduced Fetal Movement:</strong>
              <div style={{ color: "var(--text-body)", fontSize: "0.8rem", marginTop: "2px" }}>
                Decreased kick count in 3rd trimester; non-stress test (NST) evaluation indicated.
              </div>
            </div>
          </div>
        </div>

      </div>

      {/* Safety Guardrails Banner */}
      <div style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderRadius: "var(--radius-lg)", padding: "24px", marginBottom: "24px", boxShadow: "var(--shadow-xs)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
          <ShieldCheck size={20} color="var(--primary-blue)" />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--text-main)" }}>
            Frontline Scope of Practice & Safety Guardrails
          </h3>
        </div>
        <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginBottom: "16px" }}>
          SakhiCare strictly divides responsibilities between community ASHA workers and authorized physicians to protect patient safety.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "16px" }}>
          <div style={{ background: "var(--green-bg)", border: "1px solid var(--green-border)", borderRadius: "var(--radius-md)", padding: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
              <Heart size={16} color="var(--green-text)" />
              <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--green-text)" }}>
                Approved ASHA Frontline Actions (Safe Scope)
              </h4>
            </div>
            <ul style={{ margin: 0, paddingLeft: "18px", fontSize: "0.8125rem", color: "var(--text-body)", lineHeight: 1.7 }}>
              <li><strong>Dial 108 Emergency Ambulance:</strong> Call immediately and record ticket number.</li>
              <li><strong>Left Lateral Tilt Position:</strong> Relieve aortocaval compression to improve placental perfusion.</li>
              <li><strong>Airway & Convulsion Care:</strong> Keep airway clear; do not insert objects into mouth during fits.</li>
              <li><strong>Thermal Care & Comfort:</strong> Keep patient warm and calm; avoid physical exertion.</li>
              <li><strong>Accompany to Health Facility:</strong> Accompany mother and family in the transport vehicle.</li>
            </ul>
          </div>

          <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-md)", padding: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
              <Stethoscope size={16} color="var(--red-text)" />
              <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--red-text)" }}>
                Restricted: Clinician Orders Only
              </h4>
            </div>
            <ul style={{ margin: 0, paddingLeft: "18px", fontSize: "0.8125rem", color: "var(--text-body)", lineHeight: 1.7 }}>
              <li><strong>Magnesium Sulphate (MgSO4):</strong> Loading and maintenance doses require physician order.</li>
              <li><strong>Antihypertensive Administration:</strong> Oral Labetalol or Nifedipine requires doctor prescription.</li>
              <li><strong>IV Cannulation & Fluid Boluses:</strong> Intravenous fluid resuscitation by trained nursing/MO staff.</li>
              <li><strong>Blood Cross-Match & Transfusion:</strong> Blood storage centre authorization and cross-matching.</li>
              <li><strong>Uterotonics Prior to Delivery:</strong> Strictly contraindicated prior to infant delivery.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Offline Architecture Flowchart */}
      <div style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderRadius: "var(--radius-lg)", padding: "24px", boxShadow: "var(--shadow-xs)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
          <BookOpen size={20} color="var(--primary-blue)" />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--text-main)" }}>
            Zero-Cloud Offline Architecture
          </h3>
        </div>
        <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginBottom: "20px" }}>
          In remote villages with zero network coverage, clinical triage is executed locally on-device via deterministic rule sets.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "16px", alignItems: "center" }}>
          <div style={{ background: "var(--bg-muted)", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "16px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: 600 }}>STEP 1</div>
            <div style={{ fontWeight: 600, color: "var(--text-main)", marginTop: "4px" }}>Vitals Observation</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>BP, Hb, Temp captured offline</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-muted)", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "16px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: 600 }}>STEP 2</div>
            <div style={{ fontWeight: 600, color: "var(--text-main)", marginTop: "4px" }}>Local Deterministic Engine</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Evaluated in SQLCipher DB</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-muted)", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "16px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: 600 }}>STEP 3</div>
            <div style={{ fontWeight: 600, color: "var(--text-main)", marginTop: "4px" }}>Immediate Guidance</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>RED/AMBER guidance shown</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{ background: "var(--bg-muted)", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "16px", textAlign: "center" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: 600 }}>STEP 4</div>
            <div style={{ fontWeight: 600, color: "var(--text-main)", marginTop: "4px" }}>Sync to Care Desk</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Syncs & alerts 108 on connectivity</div>
          </div>
        </div>
      </div>

    </div>
  );
};
