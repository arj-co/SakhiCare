import React, { useState, useEffect } from "react";
import type { PregnancyCase, UserProfile } from "../api";
import { 
  acknowledgeCase, 
  updateTransport, 
  getAudioStreamUrl, 
  fetchCaseDetail,
  fetchFhirBundle
} from "../api";
import { 
  X, 
  Mic, 
  Truck, 
  Stethoscope, 
  Send, 
  History, 
  FileCode2, 
  ShieldCheck, 
  Download,
  Check
} from "lucide-react";

interface CaseDetailModalProps {
  caseItem: PregnancyCase;
  currentUser: UserProfile;
  onClose: () => void;
  onCaseUpdated: (updatedCase: PregnancyCase) => void;
}

export const CaseDetailModal: React.FC<CaseDetailModalProps> = ({
  caseItem: initialCase,
  currentUser,
  onClose,
  onCaseUpdated,
}) => {
  const [currentCase, setCurrentCase] = useState<PregnancyCase>(initialCase);
  const [advisoryText, setAdvisoryText] = useState("");
  const [referralFacility, setReferralFacility] = useState("FAC-01");
  const [isSubmittingAdvisory, setIsSubmittingAdvisory] = useState(false);

  // Transport form state
  const [vehicleId, setVehicleId] = useState("108-AMB-Rampur-04");
  const [destinationFacility, setDestinationFacility] = useState("CHC Rampur");
  const [driverPhone, setDriverPhone] = useState("9876543299");
  const [isSubmittingTransport, setIsSubmittingTransport] = useState(false);

  // FHIR Export preview state
  const [fhirModalOpen, setFhirModalOpen] = useState(false);
  const [fhirData, setFhirData] = useState<Record<string, unknown> | null>(null);
  const [fhirLoading, setFhirLoading] = useState(false);
  const [copiedFhir, setCopiedFhir] = useState(false);

  const [audioError, setAudioError] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const caseId = currentCase.case_id || currentCase.patient_id || "";
  const assessment = currentCase.assessment;
  const risk = assessment?.risk_level || "GREEN";
  const dangerSigns = assessment?.danger_signs || {};

  // Quick order advisory templates
  const quickTemplates = [
    "Administer oral labetalol 100mg stat; maintain left lateral tilt; immediate 108 transfer to CHC.",
    "Administer paracetamol 500mg; perform rapid malaria & urine albumin test at Sub-centre.",
    "Keep patient flat; do not give oral fluids; prepare oxygen; emergency blood transfusion alert.",
    "Immediate IM dexamethasone 6mg for fetal lung maturation; arrange obstetrician consultation."
  ];

  // Refresh case detail on open
  useEffect(() => {
    async function loadLatest() {
      try {
        const latest = await fetchCaseDetail(caseId);
        setCurrentCase(latest);
      } catch (err) {
        console.error("Failed to fetch case detail", err);
      }
    }
    loadLatest();
  }, [caseId]);

  // Handle Advisory Submission
  const handleSubmitAdvisory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!advisoryText.trim()) return;

    setIsSubmittingAdvisory(true);
    setErrorMessage(null);
    try {
      const updated = await acknowledgeCase(caseId, advisoryText, referralFacility);
      setCurrentCase(updated);
      onCaseUpdated(updated);
      setSuccessMessage("Medical Officer advisory successfully recorded and pushed to frontline ASHA worker!");
      setAdvisoryText("");
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : "Failed to record doctor advisory");
    } finally {
      setIsSubmittingAdvisory(false);
    }
  };

  // Handle Transport Coordination Submission
  const handleSubmitTransport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!vehicleId.trim() || !destinationFacility.trim()) return;

    setIsSubmittingTransport(true);
    setErrorMessage(null);
    try {
      const updated = await updateTransport(caseId, vehicleId, destinationFacility, driverPhone);
      setCurrentCase(updated);
      onCaseUpdated(updated);
      setSuccessMessage(`108 Transport ${vehicleId} successfully dispatched to ${destinationFacility}!`);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : "Failed to update transport status");
    } finally {
      setIsSubmittingTransport(false);
    }
  };

  // Fetch and show FHIR R4 Bundle
  const handleOpenFhir = async () => {
    setFhirModalOpen(true);
    setFhirLoading(true);
    try {
      const bundle = await fetchFhirBundle(caseId);
      setFhirData(bundle);
    } catch (err) {
      setFhirData({ error: "Failed to export FHIR bundle", detail: String(err) });
    } finally {
      setFhirLoading(false);
    }
  };

  const handleCopyFhir = () => {
    if (fhirData) {
      navigator.clipboard.writeText(JSON.stringify(fhirData, null, 2));
      setCopiedFhir(true);
      setTimeout(() => setCopiedFhir(false), 2000);
    }
  };

  const canDoctorAct = currentUser.role === "MEDICAL_OFFICER" || currentUser.role === "ADMIN";
  const canDispatchAct = currentUser.role === "DISPATCHER" || currentUser.role === "MEDICAL_OFFICER" || currentUser.role === "ADMIN";

  return (
    <div className="modal-overlay">
      <div 
        style={{
          width: "100%",
          maxWidth: "1120px",
          maxHeight: "92vh",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
          background: "var(--panel-white)",
          border: "1px solid var(--rule-muted)",
          borderRadius: "var(--radius-lg)",
          boxShadow: "var(--shadow-lg)"
        }}
      >
        {/* Modal Header */}
        <div style={{
          padding: "18px 26px",
          background: "var(--panel-pale)",
          borderBottom: "1px solid var(--rule-muted)",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: "14px"
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <span className={risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")}>
              {risk} RISK
            </span>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <h2 style={{ fontSize: "1.3rem", fontWeight: 600, color: "var(--navy-deep)", margin: 0 }}>
                  {currentCase.patient_name}
                </h2>
                <span className="mono-badge">
                  {caseId}
                </span>
                {currentCase.is_demo && (
                  <span style={{ fontSize: "0.68rem", background: "var(--panel-white)", color: "var(--navy-deep)", border: "1px solid var(--rule-muted)", padding: "1px 6px", borderRadius: "4px", fontWeight: 600 }}>
                    DEMO
                  </span>
                )}
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "0.8rem", color: "var(--text-muted)", marginTop: "3px" }}>
                <span>Village: <strong style={{ color: "var(--text-body)" }}>{currentCase.village}</strong></span>
                {currentCase.age_years && <span>Age: {currentCase.age_years}y</span>}
                {currentCase.gestational_age_weeks && <span>Gestation: <strong style={{ color: "var(--text-body)" }}>{currentCase.gestational_age_weeks}w</strong></span>}
                {currentCase.gravida !== null && currentCase.gravida !== undefined && <span>G{currentCase.gravida}P{currentCase.para ?? 0}</span>}
              </div>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <button
              onClick={handleOpenFhir}
              className="btn-outline"
              style={{ fontSize: "0.75rem", padding: "6px 14px" }}
              title="Export FHIR R4 Bundle"
            >
              <FileCode2 size={13} color="var(--navy-deep)" />
              <span>FHIR R4</span>
            </button>

            <button
              onClick={onClose}
              className="btn-close"
              title="Close modal"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Modal Body: Two-Column Responsive Layout */}
        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(380px, 1fr))",
          gap: "20px",
          padding: "24px",
          overflowY: "auto",
          flex: 1
        }}>

          {/* LEFT COLUMN: Clinical Assessment & Audio Artifact */}
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            
            {/* Vitals Summary Card */}
            <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div className="technical-label" style={{ marginBottom: "10px" }}>
                <Stethoscope size={13} color="var(--navy-deep)" />
                <span>MATERNAL CLINICAL VITALS</span>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "16px" }}>
                {/* BP */}
                <div style={{
                  padding: "12px",
                  borderRadius: "var(--radius-md)",
                  background: assessment?.blood_pressure?.includes("16") || assessment?.blood_pressure?.includes("11")
                    ? "var(--coral-bg)"
                    : "var(--panel-white)",
                  border: assessment?.blood_pressure?.includes("16")
                    ? "1px solid rgba(226, 123, 112, 0.5)"
                    : "1px solid var(--rule-muted)"
                }}>
                  <div style={{ fontSize: "0.68rem", color: "var(--text-muted)", textTransform: "uppercase", fontFamily: "var(--font-mono)" }}>Blood Pressure</div>
                  <div style={{ fontSize: "1.2rem", fontWeight: 700, color: "var(--navy-deep)", marginTop: "2px", fontFamily: "var(--font-mono)" }}>
                    {assessment?.blood_pressure || "Unmeasured"}
                  </div>
                  {assessment?.blood_pressure?.includes("16") && (
                    <span style={{ fontSize: "0.68rem", color: "var(--coral-dark)", fontWeight: 700 }}>
                      ⚠️ Severe Hypertensive Crisis
                    </span>
                  )}
                </div>

                {/* Hb */}
                <div style={{
                  padding: "12px",
                  borderRadius: "var(--radius-md)",
                  background: assessment?.haemoglobin && assessment.haemoglobin < 7.0
                    ? "var(--coral-bg)"
                    : "var(--panel-white)",
                  border: assessment?.haemoglobin && assessment.haemoglobin < 7.0
                    ? "1px solid rgba(226, 123, 112, 0.5)"
                    : "1px solid var(--rule-muted)"
                }}>
                  <div style={{ fontSize: "0.68rem", color: "var(--text-muted)", textTransform: "uppercase", fontFamily: "var(--font-mono)" }}>Haemoglobin</div>
                  <div style={{ fontSize: "1.2rem", fontWeight: 700, color: "var(--navy-deep)", marginTop: "2px", fontFamily: "var(--font-mono)" }}>
                    {assessment?.haemoglobin ? `${assessment.haemoglobin} g/dL` : "Unmeasured"}
                  </div>
                  {assessment?.haemoglobin && assessment.haemoglobin < 7.0 && (
                    <span style={{ fontSize: "0.68rem", color: "var(--coral-dark)", fontWeight: 700 }}>
                      ⚠️ Severe Maternal Anemia
                    </span>
                  )}
                </div>
              </div>

              {/* Danger Signs Checklist */}
              <div>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginBottom: "8px", fontWeight: 600, fontFamily: "var(--font-mono)" }}>
                  OBSERVED DANGER SIGNS:
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                  {Object.entries(dangerSigns).map(([signKey, isPresent]) => {
                    const label = signKey.replace(/_/g, " ");
                    return (
                      <span
                        key={signKey}
                        style={{
                          fontSize: "0.72rem",
                          padding: "3px 10px",
                          borderRadius: "var(--radius-pill)",
                          background: isPresent ? "var(--coral-bg)" : "var(--panel-white)",
                          color: isPresent ? "var(--coral-dark)" : "var(--text-muted)",
                          border: isPresent ? "1px solid rgba(226, 123, 112, 0.4)" : "1px solid var(--rule-muted)",
                          fontWeight: isPresent ? 700 : 500,
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "5px"
                        }}
                      >
                        {isPresent ? "⚠️" : "✓"} {label}
                      </span>
                    );
                  })}
                </div>
              </div>

            </div>

            {/* MoHFW Clinical Triage Protocol Rationale */}
            <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div className="technical-label" style={{ marginBottom: "10px" }}>
                <ShieldCheck size={13} color="var(--navy-deep)" />
                <span>CLINICAL TRIAGE RATIONALE · MOHFW PROTOCOL</span>
              </div>

              <div style={{ fontSize: "0.85rem", color: "var(--text-body)", lineHeight: 1.5, marginBottom: "12px" }}>
                {assessment?.clinical_rationale || "Clinical assessment pending triage evaluation."}
              </div>

              {assessment?.recommended_protocol && (
                <div style={{
                  background: "var(--panel-pale)",
                  border: "1px solid var(--rule-muted)",
                  borderRadius: "var(--radius-sm)",
                  padding: "12px",
                  marginBottom: "12px"
                }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--text-navy)", fontWeight: 700, textTransform: "uppercase", marginBottom: "4px" }}>
                    Recommended Clinical Action:
                  </div>
                  <div style={{ fontSize: "0.825rem", color: "var(--text-body)" }}>
                    {assessment.recommended_protocol}
                  </div>
                </div>
              )}

              {/* ASHA Safe Actions vs Clinician Directed Actions */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                <div style={{ background: "var(--panel-white)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-sm)", padding: "10px" }}>
                  <div style={{ fontSize: "0.68rem", color: "var(--seafoam-dark)", fontWeight: 700, textTransform: "uppercase", marginBottom: "4px" }}>
                    ✓ Frontline Safe Actions:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: "16px", fontSize: "0.75rem", color: "var(--text-muted)" }}>
                    {(assessment?.asha_safe_actions || ["Call 108 emergency ambulance", "Place mother in left lateral tilt", "Accompany to primary facility"]).map((act, i) => (
                      <li key={i} style={{ marginBottom: "2px" }}>{act}</li>
                    ))}
                  </ul>
                </div>

                <div style={{ background: "var(--panel-white)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-sm)", padding: "10px" }}>
                  <div style={{ fontSize: "0.68rem", color: "var(--coral-dark)", fontWeight: 700, textTransform: "uppercase", marginBottom: "4px" }}>
                    ⚡ Clinician-Directed Only:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: "16px", fontSize: "0.75rem", color: "var(--text-muted)" }}>
                    {(assessment?.clinician_directed_actions || ["IV cannulation & fluids", "Magnesium Sulphate loading dose", "Antihypertensive administration"]).map((act, i) => (
                      <li key={i} style={{ marginBottom: "2px" }}>{act}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>

            {/* Frontline Voice Note Audio Player */}
            {currentCase.audio_artifact && (
              <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                  <div className="technical-label">
                    <Mic size={13} color="var(--navy-deep)" />
                    <span>FRONTLINE VOICE NOTE RECORDING</span>
                  </div>
                  <span style={{ fontSize: "0.7rem", color: "var(--text-muted)", fontFamily: "var(--font-mono)" }}>
                    90-DAY RETENTION
                  </span>
                </div>

                {currentCase.audio_artifact.transcript && (
                  <div style={{
                    background: "var(--panel-pale)",
                    border: "1px solid var(--rule-muted)",
                    borderRadius: "var(--radius-sm)",
                    padding: "10px 12px",
                    fontSize: "0.825rem",
                    color: "var(--text-body)",
                    marginBottom: "10px",
                    fontStyle: "italic"
                  }}>
                    "{currentCase.audio_artifact.transcript}"
                  </div>
                )}

                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <audio
                    controls
                    src={getAudioStreamUrl(caseId)}
                    onError={() => setAudioError(true)}
                    style={{ width: "100%", height: "36px" }}
                  />
                </div>
                {audioError && (
                  <div style={{ fontSize: "0.72rem", color: "var(--coral-dark)", marginTop: "4px" }}>
                    Audio playback unavailable; encrypted voice artifact retained on ASHA local storage.
                  </div>
                )}
              </div>
            )}

          </div>

          {/* RIGHT COLUMN: Doctor Orders, 108 Dispatch & Event History */}
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            
            {/* Feedback Banners */}
            {successMessage && (
              <div className="success-banner" style={{ margin: 0, padding: "10px 14px", fontSize: "0.8rem" }}>
                ✅ {successMessage}
              </div>
            )}
            {errorMessage && (
              <div className="error-banner" style={{ margin: 0, padding: "10px 14px", fontSize: "0.8rem" }}>
                ⚠️ {errorMessage}
              </div>
            )}

            {/* Medical Officer Advisory Order Console */}
            <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                <div className="technical-label">
                  <Stethoscope size={13} color="var(--navy-deep)" />
                  <span>MEDICAL OFFICER ADVISORY & ORDER</span>
                </div>
                {currentCase.doctor_advisory && (
                  <span className="badge badge-green">
                    ISSUED
                  </span>
                )}
              </div>

              {/* Recorded Advisory Display */}
              {currentCase.doctor_advisory && (
                <div style={{
                  background: "var(--seafoam-bg)",
                  border: "1px solid rgba(31, 95, 88, 0.3)",
                  borderRadius: "var(--radius-sm)",
                  padding: "12px",
                  marginBottom: "14px"
                }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--seafoam-dark)", fontWeight: 700, marginBottom: "4px" }}>
                    Active Doctor Clinical Guidance:
                  </div>
                  <div style={{ fontSize: "0.85rem", color: "var(--navy-deep)", fontWeight: 500 }}>
                    "{currentCase.doctor_advisory}"
                  </div>
                </div>
              )}

              {/* Order Form (Available to MO & Admin) */}
              {canDoctorAct ? (
                <form onSubmit={handleSubmitAdvisory}>
                  <div style={{ marginBottom: "10px" }}>
                    <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                      RAPID CLINICAL ORDER TEMPLATES:
                    </label>
                    <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                      {quickTemplates.map((t, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => setAdvisoryText(t)}
                          style={{
                            textAlign: "left",
                            background: "var(--panel-white)",
                            border: "1px solid var(--rule-muted)",
                            borderRadius: "var(--radius-sm)",
                            padding: "6px 10px",
                            fontSize: "0.72rem",
                            color: "var(--text-body)",
                            cursor: "pointer",
                            transition: "all 0.15s ease"
                          }}
                        >
                          • {t}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div style={{ marginBottom: "10px" }}>
                    <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                      DIRECT CLINICAL ORDER TO ASHA:
                    </label>
                    <textarea
                      rows={3}
                      value={advisoryText}
                      onChange={(e) => setAdvisoryText(e.target.value)}
                      placeholder="Enter emergency stabilization instructions, medication dosages, and transfer protocol..."
                      style={{ width: "100%", fontSize: "0.825rem" }}
                      required
                    />
                  </div>

                  <div style={{ marginBottom: "12px" }}>
                    <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                      DESTINATION REFERRAL FACILITY:
                    </label>
                    <select
                      value={referralFacility}
                      onChange={(e) => setReferralFacility(e.target.value)}
                      style={{ width: "100%", fontSize: "0.825rem" }}
                    >
                      <option value="FAC-01">Rampur Primary Health Centre (PHC) — BEmOC Basic Care</option>
                      <option value="FAC-02">Chandanpur Community Health Centre (CHC) — Blood Bank & Surgery</option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingAdvisory || !advisoryText.trim()}
                    className="btn-primary"
                    style={{ width: "100%", justifyContent: "center" }}
                  >
                    <Send size={13} />
                    <span>{isSubmittingAdvisory ? "Submitting Order..." : "Acknowledge & Send Guidance"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", fontStyle: "italic", background: "var(--panel-pale)", padding: "10px", borderRadius: "var(--radius-sm)" }}>
                  Clinical guidance orders require Medical Officer credentials. Current role: <strong>{currentUser.role}</strong>.
                </div>
              )}
            </div>

            {/* 108 Emergency Transport Coordination Console */}
            <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                <div className="technical-label">
                  <Truck size={13} color="var(--navy-deep)" />
                  <span>108 EMERGENCY TRANSPORT COORDINATION</span>
                </div>
                {currentCase.ambulance_status && (
                  <span className="badge badge-blue">
                    DISPATCHED
                  </span>
                )}
              </div>

              {/* Dispatched Status Banner */}
              {currentCase.ambulance_status && (
                <div style={{
                  background: "var(--blue-subtle)",
                  border: "1px solid rgba(36, 73, 154, 0.25)",
                  borderRadius: "var(--radius-sm)",
                  padding: "10px 12px",
                  marginBottom: "12px"
                }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--navy-deep)", fontWeight: 700, marginBottom: "2px" }}>
                    Active 108 Transport Status:
                  </div>
                  <div style={{ fontSize: "0.825rem", color: "var(--navy-deep)", fontWeight: 600 }}>
                    {currentCase.ambulance_status}
                  </div>
                </div>
              )}

              {/* Transport Dispatch Form */}
              {canDispatchAct ? (
                <form onSubmit={handleSubmitTransport}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "10px" }}>
                    <div>
                      <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                        AMBULANCE ID:
                      </label>
                      <input
                        type="text"
                        value={vehicleId}
                        onChange={(e) => setVehicleId(e.target.value)}
                        placeholder="108-AMB-Rampur-04"
                        style={{ width: "100%", fontSize: "0.8rem" }}
                        required
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                        DRIVER CONTACT:
                      </label>
                      <input
                        type="text"
                        value={driverPhone}
                        onChange={(e) => setDriverPhone(e.target.value)}
                        placeholder="9876543299"
                        style={{ width: "100%", fontSize: "0.8rem" }}
                      />
                    </div>
                  </div>

                  <div style={{ marginBottom: "12px" }}>
                    <label style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block", marginBottom: "4px", fontFamily: "var(--font-mono)" }}>
                      DESTINATION FACILITY:
                    </label>
                    <input
                      type="text"
                      value={destinationFacility}
                      onChange={(e) => setDestinationFacility(e.target.value)}
                      placeholder="CHC Rampur"
                      style={{ width: "100%", fontSize: "0.8rem" }}
                      required
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingTransport || !vehicleId.trim()}
                    className="btn-primary"
                    style={{ width: "100%", justifyContent: "center" }}
                  >
                    <Truck size={13} />
                    <span>{isSubmittingTransport ? "Dispatching..." : "Update 108 Transport Status"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", fontStyle: "italic", background: "var(--panel-pale)", padding: "10px", borderRadius: "var(--radius-sm)" }}>
                  Ambulance dispatch requires Dispatcher or Medical Officer role.
                </div>
              )}
            </div>

            {/* Case Event Timeline */}
            <div style={{ background: "var(--panel-card)", border: "1px solid var(--rule-muted)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div className="technical-label" style={{ marginBottom: "10px" }}>
                <History size={13} color="var(--navy-deep)" />
                <span>CASE AUDIT TIMELINE</span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "8px", maxHeight: "180px", overflowY: "auto" }}>
                {(currentCase.timeline || []).length === 0 ? (
                  <span style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>No event history recorded yet.</span>
                ) : (
                  (currentCase.timeline || []).map((evt, i) => (
                    <div key={i} style={{ fontSize: "0.75rem", borderLeft: "2px solid var(--rule-muted)", paddingLeft: "8px" }}>
                      <div style={{ color: "var(--navy-deep)", fontWeight: 600 }}>{evt.summary}</div>
                      <div style={{ color: "var(--text-muted)", fontSize: "0.68rem" }}>
                        by {evt.actor_role} ({evt.actor_id}) • {new Date(evt.occurred_at * 1000).toLocaleTimeString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

          </div>

        </div>

      </div>

      {/* FHIR R4 Bundle Export Preview Modal */}
      {fhirModalOpen && (
        <div className="modal-overlay" style={{ zIndex: 1100 }}>
          <div className="modal-card" style={{ maxWidth: "720px" }}>
            <div className="modal-header">
              <div className="technical-label">
                <FileCode2 size={14} color="var(--navy-deep)" />
                <span>HL7 FHIR R4 CLINICAL BUNDLE · {caseId}</span>
              </div>
              <button className="btn-close" onClick={() => setFhirModalOpen(false)}>✕</button>
            </div>

            <div style={{ padding: "18px 24px" }}>
              {fhirLoading ? (
                <div style={{ textAlign: "center", padding: "32px", color: "var(--text-muted)" }}>Generating validated FHIR R4 bundle...</div>
              ) : (
                <pre style={{
                  background: "var(--panel-pale)",
                  border: "1px solid var(--rule-muted)",
                  borderRadius: "var(--radius-sm)",
                  padding: "14px",
                  fontSize: "0.75rem",
                  fontFamily: "var(--font-mono)",
                  color: "var(--navy-deep)",
                  maxHeight: "380px",
                  overflowY: "auto",
                  margin: 0
                }}>
                  {JSON.stringify(fhirData, null, 2)}
                </pre>
              )}

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setFhirModalOpen(false)}
                >
                  Close
                </button>
                <button
                  type="button"
                  className="btn-submit"
                  onClick={handleCopyFhir}
                >
                  {copiedFhir ? <Check size={14} /> : <Download size={14} />}
                  <span>{copiedFhir ? "Copied!" : "Copy FHIR JSON"}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
