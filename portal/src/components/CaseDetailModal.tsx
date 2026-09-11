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
  Check,
  AlertOctagon,
  AlertTriangle,
  CheckCircle,
  Activity
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

  // Quick clinical order templates
  const quickTemplates = [
    "Administer oral labetalol 100mg stat; maintain left lateral tilt; immediate 108 transfer to CHC.",
    "Administer paracetamol 500mg; perform rapid malaria & urine albumin test at Sub-centre.",
    "Keep patient flat; do not give oral fluids; prepare oxygen; emergency blood transfusion alert.",
    "Immediate IM dexamethasone 6mg for fetal lung maturation; arrange obstetrician consultation."
  ];

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

  const handleSubmitAdvisory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!advisoryText.trim()) return;

    setIsSubmittingAdvisory(true);
    setErrorMessage(null);
    try {
      const updated = await acknowledgeCase(caseId, advisoryText, referralFacility);
      setCurrentCase(updated);
      onCaseUpdated(updated);
      setSuccessMessage("Medical Officer advisory successfully sent to frontline ASHA worker!");
      setAdvisoryText("");
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : "Failed to record doctor advisory");
    } finally {
      setIsSubmittingAdvisory(false);
    }
  };

  const handleSubmitTransport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!vehicleId.trim() || !destinationFacility.trim()) return;

    setIsSubmittingTransport(true);
    setErrorMessage(null);
    try {
      const updated = await updateTransport(caseId, vehicleId, destinationFacility, driverPhone);
      setCurrentCase(updated);
      onCaseUpdated(updated);
      setSuccessMessage(`108 Ambulance ${vehicleId} dispatched to ${destinationFacility}!`);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : "Failed to update transport status");
    } finally {
      setIsSubmittingTransport(false);
    }
  };

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
      <div className="modal-dialog" style={{ maxWidth: "1080px" }}>
        
        {/* Modal Header */}
        <div className="modal-header">
          <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
            <span className={`badge ${risk === "RED" ? "badge-red" : (risk === "AMBER" ? "badge-amber" : "badge-green")}`}>
              {risk === "RED" ? <AlertOctagon size={13} /> : (risk === "AMBER" ? <AlertTriangle size={13} /> : <CheckCircle size={13} />)}
              {risk} RISK
            </span>
            <h2 className="modal-title" style={{ margin: 0, fontFamily: "var(--font-serif)", fontSize: "1.65rem" }}>
              {currentCase.patient_name}
            </h2>
            <span className="case-id-code">{caseId}</span>
            <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
              {currentCase.village} &bull; Age: {currentCase.age_years || "—"}y &bull; Gestation: {currentCase.gestational_age_weeks || "—"}w
            </span>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <button
              onClick={handleOpenFhir}
              className="btn btn-secondary btn-sm"
              title="Export FHIR R4 Clinical Bundle"
            >
              <FileCode2 size={14} color="var(--medical-teal)" />
              <span>FHIR R4</span>
            </button>

            <button onClick={onClose} className="btn-close" title="Close modal">
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="modal-body" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "24px" }}>
          
          {/* Left Column: Clinical Assessment */}
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            
            {/* Vitals Snapshot Card */}
            <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "18px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "12px" }}>
                <Activity size={16} color="var(--medical-teal)" />
                <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>Clinical Vitals</h4>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "14px" }}>
                <div style={{ background: "#FFFFFF", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "12px 14px" }}>
                  <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 600, textTransform: "uppercase" }}>Blood Pressure</div>
                  <div style={{ fontSize: "1.3rem", fontWeight: 700, color: "var(--text-primary)", fontFamily: "var(--font-mono)", marginTop: "2px" }}>
                    {assessment?.blood_pressure || "Unmeasured"}
                  </div>
                </div>

                <div style={{ background: "#FFFFFF", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-md)", padding: "12px 14px" }}>
                  <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 600, textTransform: "uppercase" }}>Haemoglobin</div>
                  <div style={{ fontSize: "1.3rem", fontWeight: 700, color: "var(--text-primary)", fontFamily: "var(--font-mono)", marginTop: "2px" }}>
                    {assessment?.haemoglobin ? `${assessment.haemoglobin} g/dL` : "Unmeasured"}
                  </div>
                </div>
              </div>

              {/* Observed Danger Signs */}
              <div>
                <div style={{ fontSize: "0.75rem", fontWeight: 600, color: "var(--text-muted)", marginBottom: "6px" }}>
                  Observed Danger Signs:
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                  {Object.entries(dangerSigns).map(([signKey, isPresent]) => {
                    const label = signKey.replace(/_/g, " ");
                    return (
                      <span
                        key={signKey}
                        className={`badge ${isPresent ? "badge-red" : "badge-gray"}`}
                      >
                        {isPresent ? "⚠" : "✓"} {label}
                      </span>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Protocol & Rationale */}
            <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "18px", boxShadow: "var(--shadow-subtle)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
                <ShieldCheck size={16} color="var(--medical-teal)" />
                <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>Protocol & Clinical Guidance</h4>
              </div>

              <div style={{ fontSize: "0.875rem", color: "var(--text-body)", marginBottom: "14px", lineHeight: 1.6 }}>
                {assessment?.clinical_rationale || "Clinical assessment evaluated under MoHFW guidelines."}
              </div>

              {/* Action boundaries */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                <div style={{ background: "var(--green-bg)", border: "1px solid var(--green-border)", borderRadius: "var(--radius-sm)", padding: "12px" }}>
                  <div style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--green-text)", marginBottom: "4px" }}>
                    ✓ ASHA Safe Actions:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: "16px", fontSize: "0.75rem", color: "var(--text-body)", lineHeight: 1.6 }}>
                    {(assessment?.asha_safe_actions || ["Dial 108 ambulance", "Position in left lateral tilt", "Accompany to facility"]).map((act, i) => (
                      <li key={i}>{act}</li>
                    ))}
                  </ul>
                </div>

                <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-sm)", padding: "12px" }}>
                  <div style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--red-text)", marginBottom: "4px" }}>
                    ⚠ Clinician Orders Only:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: "16px", fontSize: "0.75rem", color: "var(--text-body)", lineHeight: 1.6 }}>
                    {(assessment?.clinician_directed_actions || ["IV cannulation & fluids", "Magnesium Sulphate loading", "Antihypertensive administration"]).map((act, i) => (
                      <li key={i}>{act}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>

            {/* Voice Note Audio Recording */}
            {currentCase.audio_artifact && (
              <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "18px", boxShadow: "var(--shadow-subtle)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
                  <Mic size={16} color="var(--medical-teal)" />
                  <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>Frontline Voice Note</h4>
                </div>

                {currentCase.audio_artifact.transcript && (
                  <div style={{ background: "var(--bg-card-warm)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", padding: "12px", fontSize: "0.825rem", color: "var(--text-body)", marginBottom: "10px", fontStyle: "italic" }}>
                    "{currentCase.audio_artifact.transcript}"
                  </div>
                )}

                <audio
                  controls
                  src={getAudioStreamUrl(caseId)}
                  onError={() => setAudioError(true)}
                  style={{ width: "100%", height: "36px" }}
                />
                {audioError && (
                  <div style={{ fontSize: "0.75rem", color: "var(--red-text)", marginTop: "4px" }}>
                    Audio preview offline; encrypted voice artifact is stored securely on device.
                  </div>
                )}
              </div>
            )}

          </div>

          {/* Right Column: Doctor Orders, 108 Dispatch & Event History */}
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            
            {successMessage && (
              <div className="alert alert-success" style={{ margin: 0 }}>
                {successMessage}
              </div>
            )}
            {errorMessage && (
              <div className="alert alert-danger" style={{ margin: 0 }}>
                {errorMessage}
              </div>
            )}

            {/* Doctor Clinical Guidance Console */}
            <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "18px", boxShadow: "var(--shadow-subtle)" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <Stethoscope size={16} color="var(--medical-teal)" />
                  <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>Doctor Advisory Order</h4>
                </div>
                {currentCase.doctor_advisory && (
                  <span className="badge badge-green">Advisory Active</span>
                )}
              </div>

              {currentCase.doctor_advisory && (
                <div style={{ background: "var(--green-bg)", border: "1px solid var(--green-border)", borderRadius: "var(--radius-sm)", padding: "10px 14px", marginBottom: "14px", fontSize: "0.85rem", color: "var(--green-text)" }}>
                  <strong>Issued Guidance:</strong> "{currentCase.doctor_advisory}"
                </div>
              )}

              {canDoctorAct ? (
                <form onSubmit={handleSubmitAdvisory}>
                  <div className="form-group">
                    <label className="form-label" style={{ fontSize: "0.75rem" }}>Rapid Clinical Presets:</label>
                    <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                      {quickTemplates.map((t, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => setAdvisoryText(t)}
                          style={{
                            textAlign: "left",
                            background: "var(--bg-card-warm)",
                            border: "1px solid var(--border-subtle)",
                            borderRadius: "var(--radius-sm)",
                            padding: "6px 10px",
                            fontSize: "0.75rem",
                            color: "var(--text-body)",
                            cursor: "pointer"
                          }}
                        >
                          &bull; {t}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Clinical Guidance Instructions:</label>
                    <textarea
                      rows={3}
                      className="form-control"
                      value={advisoryText}
                      onChange={(e) => setAdvisoryText(e.target.value)}
                      placeholder="Enter clinical guidance, medication orders, and stabilization instructions..."
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Destination Referral Facility:</label>
                    <select
                      className="form-control"
                      value={referralFacility}
                      onChange={(e) => setReferralFacility(e.target.value)}
                    >
                      <option value="FAC-01">Rampur Primary Health Centre (PHC) — BEmOC Basic Care</option>
                      <option value="FAC-02">Chandanpur Community Health Centre (CHC) — Blood Bank & Surgery</option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingAdvisory || !advisoryText.trim()}
                    className="btn btn-primary"
                    style={{ width: "100%" }}
                  >
                    <Send size={14} />
                    <span>{isSubmittingAdvisory ? "Submitting..." : "Send Clinical Guidance"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.8125rem", color: "var(--text-muted)", background: "var(--bg-card-warm)", padding: "10px", borderRadius: "var(--radius-sm)" }}>
                  Clinical guidance orders require Medical Officer credentials. Current role: <strong>{currentUser.role}</strong>.
                </div>
              )}
            </div>

            {/* 108 Emergency Transport Console */}
            <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "18px", boxShadow: "var(--shadow-subtle)" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <Truck size={16} color="var(--medical-teal)" />
                  <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>108 Transport Dispatch</h4>
                </div>
                {currentCase.ambulance_status && (
                  <span className="badge badge-teal">Dispatched</span>
                )}
              </div>

              {canDispatchAct ? (
                <form onSubmit={handleSubmitTransport}>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">Ambulance Vehicle ID:</label>
                      <input
                        type="text"
                        className="form-control"
                        value={vehicleId}
                        onChange={(e) => setVehicleId(e.target.value)}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Driver Contact:</label>
                      <input
                        type="text"
                        className="form-control"
                        value={driverPhone}
                        onChange={(e) => setDriverPhone(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Destination Hospital:</label>
                    <input
                      type="text"
                      className="form-control"
                      value={destinationFacility}
                      onChange={(e) => setDestinationFacility(e.target.value)}
                      required
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingTransport || !vehicleId.trim()}
                    className="btn btn-primary"
                    style={{ width: "100%" }}
                  >
                    <Truck size={14} />
                    <span>{isSubmittingTransport ? "Dispatching..." : "Dispatch 108 Ambulance"}</span>
                  </button>
                </form>
              ) : (
                <div style={{ fontSize: "0.8125rem", color: "var(--text-muted)", background: "var(--bg-card-warm)", padding: "10px", borderRadius: "var(--radius-sm)" }}>
                  Ambulance dispatch requires Dispatcher or Medical Officer role.
                </div>
              )}
            </div>

            {/* Case Event Audit History */}
            <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "18px", boxShadow: "var(--shadow-subtle)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "10px" }}>
                <History size={16} color="var(--medical-teal)" />
                <h4 style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-primary)" }}>Case Audit History</h4>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "8px", maxHeight: "160px", overflowY: "auto" }}>
                {(currentCase.timeline || []).length === 0 ? (
                  <span style={{ fontSize: "0.8125rem", color: "var(--text-muted)" }}>No events recorded yet.</span>
                ) : (
                  (currentCase.timeline || []).map((evt, i) => (
                    <div key={i} style={{ fontSize: "0.8125rem", borderLeft: "2px solid var(--border-subtle)", paddingLeft: "8px" }}>
                      <div style={{ color: "var(--text-primary)", fontWeight: 600 }}>{evt.summary}</div>
                      <div style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}>
                        {evt.actor_role} ({evt.actor_id}) &bull; {new Date(evt.occurred_at * 1000).toLocaleTimeString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

          </div>

        </div>

        {/* Modal Footer */}
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            Close Case File
          </button>
        </div>

      </div>

      {/* FHIR R4 Bundle Modal */}
      {fhirModalOpen && (
        <div className="modal-overlay" style={{ zIndex: 1100 }}>
          <div className="modal-dialog" style={{ maxWidth: "720px" }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ fontFamily: "var(--font-serif)" }}>
                HL7 FHIR R4 Clinical Bundle ({caseId})
              </h3>
              <button className="btn-close" onClick={() => setFhirModalOpen(false)}>✕</button>
            </div>
            <div className="modal-body">
              {fhirLoading ? (
                <div className="empty-state">Generating FHIR R4 bundle...</div>
              ) : (
                <pre style={{
                  background: "var(--bg-card-warm)",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                  padding: "14px",
                  fontSize: "0.8rem",
                  fontFamily: "var(--font-mono)",
                  maxHeight: "380px",
                  overflowY: "auto"
                }}>
                  {JSON.stringify(fhirData, null, 2)}
                </pre>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setFhirModalOpen(false)}>
                Close
              </button>
              <button className="btn btn-primary" onClick={handleCopyFhir}>
                {copiedFhir ? <Check size={14} /> : <Download size={14} />}
                <span>{copiedFhir ? "Copied!" : "Copy FHIR JSON"}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
