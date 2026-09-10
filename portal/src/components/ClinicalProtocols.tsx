import React from "react";
import { Heart, Activity, AlertOctagon, ShieldCheck, Stethoscope, ArrowRight, Layers } from "lucide-react";

export const ClinicalProtocols: React.FC = () => {
  return (
    <div className="section-container" style={{ padding: "0 0 48px 0" }}>
      {/* Header */}
      <div className="header-row" style={{ marginBottom: "32px" }}>
        <div>
          <div className="technical-label" style={{ marginBottom: "6px" }}>
            <span>04 / CLINICAL GOVERNANCE · DETERMINISTIC PROTOCOLS</span>
          </div>
          <h2 className="title-primary" style={{ fontSize: "2.1rem", margin: "0 0 8px 0" }}>
            Maternal High-Risk Pregnancy <span className="editorial-italic">Clinical Guardrails</span>
          </h2>
          <p className="subtitle" style={{ maxWidth: "860px" }}>
            Standardized MoHFW & WHO clinical protocols (Rule pack: <code style={{ fontFamily: "var(--font-mono)", background: "var(--panel-pale)", padding: "2px 6px", borderRadius: "4px", fontSize: "0.82rem", border: "1px solid var(--rule-muted)" }}>mohfw-hrp-v1.0</code>). 
            Deterministic triage evaluation executed entirely on-device, establishing firm clinical boundaries between frontline community scope and hospital-grade interventions.
          </p>
        </div>
      </div>

      {/* Triage Protocols Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(460px, 1fr))", gap: "24px", marginBottom: "32px" }}>
        
        {/* RED Danger Signs Card */}
        <div style={{
          background: "var(--panel-white)",
          border: "1px solid var(--rule-muted)",
          borderLeft: "5px solid var(--coral-accent)",
          borderRadius: "var(--radius-lg)",
          padding: "24px",
          boxShadow: "var(--shadow-sm)"
        }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{
                width: "36px",
                height: "36px",
                borderRadius: "var(--radius-pill)",
                background: "var(--coral-bg)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "var(--coral-dark)"
              }}>
                <AlertOctagon size={20} />
              </div>
              <div>
                <div className="technical-label" style={{ fontSize: "0.68rem", color: "var(--coral-dark)" }}>IMMEDIATE 108 AMBULANCE DISPATCH</div>
                <h3 style={{ fontSize: "1.2rem", fontWeight: 600, color: "var(--text-navy)", margin: 0 }}>
                  Critical Emergency (RED Triage)
                </h3>
              </div>
            </div>
            <span className="badge badge-red">P1 CRITICAL</span>
          </div>

          <p style={{ fontSize: "0.875rem", color: "var(--text-body)", lineHeight: 1.55, marginBottom: "18px" }}>
            Autonomous deterministic trigger requiring immediate 108 ambulance dispatch, automated SMS alert to the on-duty Medical Officer, and referral to a CEmOC / Blood Storage centre.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <div style={{
              background: "var(--coral-bg)",
              border: "1px solid rgba(226, 123, 112, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--coral-dark)" }}>Antepartum Hemorrhage (APH):</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Any visible vaginal bleeding occurring during the second or third trimester. Suspect placenta praevia or abruption.
              </div>
            </div>

            <div style={{
              background: "var(--coral-bg)",
              border: "1px solid rgba(226, 123, 112, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--coral-dark)" }}>Severe Hypertensive Crisis / Severe Pre-Eclampsia:</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Systolic BP ≥ 160 mmHg OR Diastolic BP ≥ 110 mmHg with headache, scotoma, or epigastric pain.
              </div>
            </div>

            <div style={{
              background: "var(--coral-bg)",
              border: "1px solid rgba(226, 123, 112, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--coral-dark)" }}>Severe Maternal Anemia (Hb &lt; 7.0 g/dL):</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Extremely low hemoglobin presenting clinical pallor and breathlessness at rest. Automatic blood bank requisition advisory.
              </div>
            </div>

            <div style={{
              background: "var(--coral-bg)",
              border: "1px solid rgba(226, 123, 112, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--coral-dark)" }}>Eclampsia & Convulsive Seizures:</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Generalized tonic-clonic convulsions or loss of consciousness during pregnancy or early postpartum.
              </div>
            </div>
          </div>
        </div>

        {/* AMBER Danger Signs Card */}
        <div style={{
          background: "var(--panel-white)",
          border: "1px solid var(--rule-muted)",
          borderLeft: "5px solid var(--amber-accent)",
          borderRadius: "var(--radius-lg)",
          padding: "24px",
          boxShadow: "var(--shadow-sm)"
        }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{
                width: "36px",
                height: "36px",
                borderRadius: "var(--radius-pill)",
                background: "var(--amber-bg)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "var(--amber-dark)"
              }}>
                <Activity size={20} />
              </div>
              <div>
                <div className="technical-label" style={{ fontSize: "0.68rem", color: "var(--amber-dark)" }}>PHC ASSESSMENT WITHIN 24 HOURS</div>
                <h3 style={{ fontSize: "1.2rem", fontWeight: 600, color: "var(--text-navy)", margin: 0 }}>
                  Moderate High-Risk (AMBER Triage)
                </h3>
              </div>
            </div>
            <span className="badge badge-amber">P2 MODERATE</span>
          </div>

          <p style={{ fontSize: "0.875rem", color: "var(--text-body)", lineHeight: 1.55, marginBottom: "18px" }}>
            Requires scheduled transport and formal physician clinical review at the Primary Health Centre (PHC) within 24 hours to prevent acute maternal-fetal decompensation.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <div style={{
              background: "var(--amber-bg)",
              border: "1px solid rgba(217, 119, 6, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--amber-dark)" }}>Stage 1 Gestational Hypertension:</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Systolic BP 140–159 mmHg OR Diastolic BP 90–109 mmHg on two readings taken 4 hours apart.
              </div>
            </div>

            <div style={{
              background: "var(--amber-bg)",
              border: "1px solid rgba(217, 119, 6, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--amber-dark)" }}>Moderate Maternal Anemia (Hb 7.0–9.9 g/dL):</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Indicative of iron deficiency anemia; oral Iron-Folic Acid (IFA) escalation or IV iron sucrose scheduling required.
              </div>
            </div>

            <div style={{
              background: "var(--amber-bg)",
              border: "1px solid rgba(217, 119, 6, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--amber-dark)" }}>Maternal Pyrexia / Fever (≥ 38.0°C / 100.4°F):</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                High risk of endemic falciparum malaria, urinary tract infection, or chorioamnionitis. Immediate RDT recommended.
              </div>
            </div>

            <div style={{
              background: "var(--amber-bg)",
              border: "1px solid rgba(217, 119, 6, 0.35)",
              padding: "12px 14px",
              borderRadius: "var(--radius-md)",
              fontSize: "0.85rem",
              color: "var(--text-navy)"
            }}>
              <strong style={{ color: "var(--amber-dark)" }}>Reduced Fetal Movement / Decreased Kick Count:</strong>
              <div style={{ marginTop: "3px", color: "var(--text-body)", fontSize: "0.82rem" }}>
                Reported reduction in daily fetal kicks in third trimester; non-stress test (NST) and ultrasound Doppler indicated.
              </div>
            </div>
          </div>
        </div>

      </div>

      {/* Frontline Scope vs Clinical Orders Guardrails */}
      <div style={{
        background: "var(--panel-white)",
        border: "1px solid var(--rule-muted)",
        borderRadius: "var(--radius-lg)",
        padding: "28px",
        marginBottom: "32px",
        boxShadow: "var(--shadow-sm)"
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
          <ShieldCheck size={22} color="var(--primary-blue)" />
          <h3 style={{ fontSize: "1.2rem", fontWeight: 600, color: "var(--text-navy)", margin: 0 }}>
            Scope of Practice & Clinical Safety <span className="editorial-italic">Guardrails</span>
          </h3>
        </div>
        <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", margin: "0 0 20px 0" }}>
          SakhiCare enforces unambiguous role divisions. Community ASHA workers are equipped for triage and stabilizing actions; pharmaceutical prescriptions and invasive procedures strictly require authenticated Medical Officer authorization.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px" }}>
          
          {/* Approved ASHA Frontline Actions */}
          <div style={{
            background: "var(--seafoam-bg)",
            border: "1px solid rgba(31, 95, 88, 0.3)",
            borderRadius: "var(--radius-md)",
            padding: "20px"
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
              <Heart size={18} color="var(--seafoam-dark)" />
              <h4 style={{ fontSize: "0.95rem", fontWeight: 600, color: "var(--seafoam-dark)", margin: 0 }}>
                Approved ASHA First-Response Actions (Safe Frontline Scope)
              </h4>
            </div>
            <ul style={{
              margin: 0,
              paddingLeft: "20px",
              fontSize: "0.825rem",
              color: "var(--text-navy)",
              lineHeight: 1.8
            }}>
              <li><strong>Call 108 Emergency Ambulance:</strong> Dial immediately and record reference number.</li>
              <li><strong>Left Lateral Tilt Position:</strong> Position patient on left side to relieve vena cava compression.</li>
              <li><strong>Airway & Convulsion Safety:</strong> Keep airway patent; never insert spoons or fingers into mouth.</li>
              <li><strong>Thermal Protection & Reassurance:</strong> Keep patient warm, calm, and resting; avoid exertion.</li>
              <li><strong>Physical Escort:</strong> Accompany mother and family directly in the transport vehicle to the facility.</li>
              <li><strong>Paper/Digital MCP Handover:</strong> Present maternal records to receiving medical staff.</li>
            </ul>
          </div>

          {/* Restricted Clinician-Only Orders */}
          <div style={{
            background: "var(--coral-bg)",
            border: "1px solid rgba(226, 123, 112, 0.35)",
            borderRadius: "var(--radius-md)",
            padding: "20px"
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
              <Stethoscope size={18} color="var(--coral-dark)" />
              <h4 style={{ fontSize: "0.95rem", fontWeight: 600, color: "var(--coral-dark)", margin: 0 }}>
                Restricted: Requires Medical Officer / Clinician Order Only
              </h4>
            </div>
            <ul style={{
              margin: 0,
              paddingLeft: "20px",
              fontSize: "0.825rem",
              color: "var(--text-navy)",
              lineHeight: 1.8
            }}>
              <li><strong>Magnesium Sulphate (MgSO4):</strong> 4g IV / 10g IM loading dose requires physician prescription.</li>
              <li><strong>Antihypertensive Administration:</strong> Oral Labetalol 100mg / Nifedipine 10mg strictly clinician-ordered.</li>
              <li><strong>IV Cannulation & Fluid Resuscitation:</strong> 16G/18G IV line insertion and Ringer's Lactate infusion.</li>
              <li><strong>Blood Component Cross-Matching:</strong> PRBC / FFP requisition and transfusion oversight.</li>
              <li><strong>Oxytocin & Uterotonics:</strong> Administration contraindicated prior to active delivery of infant.</li>
              <li><strong>Diagnostic Ultrasound / Doppler:</strong> Obstetric assessment by certified sonologist/OB-GYN.</li>
            </ul>
          </div>

        </div>
      </div>

      {/* System Metaphor & Architecture Flow */}
      <div style={{
        background: "var(--panel-pale)",
        border: "1px solid var(--rule-muted)",
        borderRadius: "var(--radius-lg)",
        padding: "28px"
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
          <Layers size={20} color="var(--navy-deep)" />
          <h3 style={{ fontSize: "1.15rem", fontWeight: 600, color: "var(--text-navy)", margin: 0 }}>
            Deterministic Engine Architecture: <span className="editorial-italic">Zero Cloud Dependence</span>
          </h3>
        </div>
        <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", margin: "0 0 20px 0" }}>
          To operate reliably in remote rural terrain with zero cellular reception, clinical triage rules evaluate locally inside the Android ASHA client without remote API latency.
        </p>

        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
          gap: "16px",
          alignItems: "center"
        }}>
          <div style={{
            background: "var(--panel-white)",
            border: "1px solid var(--rule-muted)",
            borderRadius: "var(--radius-md)",
            padding: "16px",
            textAlign: "center"
          }}>
            <div className="technical-label" style={{ fontSize: "0.68rem" }}>STEP 01</div>
            <div style={{ fontWeight: 600, color: "var(--text-navy)", fontSize: "0.9rem", marginTop: "4px" }}>Vitals Observation</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>BP, Hb, Temp, Symptoms captured offline</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{
            background: "var(--panel-white)",
            border: "1px solid var(--rule-muted)",
            borderRadius: "var(--radius-md)",
            padding: "16px",
            textAlign: "center"
          }}>
            <div className="technical-label" style={{ fontSize: "0.68rem" }}>STEP 02</div>
            <div style={{ fontWeight: 600, color: "var(--text-navy)", fontSize: "0.9rem", marginTop: "4px" }}>Local Deterministic Engine</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Evaluated against MoHFW ruleset in SQLCipher</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{
            background: "var(--panel-white)",
            border: "1px solid var(--rule-muted)",
            borderRadius: "var(--radius-md)",
            padding: "16px",
            textAlign: "center"
          }}>
            <div className="technical-label" style={{ fontSize: "0.68rem" }}>STEP 03</div>
            <div style={{ fontWeight: 600, color: "var(--text-navy)", fontSize: "0.9rem", marginTop: "4px" }}>Immediate Guidance</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>RED / AMBER prompt + 108 dial + lateral tilt</div>
          </div>

          <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
            <ArrowRight size={20} style={{ margin: "0 auto" }} />
          </div>

          <div style={{
            background: "var(--panel-white)",
            border: "1px solid var(--rule-muted)",
            borderRadius: "var(--radius-md)",
            padding: "16px",
            textAlign: "center"
          }}>
            <div className="technical-label" style={{ fontSize: "0.68rem" }}>STEP 04</div>
            <div style={{ fontWeight: 600, color: "var(--text-navy)", fontSize: "0.9rem", marginTop: "4px" }}>Sync & Coordination</div>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "4px" }}>Syncs to Care Desk & 108 dispatch on signal</div>
          </div>
        </div>
      </div>
    </div>
  );
};
