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

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8000";

// Pre-configured default users for role-based switching
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

  // Authenticate user when role is changed
  const handleRoleChange = async (role: UserProfile["role"]) => {
    const targetUser = DEFAULT_USERS[role];
    setCurrentUser(targetUser);

    // Attempt login to acquire real backend JWT for this role
    try {
      const passwords: Record<string, string> = {
        doctor_sharma: "DoctorPass123!",
        supervisor_anita: "SuperPass123!",
        dispatch_108: "DispatchPass123!",
        admin_sakhicare: "AdminPass123!"
      };
      await login(targetUser.username, passwords[targetUser.username] || "Password123!");
    } catch (err) {
      console.warn("Backend auth offline or using fallback token", err);
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
    // Initial login as doctor
    handleRoleChange("MEDICAL_OFFICER");
    loadCases();
  }, [loadCases]);

  // Connect to SSE live stream for real-time queue updates
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
            // Heartbeat or parse error
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

  // Compute critical RED cases count for navbar badge
  const criticalCount = useMemo(() => {
    return cases.filter((c) => c.assessment?.risk_level === "RED").length;
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
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      
      {/* Top Glass Navigation Bar */}
      <Navbar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        currentUser={currentUser}
        onChangeRole={handleRoleChange}
        criticalCount={criticalCount}
        totalCases={cases.length}
        isLive={isLive}
        onRefresh={loadCases}
      />

      {/* Main Screen Content */}
      <main style={{ flex: 1 }}>
        {errorMessage && (
          <div style={{
            margin: "0 24px 20px 24px",
            padding: "12px 16px",
            borderRadius: "8px",
            background: "rgba(239, 68, 68, 0.15)",
            border: "1px solid rgba(239, 68, 68, 0.3)",
            color: "#fca5a5",
            fontSize: "0.85rem",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between"
          }}>
            <span>⚠️ Backend connection notice: {errorMessage} (Displaying cached/local queue)</span>
            <button onClick={loadCases} className="btn-outline" style={{ fontSize: "0.75rem", padding: "4px 10px" }}>
              Retry
            </button>
          </div>
        )}

        {currentTab === "queue" && (
          <UrgentQueue
            cases={cases}
            loading={loading}
            onRefresh={loadCases}
            onSelectCase={(c) => setSelectedCase(c)}
          />
        )}

        {currentTab === "facilities" && <FacilityRoster />}

        {currentTab === "workers" && <WorkerRoster />}

        {currentTab === "protocols" && <ClinicalProtocols />}

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
      </main>

      {/* Case Detail Console Modal */}
      {selectedCase && (
        <CaseDetailModal
          caseItem={selectedCase}
          currentUser={currentUser}
          onClose={() => setSelectedCase(null)}
          onCaseUpdated={handleCaseUpdated}
        />
      )}

      {/* Footer */}
      <footer style={{
        padding: "16px 24px",
        borderTop: "1px solid var(--border-subtle)",
        background: "rgba(9, 13, 22, 0.9)",
        fontSize: "0.75rem",
        color: "var(--text-muted)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: "12px"
      }}>
        <div>
          <span>SakhiCare Maternal Danger-Sign Screening & Response Platform • </span>
          <span style={{ color: "#38bdf8" }}>MoHFW Clinical Rule Pack v1.0</span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <span>Offline Room SQLCipher ASHA App</span>
          <span>•</span>
          <span>Durable FastAPI Backend</span>
          <span>•</span>
          <span>FHIR R4 Interoperability</span>
        </div>
      </footer>

    </div>
  );
};

export default App;

