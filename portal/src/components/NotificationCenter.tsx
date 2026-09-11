import React, { useState, useEffect } from "react";
import type { NotificationLog, PregnancyCase } from "../api";
import {
  fetchNotificationLogs,
  escalateNotification,
  fetchCases
} from "../api";
import { 
  Bell, 
  Send, 
  ShieldCheck, 
  RefreshCw, 
  Smartphone, 
  Volume2, 
  AlertTriangle 
} from "lucide-react";

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
        return <span className="badge badge-teal">SENT (IN FLIGHT)</span>;
      case "NOT_CONFIGURED":
        return <span className="badge badge-amber" title="Provider API keys not configured">NOT CONFIGURED</span>;
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
        return <span className="badge badge-teal"><Smartphone size={12} /> SMS</span>;
      case "PUSH":
        return <span className="badge badge-teal"><Bell size={12} /> PUSH</span>;
      case "VOICE_CALL":
        return <span className="badge badge-teal"><Volume2 size={12} /> VOICE IVR</span>;
      default:
        return <span className="badge badge-gray">{channel}</span>;
    }
  };

  const formatTimestamp = (ts?: number | null) => {
    if (!ts) return "—";
    return new Date(ts * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const canEscalate = !userRole || ["MEDICAL_OFFICER", "ADMIN", "SUPERVISOR", "DISPATCHER"].includes(userRole);

  return (
    <div>
      {/* Eyra Section Label & Page Title */}
      <div className="page-title-row">
        <div>
          <div className="eyra-section-label">Telecommunications & Alerts</div>
          <h1 className="page-title">Notification & Escalation Center</h1>
          <p className="page-subtitle">
            Auditable delivery tracking across SMS gateways, OneSignal push notifications, and automated supervisor fallbacks.
          </p>
        </div>

        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <select
            className="form-control"
            style={{ width: "auto", fontSize: "0.8125rem", padding: "8px 14px", borderRadius: "var(--radius-pill)" }}
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
          >
            <option value="ALL">All Delivery Channels</option>
            <option value="SMS">SMS Gateway</option>
            <option value="PUSH">Push Notifications</option>
            <option value="VOICE_CALL">Voice Call IVR</option>
          </select>

          <button className="btn btn-secondary btn-sm" onClick={loadLogs}>
            <RefreshCw size={13} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {error && <div className="alert alert-danger">⚠️ {error}</div>}
      {escalateSuccess && <div className="alert alert-success">🚀 {escalateSuccess}</div>}

      {/* Manual Emergency Escalation Bar */}
      {canEscalate && urgentCases.length > 0 && (
        <div style={{ background: "var(--red-bg)", border: "1px solid var(--red-border)", borderRadius: "var(--radius-lg)", padding: "18px 22px", marginBottom: "20px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "12px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <AlertTriangle size={18} color="var(--red-primary)" />
              <strong style={{ color: "var(--red-text)", fontSize: "0.9rem" }}>
                Emergency Escalation Dispatcher:
              </strong>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
              <select
                value={selectedCaseId}
                onChange={(e) => setSelectedCaseId(e.target.value)}
                className="form-control"
                style={{ width: "auto", fontSize: "0.85rem" }}
              >
                {urgentCases.map((c) => (
                  <option key={c.case_id} value={c.case_id}>
                    {c.case_id} — {c.patient_name} ({c.village}) [RED]
                  </option>
                ))}
              </select>

              <button
                className="btn btn-danger btn-sm"
                onClick={handleTriggerEscalation}
                disabled={escalating}
              >
                <Send size={13} />
                <span>{escalating ? "Dispatching..." : "Trigger Emergency Escalation"}</span>
              </button>
            </div>
          </div>
          <div style={{ fontSize: "0.75rem", color: "var(--red-text)", marginTop: "6px" }}>
            Dispatches primary push notification + minimal SMS. Automatically alerts Block Supervisor if unacknowledged within protocol window.
          </div>
        </div>
      )}

      {/* MoHFW Patient Privacy Guarantee Banner */}
      <div style={{ background: "#FFFFFF", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "16px 20px", marginBottom: "20px", display: "flex", alignItems: "center", gap: "12px", boxShadow: "var(--shadow-subtle)" }}>
        <ShieldCheck size={22} color="var(--medical-teal)" />
        <div style={{ fontSize: "0.85rem", color: "var(--text-body)" }}>
          <strong>MoHFW / DISHA Patient Data Privacy Guarantee:</strong> Plain SMS dispatches strictly exclude patient full legal names. Only Case ID, risk level, village, and clinical callback phone numbers are transmitted across public cellular networks.
        </div>
      </div>

      {/* Logs Table */}
      <div className="table-card">
        {loading ? (
          <div className="empty-state">Loading notification dispatch logs...</div>
        ) : filteredLogs.length === 0 ? (
          <div className="empty-state">
            <Bell size={36} className="empty-state-icon" color="var(--medical-teal)" />
            <h3 style={{ fontFamily: "var(--font-serif)" }}>No notification dispatches logged</h3>
            <p>Outgoing push alerts, SMS transmissions, and voice calls will appear here.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Case ID</th>
                  <th>Channel</th>
                  <th>Recipient</th>
                  <th>Status</th>
                  <th>Message Preview</th>
                  <th>Audit Reference</th>
                </tr>
              </thead>
              <tbody>
                {filteredLogs.map((l) => (
                  <tr key={l.id}>
                    <td style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontFamily: "var(--font-mono)" }}>
                      {formatTimestamp(l.created_at)}
                    </td>
                    <td>
                      <a
                        href={`#case-${l.case_id}`}
                        className="case-id-code"
                        onClick={(e) => {
                          e.preventDefault();
                          if (onSelectCase) onSelectCase(l.case_id);
                        }}
                      >
                        {l.case_id}
                      </a>
                    </td>
                    <td>{getChannelBadge(l.channel)}</td>
                    <td style={{ fontFamily: "var(--font-mono)", fontSize: "0.8rem" }}>
                      {l.recipient}
                    </td>
                    <td>{getStatusBadge(l.status)}</td>
                    <td style={{ maxWidth: "340px" }}>
                      <div style={{ fontSize: "0.8rem", fontFamily: "var(--font-mono)", color: "var(--text-body)" }}>
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
                        <span className="case-id-code">Ref: {l.provider_ref}</span>
                      ) : l.error_message ? (
                        <span style={{ fontSize: "0.75rem", color: "var(--amber-primary)" }} title={l.error_message}>
                          ⚠ {l.error_message.slice(0, 35)}...
                        </span>
                      ) : (
                        <span style={{ color: "var(--text-light)" }}>—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
};
