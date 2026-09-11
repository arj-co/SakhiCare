import React, { useState, useEffect, useCallback, useMemo } from "react";
import type { PregnancyCase, UserProfile } from "./api";
import { fetchCases, login } from "./api";
import { Navbar } from "./components/Navbar";
import { UrgentQueue } from "./components/UrgentQueue";
import { CaseDetailModal } from "./components/CaseDetailModal";
import { FacilityRoster } from "./components/FacilityRoster";
import { WorkerRoster } from "./components/WorkerRoster";
import { ClinicalProtocols } from "./components/ClinicalProtocols";
import { TransportBoard } from "./components/TransportBoard";
import { NotificationCenter } from "./components/NotificationCenter";
import { AlertCircle, AlertTriangle, CheckCircle, Users } from "lucide-react";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8000";

const DEFAULT_USERS: Record<UserProfile["role"], UserProfile> = {
  MEDICAL_OFFICER: {
    id: "usr_mo_01",
    username: "doctor_sharma",
    full_name: "Dr. Rajiv Sharma (Medical Officer)",
    role: "MEDICAL_OFFICER",
    facility_id: "FAC-01",
    email: "dr.sharma@sakhicare.gov.in"
  },
  SUPERVISOR: {
    id: "usr_sup_01",
    username: "supervisor_anita",
    full_name: "Anita Kumari (Block Supervisor)",
    role: "SUPERVISOR",
    facility_id: "FAC-01",
    email: "anita.sup@sakhicare.gov.in"
  },
  DISPATCHER: {
    id: "usr_disp_01",
    username: "dispatch_108",
    full_name: "Vikram Singh (108 Transport Coordinator)",
    role: "DISPATCHER",
    facility_id: null,
    email: "dispatch108@sakhicare.gov.in"
  },
  ADMIN: {
    id: "usr_admin_01",
    username: "admin_sakhicare",
    full_name: "System Administrator",
    role: "ADMIN",
    facility_id: null,
    email: "admin@sakhicare.gov.in"
  }
};

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<UserProfile>(DEFAULT_USERS.MEDICAL_OFFICER);
  const [currentTab, setCurrentTab] = useState<"queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications">("queue");
  const [cases, setCases] = useState<PregnancyCase[]>([]);
  const [selectedCase, setSelectedCase] = useState<PregnancyCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleRoleChange = async (role: UserProfile["role"]) => {
    const targetUser = DEFAULT_USERS[role];
    setCurrentUser(targetUser);

    try {
      const passwords: Record<string, string> = {
        doctor_sharma: "DoctorPass123!",
        supervisor_anita: "SuperPass123!",
        dispatch_108: "DispatchPass123!",
        admin_sakhicare: "AdminPass123!"
      };
      await login(targetUser.username, passwords[targetUser.username] || "Password123!");
    } catch (err) {
      console.warn("Backend auth offline or using cached session token", err);
    }
  };

  const loadCases = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await fetchCases();
      setCases(res.cases);
    } catch (err: unknown) {
      console.error("Failed to load cases from backend", err);
      setErrorMessage(err instanceof Error ? err.message : "Failed to load cases");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    handleRoleChange("MEDICAL_OFFICER");
    loadCases();
  }, [loadCases]);

  // Connect to SSE stream
  useEffect(() => {
    let eventSource: EventSource | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout>;

    const connectSSE = () => {
      try {
        eventSource = new EventSource(`${API_BASE}/api/v1/live-stream`);

        eventSource.onopen = () => {
          setIsLive(true);
        };

        eventSource.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === "NEW_CASE" && data.case) {
              const newCase = data.case;
              setCases((prev) => {
                const id = newCase.case_id || newCase.patient_id;
                const exists = prev.some((c) => (c.case_id || c.patient_id) === id);
                if (exists) return prev;
                return [newCase, ...prev];
              });
            } else if (data.type === "CASE_UPDATED" && data.case) {
              const updated = data.case;
              const id = updated.case_id || updated.patient_id;
              setCases((prev) =>
                prev.map((c) => ((c.case_id || c.patient_id) === id ? { ...c, ...updated } : c))
              );
              setSelectedCase((cur) =>
                cur && (cur.case_id || cur.patient_id) === id ? { ...cur, ...updated } : cur
              );
            }
          } catch (err) {
            // Heartbeat
          }
        };

        eventSource.onerror = () => {
          setIsLive(false);
          if (eventSource) {
            eventSource.close();
          }
          reconnectTimeout = setTimeout(connectSSE, 5000);
        };
      } catch (e) {
        setIsLive(false);
      }
    };

    connectSSE();

    return () => {
      if (eventSource) eventSource.close();
      clearTimeout(reconnectTimeout);
    };
  }, []);

  const stats = useMemo(() => {
    const red = cases.filter((c) => c.assessment?.risk_level === "RED").length;
    const amber = cases.filter((c) => c.assessment?.risk_level === "AMBER").length;
    const green = cases.filter((c) => c.assessment?.risk_level === "GREEN").length;
    return { red, amber, green, total: cases.length };
  }, [cases]);

  const handleCaseUpdated = (updatedCase: PregnancyCase) => {
    const id = updatedCase.case_id || updatedCase.patient_id;
    setCases((prev) =>
      prev.map((c) => ((c.case_id || c.patient_id) === id ? updatedCase : c))
    );
    setSelectedCase(updatedCase);
  };

  return (
    <div className="app-container">
      {/* Eyra-inspired TopBar */}
      <Navbar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        currentUser={currentUser}
        onChangeRole={handleRoleChange}
        criticalCount={stats.red}
        totalCases={stats.total}
        isLive={isLive}
        onRefresh={loadCases}
      />

      <main className="main-content">
        {errorMessage && (
          <div className="alert alert-warning">
            <AlertCircle size={18} />
            <div style={{ flex: 1 }}>
              <strong>Notice:</strong> {errorMessage} (Displaying locally synchronized records)
            </div>
            <button className="btn btn-secondary btn-sm" onClick={loadCases}>
              Retry Connection
            </button>
          </div>
        )}

        {/* Global Summary Stats (on queue tab) */}
        {currentTab === "queue" && (
          <div>
            <div className="eyra-section-label">Maternal Care Infrastructure</div>
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Total Patients</span>
                  <span className="stat-value">{stats.total}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-teal">
                  <Users size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Critical Emergency</span>
                  <span className="stat-value" style={{ color: "var(--red-primary)" }}>{stats.red}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-red">
                  <AlertCircle size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Moderate High-Risk</span>
                  <span className="stat-value" style={{ color: "var(--amber-primary)" }}>{stats.amber}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-amber">
                  <AlertTriangle size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Routine ANC Care</span>
                  <span className="stat-value" style={{ color: "var(--medical-teal)" }}>{stats.green}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-green">
                  <CheckCircle size={22} />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab Views */}
        {currentTab === "queue" && (
          <UrgentQueue
            cases={cases}
            loading={loading}
            onRefresh={loadCases}
            onSelectCase={(c) => setSelectedCase(c)}
          />
        )}

        {currentTab === "transport" && (
          <TransportBoard
            userRole={currentUser.role}
            onSelectCase={(caseId) => {
              const found = cases.find((c) => (c.case_id || c.patient_id) === caseId);
              if (found) setSelectedCase(found);
            }}
          />
        )}

        {currentTab === "notifications" && (
          <NotificationCenter
            userRole={currentUser.role}
            onSelectCase={(caseId) => {
              const found = cases.find((c) => (c.case_id || c.patient_id) === caseId);
              if (found) setSelectedCase(found);
            }}
          />
        )}

        {currentTab === "facilities" && <FacilityRoster />}

        {currentTab === "workers" && <WorkerRoster />}

        {currentTab === "protocols" && <ClinicalProtocols />}
      </main>

      {/* Case Detail Modal */}
      {selectedCase && (
        <CaseDetailModal
          caseItem={selectedCase}
          currentUser={currentUser}
          onClose={() => setSelectedCase(null)}
          onCaseUpdated={handleCaseUpdated}
        />
      )}

      {/* Eyra-style App Footer & Status Bar */}
      <footer className="app-footer">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px", maxWidth: "1280px", margin: "0 auto" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span style={{ fontFamily: "var(--font-serif)", fontSize: "1.1rem", fontWeight: 700 }}>SakhiCare</span>
            <span>&bull;</span>
            <span>Offline Maternal Danger-Sign Screening & 108 Emergency Transport</span>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span>Global Commitment:</span>
            <span className="badge-sdg3">SDG 3: Good Health</span>
            <span className="badge-sdg10">SDG 10: Reduced Inequalities</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
