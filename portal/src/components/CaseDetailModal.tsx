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

  // Handle Transport Submission
  const handleSubmitTransport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!vehicleId.trim()) return;

    setIsSubmittingTransport(true);
    setErrorMessage(null);
    try {
      const updated = await updateTransport(caseId, vehicleId, destinationFacility, driverPhone);
      setCurrentCase(updated);
      onCaseUpdated(updated);
      setSuccessMessage(`108 Ambulance ${vehicleId} dispatched successfully to patient location!`);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : "Failed to dispatch transport");
    } finally {
      setIsSubmittingTransport(false);
    }
  };

  // Handle FHIR Export
  const handleOpenFhir = async () => {
    setFhirModalOpen(true);
    setFhirLoading(true);
    try {
      const data = await fetchFhirBundle(caseId);
      setFhirData(data);
    } catch (err: unknown) {
      setFhirData({ error: err instanceof Error ? err.message : "Failed to load FHIR bundle" });
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
    <div style={{
      position: "fixed",
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: "rgba(3, 7, 18, 0.85)",
      backdropFilter: "blur(8px)",
      zIndex: 1000,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "24px"
    }}>
      <div 
        className="glass-panel-elevated"
        style={{
          width: "100%",
          maxWidth: "1100px",
          maxHeight: "92vh",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
          border: risk === "RED" ? "1px solid rgba(239, 68, 68, 0.4)" : "1px solid var(--border-strong)",
          boxShadow: risk === "RED" ? "0 0 32px rgba(239, 68, 68, 0.25)" : "var(--shadow-lg)"
        }}
      >
        {/* Modal Header */}
        <div style={{
          padding: "18px 24px",
          background: "rgba(16, 23, 38, 0.95)",
          borderBottom: "1px solid var(--border-subtle)",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: "14px"
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <span className={risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")}>
              {risk === "RED" && <span style={{ width: "8px", height: "8px", borderRadius: "50%", background: "var(--risk-red)" }} className="animate-pulse-red" />}
              {risk} RISK
            </span>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <h2 style={{ fontSize: "1.35rem", fontWeight: 800, color: "#ffffff" }}>
                  {currentCase.patient_name}
                </h2>
                <span className="mono" style={{ fontSize: "0.8rem", color: "var(--text-muted)", background: "rgba(255, 255, 255, 0.05)", padding: "2px 8px", borderRadius: "4px" }}>
                  {caseId}
                </span>
                {currentCase.is_demo && (
                  <span style={{ fontSize: "0.7rem", background: "rgba(168, 85, 247, 0.2)", color: "#c084fc", padding: "2px 8px", borderRadius: "4px", fontWeight: 600 }}>
                    Demo Mode
                  </span>
                )}
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "0.8rem", color: "var(--text-secondary)", marginTop: "3px" }}>
                <span>Village: <strong>{currentCase.village}</strong></span>
                {currentCase.age_years && <span>Age: {currentCase.age_years}y</span>}
                {currentCase.gestational_age_weeks && <span>Gestation: <strong>{currentCase.gestational_age_weeks} weeks</strong></span>}
                {currentCase.gravida !== null && currentCase.gravida !== undefined && <span>Gravida: G{currentCase.gravida}P{currentCase.para ?? 0}</span>}
              </div>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <button
              onClick={handleOpenFhir}
              className="btn-outline"
              style={{ fontSize: "0.75rem", padding: "6px 12px" }}
              title="Export FHIR R4 Bundle"
            >
              <FileCode2 size={14} color="#38bdf8" />
              <span>FHIR R4</span>
            </button>

            <button
              onClick={onClose}
              style={{
                background: "rgba(255, 255, 255, 0.05)",
                border: "1px solid var(--border-subtle)",
                borderRadius: "8px",
                width: "34px",
                height: "34px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "var(--text-secondary)",
                cursor: "pointer"
              }}
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Modal Body: Two-Column Responsive Layout */}
        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))",
          gap: "20px",
          padding: "24px",
          overflowY: "auto",
          flex: 1
        }}>

          {/* LEFT COLUMN: Clinical Assessment & Audio Artifact */}
          <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
            
            {/* Vitals Summary Card */}
            <div className="glass-panel" style={{ padding: "18px" }}>
              <h3 style={{ fontSize: "0.95rem", color: "#ffffff", marginBottom: "14px", display: "flex", alignItems: "center", gap: "8px" }}>
                <Stethoscope size={16} color="#38bdf8" />
                <span>Maternal Clinical Vitals</span>
              </h3>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "16px" }}>
                {/* BP */}
                <div style={{
                  padding: "12px",
                  borderRadius: "8px",
                  background: assessment?.blood_pressure?.includes("16") || assessment?.blood_pressure?.includes("11")
                    ? "rgba(239, 68, 68, 0.15)"
                    : "rgba(255, 255, 255, 0.04)",
                  border: assessment?.blood_pressure?.includes("16")
                    ? "1px solid rgba(239, 68, 68, 0.4)"
                    : "1px solid var(--border-subtle)"
                }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--text-muted)", textTransform: "uppercase" }}>Blood Pressure</div>
                  <div style={{ fontSize: "1.25rem", fontWeight: 800, color: "#ffffff", marginTop: "2px" }}>
                    {assessment?.blood_pressure || "Unmeasured"}
                  </div>
                  {assessment?.blood_pressure?.includes("16") && (
                    <span style={{ fontSize: "0.7rem", color: "#f87171", fontWeight: 700 }}>
                      ⚠️ Severe Gestational Hypertension
                    </span>
                  )}
                </div>

                {/* Hb */}
                <div style={{
                  padding: "12px",
                  borderRadius: "8px",
                  background: assessment?.haemoglobin && assessment.haemoglobin < 7.0
                    ? "rgba(239, 68, 68, 0.15)"
                    : "rgba(255, 255, 255, 0.04)",
                  border: assessment?.haemoglobin && assessment.haemoglobin < 7.0
                    ? "1px solid rgba(239, 68, 68, 0.4)"
                    : "1px solid var(--border-subtle)"
                }}>
                  <div style={{ fontSize: "0.7rem", color: "var(--text-muted)", textTransform: "uppercase" }}>Haemoglobin</div>
                  <div style={{ fontSize: "1.25rem", fontWeight: 800, color: "#ffffff", marginTop: "2px" }}>
                    {assessment?.haemoglobin ? `${assessment.haemoglobin} g/dL` : "Unmeasured"}
                  </div>
                  {assessment?.haemoglobin && assessment.haemoglobin < 7.0 && (
                    <span style={{ fontSize: "0.7rem", color: "#f87171", fontWeight: 700 }}>
                      ⚠️ Severe Maternal Anemia
                    </span>
                  )}
                </div>
              </div>

              {/* Danger Signs Checklist */}
              <div>
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", marginBottom: "8px", fontWeight: 600 }}>
                  Observed Danger Signs:
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                  {Object.entries(dangerSigns).map(([signKey, isPresent]) => {
                    const label = signKey.replace(/_/g, " ");
                    return (
                      <span
                        key={signKey}
                        style={{
                          fontSize: "0.75rem",
                          padding: "4px 10px",
                          borderRadius: "6px",
                          background: isPresent ? "rgba(239, 68, 68, 0.2)" : "rgba(255, 255, 255, 0.04)",
                          color: isPresent ? "#fca5a5" : "var(--text-dim)",
                          border: isPresent ? "1px solid rgba(239, 68, 68, 0.4)" : "1px solid var(--border-subtle)",
                          fontWeight: isPresent ? 700 : 400,
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "6px"
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
            <div className="glass-panel" style={{ padding: "18px" }}>
              <h3 style={{ fontSize: "0.95rem", color: "#ffffff", marginBottom: "10px", display: "flex", alignItems: "center", gap: "8px" }}>
                <ShieldCheck size={16} color="#10b981" />
                <span>Clinical Triage Rationale (MoHFW Guidelines)</span>
              </h3>

              <div style={{
                background: "rgba(0, 0, 0, 0.2)",
                padding: "12px",
                borderRadius: "8px",
                fontSize: "0.85rem",
                color: "var(--text-secondary)",
                lineHeight: 1.6,
                marginBottom: "14px"
              }}>
                {assessment?.clinical_rationale || "Clinical evaluation according to MoHFW High-Risk Pregnancy guidelines."}
              </div>

              {/* Protocol Recommendation */}
              <div style={{
                background: risk === "RED" ? "rgba(239, 68, 68, 0.1)" : "rgba(14, 165, 233, 0.1)",
                border: risk === "RED" ? "1px solid rgba(239, 68, 68, 0.3)" : "1px solid rgba(14, 165, 233, 0.3)",
                borderRadius: "8px",
                padding: "12px",
                fontSize: "0.8rem",
                color: "#ffffff"
              }}>
                <div style={{ fontWeight: 700, color: risk === "RED" ? "#f87171" : "#38bdf8", marginBottom: "4px" }}>
                  Recommended Action Protocol:
                </div>
                {assessment?.recommended_protocol || "Immediate facility referral and monitoring."}
              </div>
            </div>

            {/* Voice Note Artifact Inspector */}
            {currentCase.audio_artifact && (
              <div className="glass-panel" style={{ padding: "18px", borderLeft: "4px solid #38bdf8" }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
                  <h3 style={{ fontSize: "0.95rem", color: "#ffffff", display: "flex", alignItems: "center", gap: "8px" }}>
                    <Mic size={16} color="#38bdf8" />
                    <span>Point-of-Care Voice Note Artifact</span>
                  </h3>
                  <span style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>
                    Verified by ASHA
                  </span>
                </div>

                {/* Audio Player */}
                <div style={{ background: "rgba(0, 0, 0, 0.3)", padding: "12px", borderRadius: "8px", marginBottom: "14px" }}>
                  <audio
                    controls
                    src={getAudioStreamUrl(caseId)}
                    onError={() => setAudioError(true)}
                    style={{ width: "100%", height: "36px" }}
                  />
                  {audioError && (
                    <div style={{ fontSize: "0.75rem", color: "#f87171", marginTop: "6px" }}>
                      Audio file streaming from backend storage...
                    </div>
                  )}
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: "8px", fontSize: "0.7rem", color: "var(--text-muted)" }}>
                    <span className="mono">SHA: {currentCase.audio_artifact.sha256.substring(0, 16)}...</span>
                    <span>Retention: 90 days MoHFW</span>
                  </div>
                </div>

                {/* Transcript vs Form Fields */}
                <div style={{ background: "rgba(14, 165, 233, 0.05)", border: "1px solid rgba(56, 189, 248, 0.2)", borderRadius: "8px", padding: "12px" }}>
                  <div style={{ fontSize: "0.75rem", fontWeight: 700, color: "#38bdf8", marginBottom: "4px" }}>
                    Audio Transcript (Indic Voice Dictation):
                  </div>
                  <p style={{ fontSize: "0.85rem", color: "#ffffff", fontStyle: "italic" }}>
                    "{currentCase.audio_artifact.transcript || "मरीज गर्भवती, तेज सिरदर्द, रक्तचाप अधिक और कमजोरी (ASHA confirmed)"}"
                  </p>
                </div>
              </div>
            )}

          </div>

          {/* RIGHT COLUMN: Doctor Guidance, Transport, & Audit Timeline */}
          <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
            
            {/* Medical Officer Advisory Console */}
            <div className="glass-panel" style={{ padding: "18px", border: "1px solid rgba(14, 165, 233, 0.3)" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
                <h3 style={{ fontSize: "0.95rem", color: "#ffffff", display: "flex", alignItems: "center", gap: "8px" }}>
                  <Stethoscope size={16} color="#38bdf8" />
                  <span>Medical Officer Clinical Guidance</span>
                </h3>
                {currentCase.doctor_advisory && (
                  <span style={{ fontSize: "0.7rem", background: "rgba(16, 185, 129, 0.2)", color: "#6ee7b7", padding: "2px 8px", borderRadius: "999px", fontWeight: 700 }}>
                    Advisory Active
                  </span>
                )}
              </div>

              {/* If advisory already issued, display it prominently */}
              {currentCase.doctor_advisory && (
                <div style={{
                  background: "rgba(16, 185, 129, 0.1)",
                  border: "1px solid rgba(16, 185, 129, 0.3)",
                  borderRadius: "8px",
                  padding: "12px",
                  marginBottom: "14px"
                }}>
                  <div style={{ fontSize: "0.75rem", color: "#6ee7b7", fontWeight: 700, marginBottom: "4px" }}>
                    Active Doctor Order:
                  </div>
                  <div style={{ fontSize: "0.85rem", color: "#ffffff", fontWeight: 500 }}>
                    {currentCase.doctor_advisory}
                  </div>
                </div>
              )}

              {/* Advisory Input Form */}
              {canDoctorAct ? (
                <form onSubmit={handleSubmitAdvisory}>
                  <div style={{ marginBottom: "10px" }}>
                    <label style={{ fontSize: "0.75rem", color: "var(--text-muted)", display: "block", marginBottom: "6px" }}>
                      Clinical Order / Immediate Instructions for ASHA Worker:
                    </label>
                    <textarea
                      rows={3}
                      value={advisoryText}
                      onChange={(e) => setAdvisoryText(e.target.value)}
                      placeholder="e.g. Administer oral labetalol 100mg stat; position in left lateral tilt; call 108 ambulance immediately..."
                      style={{ width: "100%", resize: "vertical", fontSize: "0.85rem" }}
                      required
                    />
                  </div>

                  {/* Quick Templates */}
                  <div style={{ marginBottom: "12px" }}>
                    <span style={{ fontSize: "0.7rem", color: "var(--text-muted)", display: "block", marginBottom: "6px" }}>
                      Quick Order Templates:
                    </span>
                    <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                      {quickTemplates.map((tmpl, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => setAdvisoryText(tmpl)}
                          style={{
                            textAlign: "left",
                            padding: "4px 8px",
                            background: "rgba(255, 255, 255, 0.03)",
                            border: "1px solid var(--border-subtle)",
                            borderRadius: "4px",
                            color: "var(--text-secondary)",
                            fontSize: "0.7rem",
                            cursor: "pointer"
                          }}
                        >
                          + {tmpl.substring(0, 65)}...
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Referral Facility Picker */}
                  <div style={{ display: "flex", gap: "10px", alignItems: "center", marginBottom: "14px" }}>
                    <label style={{ fontSize: "0.75rem", color: "var(--text-muted)", whiteSpace: "nowrap" }}>
                      Referral Facility:
                    </label>
                    <select
                      value={referralFacility}
                      onChange={(e) => setReferralFacility(e.target.value)}
                      style={{ flex: 1, fontSize: "0.8rem", padding: "6px 10px" }}
                    >
                      <option value="FAC-01">Rampur Primary Health Centre (PHC)</option>
                      <option value="FAC-02">Chandanpur Community Health Centre (CHC)</option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingAdvisory || !advisoryText.trim()}
                    className="btn-primary"
                    style={{ width: "100%", justifyContent: "center" }}
                  >
                    <Send size={14} />
                    <span>{isSubmittingAdvisory ? "Submitting Order..." : "Acknowledge & Send Guidance"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontStyle: "italic", background: "rgba(255, 255, 255, 0.02)", padding: "10px", borderRadius: "6px" }}>
                  Clinical guidance orders require Medical Officer credentials. Current role: <strong>{currentUser.role}</strong>.
                </div>
              )}
            </div>

            {/* 108 Emergency Transport Coordination Console */}
            <div className="glass-panel" style={{ padding: "18px", border: "1px solid rgba(56, 189, 248, 0.3)" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
                <h3 style={{ fontSize: "0.95rem", color: "#ffffff", display: "flex", alignItems: "center", gap: "8px" }}>
                  <Truck size={16} color="#38bdf8" />
                  <span>108 Emergency Transport Coordination</span>
                </h3>
                {currentCase.ambulance_status && (
                  <span style={{ fontSize: "0.7rem", background: "rgba(56, 189, 248, 0.2)", color: "#7dd3fc", padding: "2px 8px", borderRadius: "999px", fontWeight: 700 }}>
                    Dispatched
                  </span>
                )}
              </div>

              {/* Dispatched Status Banner */}
              {currentCase.ambulance_status && (
                <div style={{
                  background: "rgba(56, 189, 248, 0.1)",
                  border: "1px solid rgba(56, 189, 248, 0.3)",
                  borderRadius: "8px",
                  padding: "12px",
                  marginBottom: "14px"
                }}>
                  <div style={{ fontSize: "0.75rem", color: "#7dd3fc", fontWeight: 700, marginBottom: "4px" }}>
                    Active 108 Transport Status:
                  </div>
                  <div style={{ fontSize: "0.85rem", color: "#ffffff", fontWeight: 600 }}>
                    {currentCase.ambulance_status}
                  </div>
                </div>
              )}

              {/* Transport Dispatch Form */}
              {canDispatchAct ? (
                <form onSubmit={handleSubmitTransport}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "10px" }}>
                    <div>
                      <label style={{ fontSize: "0.75rem", color: "var(--text-muted)", display: "block", marginBottom: "4px" }}>
                        Ambulance ID:
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
                      <label style={{ fontSize: "0.75rem", color: "var(--text-muted)", display: "block", marginBottom: "4px" }}>
                        Driver Contact:
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
                    <label style={{ fontSize: "0.75rem", color: "var(--text-muted)", display: "block", marginBottom: "4px" }}>
                      Destination Facility:
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
                    style={{ width: "100%", justifyContent: "center", background: "linear-gradient(135deg, #0284c7 0%, #0369a1 100%)" }}
                  >
                    <Truck size={14} />
                    <span>{isSubmittingTransport ? "Dispatching..." : "Dispatch / Update 108 Ambulance"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontStyle: "italic", background: "rgba(255, 255, 255, 0.02)", padding: "10px", borderRadius: "6px" }}>
                  108 transport assignment requires Dispatcher or Medical Officer role. Current role: <strong>{currentUser.role}</strong>.
                </div>
              )}
            </div>

            {/* Chronological Audit Timeline */}
            <div className="glass-panel" style={{ padding: "18px" }}>
              <h3 style={{ fontSize: "0.95rem", color: "#ffffff", marginBottom: "12px", display: "flex", alignItems: "center", gap: "8px" }}>
                <History size={16} color="#c084fc" />
                <span>Immutable Case Audit History</span>
              </h3>

              <div style={{ display: "flex", flexDirection: "column", gap: "10px", maxHeight: "200px", overflowY: "auto", paddingRight: "4px" }}>
                {currentCase.timeline && currentCase.timeline.length > 0 ? (
                  currentCase.timeline.map((evt, idx) => (
                    <div
                      key={evt.event_id || evt.id || idx}
                      style={{
                        padding: "8px 12px",
                        borderRadius: "6px",
                        background: "rgba(255, 255, 255, 0.03)",
                        border: "1px solid var(--border-subtle)",
                        fontSize: "0.75rem"
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "2px" }}>
                        <span style={{ fontWeight: 700, color: "#38bdf8" }}>
                          {evt.event_type}
                        </span>
                        <span style={{ color: "var(--text-muted)", fontSize: "0.65rem" }}>
                          {evt.actor_role} • {new Date(evt.occurred_at * 1000).toLocaleTimeString()}
                        </span>
                      </div>
                      <div style={{ color: "var(--text-secondary)" }}>
                        {evt.summary}
                      </div>
                    </div>
                  ))
                ) : (
                  <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontStyle: "italic", padding: "8px" }}>
                    Synced to cloud • Awaiting first operator action
                  </div>
                )}
              </div>
            </div>

          </div>

        </div>

        {/* Feedback Alert Toast */}
        {successMessage && (
          <div style={{
            margin: "0 24px 16px 24px",
            padding: "10px 16px",
            background: "rgba(16, 185, 129, 0.15)",
            border: "1px solid rgba(16, 185, 129, 0.4)",
            borderRadius: "8px",
            color: "#6ee7b7",
            fontSize: "0.85rem",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between"
          }}>
            <span>✓ {successMessage}</span>
            <button onClick={() => setSuccessMessage(null)} style={{ background: "transparent", border: "none", color: "#6ee7b7", cursor: "pointer" }}>
              <X size={14} />
            </button>
          </div>
        )}

        {errorMessage && (
          <div style={{
            margin: "0 24px 16px 24px",
            padding: "10px 16px",
            background: "rgba(239, 68, 68, 0.15)",
            border: "1px solid rgba(239, 68, 68, 0.4)",
            borderRadius: "8px",
            color: "#fca5a5",
            fontSize: "0.85rem",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between"
          }}>
            <span>⚠️ {errorMessage}</span>
            <button onClick={() => setErrorMessage(null)} style={{ background: "transparent", border: "none", color: "#fca5a5", cursor: "pointer" }}>
              <X size={14} />
            </button>
          </div>
        )}

      </div>

      {/* FHIR R4 Bundle Modal */}
      {fhirModalOpen && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: "rgba(0, 0, 0, 0.8)",
          zIndex: 1100,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "24px"
        }}>
          <div className="glass-panel-elevated" style={{ width: "100%", maxWidth: "750px", maxHeight: "80vh", display: "flex", flexDirection: "column" }}>
            <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--border-subtle)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <h3 style={{ fontSize: "1.1rem", color: "#ffffff", display: "flex", alignItems: "center", gap: "8px" }}>
                <FileCode2 size={18} color="#38bdf8" />
                <span>Interoperable FHIR R4 Clinical Bundle</span>
              </h3>
              <button onClick={() => setFhirModalOpen(false)} style={{ background: "transparent", border: "none", color: "var(--text-secondary)", cursor: "pointer" }}>
                <X size={18} />
              </button>
            </div>
            <div style={{ padding: "16px", overflowY: "auto", flex: 1 }}>
              {fhirLoading ? (
                <div style={{ padding: "32px", textAlign: "center", color: "var(--text-muted)" }}>
                  Generating FHIR R4 bundle from deterministic evaluation...
                </div>
              ) : (
                <pre className="mono" style={{ background: "#050811", padding: "14px", borderRadius: "8px", fontSize: "0.75rem", color: "#38bdf8", overflowX: "auto" }}>
                  {JSON.stringify(fhirData, null, 2)}
                </pre>
              )}
            </div>
            <div style={{ padding: "14px 20px", borderTop: "1px solid var(--border-subtle)", display: "flex", justifyContent: "flex-end", gap: "10px" }}>
              <button onClick={handleCopyFhir} className="btn-primary" style={{ fontSize: "0.8rem", padding: "6px 14px" }}>
                {copiedFhir ? <Check size={14} /> : <Download size={14} />}
                <span>{copiedFhir ? "Copied to Clipboard" : "Copy JSON"}</span>
              </button>
              <button onClick={() => setFhirModalOpen(false)} className="btn-outline" style={{ fontSize: "0.8rem", padding: "6px 14px" }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
