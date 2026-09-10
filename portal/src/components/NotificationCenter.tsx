import React, { useState, useEffect } from "react";
import type { NotificationLog, PregnancyCase } from "../api";
import {
  fetchNotificationLogs,
  escalateNotification,
  fetchCases
} from "../api";

interface NotificationCenterProps {
  userRole?: string;
  onSelectCase?: (caseId: string) => void;
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({ userRole, onSelectCase }) => {
  const [logs, setLogs] = useState<NotificationLog[]>([]);
  const [urgentCases, setUrgentCases] = useState<PregnancyCase[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [channelFilter, setChannelFilter] = useState<string>("ALL");
  const [selectedCaseId, setSelectedCaseId] = useState<string>("");
  const [escalating, setEscalating] = useState<boolean>(false);
  const [escalateSuccess, setEscalateSuccess] = useState<string | null>(null);

  const loadLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const [logsRes, casesRes] = await Promise.all([
        fetchNotificationLogs(),
        fetchCases({ risk_level: "RED" }).catch(() => ({ count: 0, cases: [] }))
      ]);
      setLogs(logsRes.logs || []);
      setUrgentCases(casesRes.cases || []);
      if (casesRes.cases && casesRes.cases.length > 0 && !selectedCaseId) {
        setSelectedCaseId(casesRes.cases[0].case_id);
      }
    } catch (err: any) {
      setError(err.message || "Failed to load notification logs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
    const interval = setInterval(loadLogs, 12000);
    return () => clearInterval(interval);
  }, []);

  const handleTriggerEscalation = async () => {
    if (!selectedCaseId) return;
    setEscalating(true);
    setEscalateSuccess(null);
    setError(null);
    try {
      const res = await escalateNotification(selectedCaseId);
      setEscalateSuccess(`Emergency escalation dispatched for Case ${res.case_id}! (Supervisor alerted: ${res.escalation_triggered ? "YES" : "NO"})`);
      setTimeout(() => {
        loadLogs();
        setEscalateSuccess(null);
      }, 3000);
    } catch (err: any) {
      setError(err.message || "Failed to trigger emergency escalation");
    } finally {
      setEscalating(false);
    }
  };

  const filteredLogs = logs.filter((l) => {
    if (channelFilter !== "ALL" && l.channel !== channelFilter) return false;
    return true;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "DELIVERED":
        return <span className="badge badge-green">DELIVERED</span>;
      case "SENT":
        return <span className="badge badge-blue">SENT (IN FLIGHT)</span>;
      case "NOT_CONFIGURED":
        return <span className="badge badge-amber" title="No fake delivery without real provider receipt">NOT_CONFIGURED</span>;
      case "FAILED":
        return <span className="badge badge-red">FAILED</span>;
      case "QUEUED":
      default:
        return <span className="badge badge-gray">QUEUED</span>;
    }
  };

  const getChannelBadge = (channel: string) => {
    switch (channel) {
      case "SMS":
        return <span className="mono-badge">📱 SMS</span>;
      case "PUSH":
        return <span className="mono-badge">🔔 PUSH</span>;
      case "VOICE_CALL":
        return <span className="mono-badge">📞 VOICE IVR</span>;
      default:
        return <span className="mono-badge">{channel}</span>;
    }
  };

  const formatTimestamp = (ts?: number | null) => {
    if (!ts) return "—";
    return new Date(ts * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const canEscalate = !userRole || ["MEDICAL_OFFICER", "ADMIN", "SUPERVISOR", "DISPATCHER"].includes(userRole);

  return (
    <div className="section-container">
      <div className="header-row">
        <div>
          <h2 className="title-primary">Multi-Channel Notification & Escalation Center</h2>
          <p className="subtitle">
            Truthful provider delivery status tracking. Minimal privacy-safe SMS templates omitting patient names.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <select
            className="filter-select"
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
          >
            <option value="ALL">All Channels</option>
            <option value="SMS">SMS Gateway</option>
            <option value="PUSH">OneSignal Push</option>
            <option value="VOICE_CALL">Voice Call IVR</option>
          </select>
          <button className="btn-refresh" onClick={loadLogs}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-banner">⚠️ {error}</div>}
      {escalateSuccess && <div className="success-banner">🚀 {escalateSuccess}</div>}

      {/* Manual Escalation Bar */}
      {canEscalate && urgentCases.length > 0 && (
        <div className="escalation-bar">
          <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
            <span style={{ fontWeight: 600, color: "#991b1b" }}>⚡ Emergency Escalation Trigger:</span>
            <select
              value={selectedCaseId}
              onChange={(e) => setSelectedCaseId(e.target.value)}
              className="form-input"
              style={{ minWidth: "220px" }}
            >
              {urgentCases.map((c) => (
                <option key={c.case_id} value={c.case_id}>
                  {c.case_id} — {c.patient_name} ({c.village}) [RED]
                </option>
              ))}
            </select>
            <button
              className="btn-escalate"
              onClick={handleTriggerEscalation}
              disabled={escalating}
            >
              {escalating ? "Dispatching..." : "Dispatch Multi-Channel Escalation"}
            </button>
          </div>
          <small style={{ color: "#64748b", marginTop: "4px", display: "block" }}>
            Triggers Primary Push + Minimal SMS + Automatic Block Supervisor fallback if unconfirmed.
          </small>
        </div>
      )}

      {/* Privacy Notice Alert */}
      <div className="privacy-notice-box">
        <div style={{ fontWeight: 600, color: "#1e293b", marginBottom: "4px" }}>
          🔒 MoHFW / DISHA Patient Privacy Enforcement:
        </div>
        <p style={{ margin: 0, fontSize: "0.875rem", color: "#475569" }}>
          Unencrypted SMS notifications strictly exclude the patient's full legal name and detailed identity.
          Only Case ID, Risk Urgency, Village, and Callback Contact are transmitted over public telecommunications networks.
        </p>
      </div>

      {loading ? (
        <div className="loading-state">Loading notification dispatch logs...</div>
      ) : filteredLogs.length === 0 ? (
        <div className="empty-state">
          <p>No notification dispatch logs recorded yet.</p>
          <p className="subtle">Triggering a case triage or emergency sync will log outgoing multi-channel dispatches here.</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Case ID</th>
                <th>Channel</th>
                <th>Recipient</th>
                <th>Truthful Status</th>
                <th>Message Content Preview</th>
                <th>Provider / Escalation Audit</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((l) => (
                <tr key={l.id}>
                  <td className="subtle-code">{formatTimestamp(l.created_at)}</td>
                  <td>
                    <a
                      href={`#case-${l.case_id}`}
                      className="subtle-code"
                      onClick={(e) => {
                        e.preventDefault();
                        if (onSelectCase) onSelectCase(l.case_id);
                      }}
                    >
                      {l.case_id}
                    </a>
                  </td>
                  <td>{getChannelBadge(l.channel)}</td>
                  <td className="subtle-code">{l.recipient}</td>
                  <td>{getStatusBadge(l.status)}</td>
                  <td style={{ maxWidth: "320px" }}>
                    <div className="text-notes" style={{ fontFamily: "monospace", fontSize: "0.8rem" }}>
                      {l.content_preview}
                    </div>
                  </td>
                  <td>
                    {l.escalated_to && (
                      <span className="badge badge-amber" style={{ marginRight: "6px" }}>
                        Escalated: {l.escalated_to}
                      </span>
                    )}
                    {l.provider_ref ? (
                      <span className="subtle-code">Ref: {l.provider_ref}</span>
                    ) : l.error_message ? (
                      <span className="subtle" title={l.error_message} style={{ color: "#d97706" }}>
                        ⚠️ {l.error_message.slice(0, 40)}...
                      </span>
                    ) : (
                      <span className="subtle">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
