import React, { useState, useEffect, useCallback, useMemo } from "react";
import type { PregnancyCase, UserProfile } from "./api";
import { fetchCases, getAuthToken, getMe, login, loginWithSupabase, setAuthToken } from "./api";
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
export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [currentTab, setCurrentTab] = useState<"queue" | "facilities" | "workers" | "protocols" | "transport" | "notifications">("queue");
  const [cases, setCases] = useState<PregnancyCase[]>([]);
  const [selectedCase, setSelectedCase] = useState<PregnancyCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [loginMode, setLoginMode] = useState<"desk" | "supabase">("desk");
  const [authUsername, setAuthUsername] = useState("doctor_sharma");
  const [authEmail, setAuthEmail] = useState("");
  const [authPassword, setAuthPassword] = useState("DoctorPass123!");
  const [authError, setAuthError] = useState<string | null>(null);
  const [authLoading, setAuthLoading] = useState(false);

  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError(null);
    try {
      if (loginMode === "supabase") {
        const user = await loginWithSupabase(authEmail.trim(), authPassword);
        setCurrentUser(user);
      } else {
        const res = await login(authUsername.trim(), authPassword);
        setCurrentUser(res.user);
      }
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : "Unable to sign in");
    } finally {
      setAuthLoading(false);
    }
  };

  const setDemoCreds = (user: string, pass: string) => {
    setLoginMode("desk");
    setAuthUsername(user);
    setAuthPassword(pass);
    setAuthError(null);
  };

  const loadCases = useCallback(async () => {
    if (!getAuthToken()) return;
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
    if (getAuthToken()) {
      getMe().then((user) => {
        setCurrentUser(user);
        void loadCases();
      }).catch(() => setAuthToken(null));
    } else {
      setLoading(false);
    }
  }, [loadCases]);

  const handleLogout = () => {
    setAuthToken(null);
    setCurrentUser(null);
    setCases([]);
  };

  // Connect to SSE stream
  useEffect(() => {
    if (!currentUser || !getAuthToken()) return;
    let eventSource: EventSource | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout>;

    const connectSSE = () => {
      try {
        eventSource = new EventSource(`${API_BASE}/api/v1/live-stream?access_token=${encodeURIComponent(getAuthToken() || "")}`);

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
  }, [currentUser]);

  const stats = useMemo(() => {
    const red = cases.filter((c) => c.assessment?.risk_level === "RED").length;
    const amber = cases.filter((c) => c.assessment?.risk_level === "AMBER").length;
    const green = cases.filter((c) => c.assessment?.risk_level === "GREEN").length;
    const demoCount = cases.filter((c) => (c as any).is_demo).length;
    return { red, amber, green, total: cases.length, demoCount };
  }, [cases]);

  const handleCaseUpdated = (updatedCase: PregnancyCase) => {
    const id = updatedCase.case_id || updatedCase.patient_id;
    setCases((prev) =>
      prev.map((c) => ((c.case_id || c.patient_id) === id ? updatedCase : c))
    );
    setSelectedCase(updatedCase);
  };

  if (!currentUser) {
    return (
      <div className="auth-shell">
        <form className="auth-card" onSubmit={handleLogin}>
          <div className="eyebrow">SAKHICARE CARE DESK</div>
          <h1>Sign in to the clinical operations desk</h1>
          <p>Select your authentication mode and enter credentials.</p>

          <div style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
            <button
              type="button"
              className={`btn btn-sm ${loginMode === "desk" ? "btn-primary" : "btn-secondary"}`}
              onClick={() => setLoginMode("desk")}
            >
              Desk / Local Login
            </button>
            <button
              type="button"
              className={`btn btn-sm ${loginMode === "supabase" ? "btn-primary" : "btn-secondary"}`}
              onClick={() => setLoginMode("supabase")}
            >
              Supabase Auth
            </button>
          </div>

          {loginMode === "desk" ? (
            <>
              <label>
                Username
                <input
                  type="text"
                  value={authUsername}
                  onChange={(event) => setAuthUsername(event.target.value)}
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={authPassword}
                  onChange={(event) => setAuthPassword(event.target.value)}
                  required
                />
              </label>

              {/* Quick Demo Login Pills */}
              <div style={{ marginTop: "8px", marginBottom: "16px" }}>
                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginBottom: "4px" }}>
                  Quick Operator Fill:
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: "0.72rem", padding: "3px 8px" }}
                    onClick={() => setDemoCreds("doctor_sharma", "DoctorPass123!")}
                  >
                    Dr. Sharma (MO)
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: "0.72rem", padding: "3px 8px" }}
                    onClick={() => setDemoCreds("dispatch_108", "DispatchPass123!")}
                  >
                    108 Dispatcher
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: "0.72rem", padding: "3px 8px" }}
                    onClick={() => setDemoCreds("admin", "AdminPass123!")}
                  >
                    Admin
                  </button>
                </div>
              </div>
            </>
          ) : (
            <>
              <label>
                Email
                <input
                  type="email"
                  value={authEmail}
                  onChange={(event) => setAuthEmail(event.target.value)}
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={authPassword}
                  onChange={(event) => setAuthPassword(event.target.value)}
                  required
                />
              </label>
            </>
          )}

          {authError && <div className="alert alert-danger">{authError}</div>}
          <button className="primary-button" type="submit" disabled={authLoading}>
            {authLoading ? "Signing in…" : "Sign in securely"}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="app-container">
      {/* Eyra-inspired TopBar */}
      <Navbar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        currentUser={currentUser}
        onLogout={handleLogout}
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

        {stats.demoCount > 0 && (
          <div className="alert alert-info" style={{ background: "rgba(59, 130, 246, 0.1)", border: "1px solid rgba(59, 130, 246, 0.3)", color: "#1d4ed8", marginBottom: "16px" }}>
            <AlertTriangle size={18} />
            <div style={{ flex: 1 }}>
              <strong>Demo Mode Active:</strong> {stats.demoCount} demonstration case(s) loaded. These cases are flagged with <code>is_demo=true</code> for training and operational testing.
            </div>
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
