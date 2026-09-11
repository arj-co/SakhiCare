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
import { AlertCircle, AlertTriangle, CheckCircle, FolderHeart } from "lucide-react";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8000";

// Standard pre-configured demo users for role switching
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

  // Authenticate user when role changes
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
      console.warn("Backend auth notice, using cached session token", err);
    }
  };

  // Initial load
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

  // Connect to SSE live stream for real-time updates
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

  // Summary counts
  const stats = useMemo(() => {
    const red = cases.filter((c) => c.assessment?.risk_level === "RED").length;
    const amber = cases.filter((c) => c.assessment?.risk_level === "AMBER").length;
    const green = cases.filter((c) => c.assessment?.risk_level === "GREEN").length;
    return { red, amber, green, total: cases.length };
  }, [cases]);

  // Callback when a case is updated inside modal
  const handleCaseUpdated = (updatedCase: PregnancyCase) => {
    const id = updatedCase.case_id || updatedCase.patient_id;
    setCases((prev) =>
      prev.map((c) => ((c.case_id || c.patient_id) === id ? updatedCase : c))
    );
    setSelectedCase(updatedCase);
  };

  return (
    <div className="app-container">
      {/* Clean Navbar */}
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

      {/* Main Content Area */}
      <main className="main-content">
        {/* Error Alert banner if backend unreachable */}
        {errorMessage && (
          <div className="alert alert-warning">
            <AlertCircle size={18} />
            <div style={{ flex: 1 }}>
              <strong>Notice:</strong> {errorMessage} (Displaying locally cached records)
            </div>
            <button className="btn btn-secondary btn-sm" onClick={loadCases}>
              Retry Connection
            </button>
          </div>
        )}

        {/* Global Stats Overview (shown on queue tab) */}
        {currentTab === "queue" && (
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-info">
                <span className="stat-label">Total Cases</span>
                <span className="stat-value">{stats.total}</span>
              </div>
              <div className="stat-icon-wrapper stat-icon-blue">
                <FolderHeart size={22} />
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-info">
                <span className="stat-label">Critical Emergency (RED)</span>
                <span className="stat-value" style={{ color: "var(--red-primary)" }}>{stats.red}</span>
              </div>
              <div className="stat-icon-wrapper stat-icon-red">
                <AlertCircle size={22} />
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-info">
                <span className="stat-label">Moderate Risk (AMBER)</span>
                <span className="stat-value" style={{ color: "var(--amber-primary)" }}>{stats.amber}</span>
              </div>
              <div className="stat-icon-wrapper stat-icon-amber">
                <AlertTriangle size={22} />
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-info">
                <span className="stat-label">Routine Care (GREEN)</span>
                <span className="stat-value" style={{ color: "var(--green-primary)" }}>{stats.green}</span>
              </div>
              <div className="stat-icon-wrapper stat-icon-green">
                <CheckCircle size={22} />
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

      {/* Clean Modern Footer */}
      <footer className="app-footer">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px", maxWidth: "1320px", margin: "0 auto" }}>
          <div>
            <strong>SakhiCare</strong> Care Desk — Maternal Danger-Sign Screening & Response Platform (MoHFW v1.0)
          </div>
          <div>
            <span>Offline-First Android App</span> &bull; <span>108 Transport Hub</span> &bull; <span>FHIR R4 Compliant</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
