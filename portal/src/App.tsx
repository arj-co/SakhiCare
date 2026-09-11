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
import { AlertCircle, AlertTriangle, CheckCircle, Users, ArrowUpRight, HeartPulse, LockKeyhole, Radio } from "lucide-react";
import type { PortalLanguage } from "./i18n";

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
  const [language, setLanguage] = useState<PortalLanguage>(() => (localStorage.getItem("sakhicare-language") as PortalLanguage) || "en");
  const handleLanguageChange = (next: PortalLanguage) => { setLanguage(next); localStorage.setItem("sakhicare-language", next); };

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
        <div className="auth-frame">
          <section className="auth-intro" aria-label="About SakhiCare Care Desk">
            <div className="auth-brand-lockup">
              <span className="brand-mark"><HeartPulse size={20} strokeWidth={2.4} /></span>
              <span className="auth-brand-name">SakhiCare</span>
            </div>
            <div className="eyebrow">CLINICAL OPERATIONS / 01</div>
            <h1>Care coordination, with a human pace.</h1>
            <p className="auth-intro-copy">
              A focused desk for turning frontline screening into the right next action — quickly, clearly, and with dignity.
            </p>
            <div className="auth-principles">
              <div><Radio size={16} /><span><strong>Live care signal</strong><small>Updates from the field as they arrive</small></span></div>
              <div><LockKeyhole size={16} /><span><strong>Protected by design</strong><small>Role-aware access to clinical records</small></span></div>
            </div>
            <div className="auth-intro-footer"><span className="live-dot active" /> Operational workspace for maternal health teams <ArrowUpRight size={14} /></div>
          </section>

          <form className="auth-card" onSubmit={handleLogin}>
            <div className="auth-card-heading">
              <div>
                <div className="eyebrow">WELCOME BACK</div>
                <h2>Sign in to Care Desk</h2>
              </div>
              <span className="auth-secure-badge"><LockKeyhole size={13} /> Secure</span>
            </div>
            <p className="auth-card-description">Use your operator credentials to continue to the clinical workspace.</p>

          <div className="auth-mode-toggle" role="tablist" aria-label="Authentication mode">
            <button
              type="button"
              className={`auth-mode-button ${loginMode === "desk" ? "active" : ""}`}
              onClick={() => setLoginMode("desk")}
            >
              Desk / Local Login
            </button>
            <button
              type="button"
              className={`auth-mode-button ${loginMode === "supabase" ? "active" : ""}`}
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

              <div className="demo-login-block">
                <div className="demo-login-label">Demo workspace · choose a role to pre-fill</div>
                <div className="demo-login-options">
                  <button
                    type="button"
                    className="demo-login-option"
                    onClick={() => setDemoCreds("doctor_sharma", "DoctorPass123!")}
                  >
                    <span>Dr. Sharma</span><small>Medical Officer</small>
                  </button>
                  <button
                    type="button"
                    className="demo-login-option"
                    onClick={() => setDemoCreds("dispatch_108", "DispatchPass123!")}
                  >
                    <span>108 Dispatcher</span><small>Emergency transport</small>
                  </button>
                  <button
                    type="button"
                    className="demo-login-option"
                    onClick={() => setDemoCreds("admin", "AdminPass123!")}
                  >
                    <span>Admin</span><small>System operations</small>
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
          <button className="auth-submit" type="submit" disabled={authLoading}>
            {authLoading ? "Signing in…" : "Continue to Care Desk"}
            {!authLoading && <ArrowUpRight size={16} />}
          </button>
          <p className="auth-card-note"><LockKeyhole size={13} /> Your access is limited to the role and facilities assigned to you.</p>
          </form>
        </div>
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
        isLive={isLive}
        onRefresh={loadCases}
        language={language}
        onLanguageChange={handleLanguageChange}
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
          <div className="dashboard-overview">
            <div className="overview-heading">
              <div>
                <div className="eyra-section-label">Maternal Care Infrastructure</div>
                <p className="overview-kicker">Today’s operational pulse</p>
              </div>
              <span className="overview-updated"><span className="live-dot active" /> Live across the care network</span>
            </div>
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Active encounters</span>
                  <span className="stat-value">{stats.total}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-teal">
                  <Users size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Critical now</span>
                  <span className="stat-value" style={{ color: "var(--red-primary)" }}>{stats.red}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-red">
                  <AlertCircle size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Needs review</span>
                  <span className="stat-value" style={{ color: "var(--amber-primary)" }}>{stats.amber}</span>
                </div>
                <div className="stat-icon-wrapper stat-icon-amber">
                  <AlertTriangle size={22} />
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-info">
                  <span className="stat-label">Routine follow-up</span>
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
