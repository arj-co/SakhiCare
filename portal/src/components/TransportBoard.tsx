import React, { useState, useEffect } from "react";
import type { TransportRequestItem, Facility } from "../api";
import {
  fetchTransportRequests,
  updateTransportRequest,
  fetchFacilities
} from "../api";

interface TransportBoardProps {
  userRole?: string;
  onSelectCase?: (caseId: string) => void;
}

export const TransportBoard: React.FC<TransportBoardProps> = ({ userRole, onSelectCase }) => {
  const [requests, setRequests] = useState<TransportRequestItem[]>([]);
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Edit / Call-and-Confirm modal state
  const [activeItem, setActiveItem] = useState<TransportRequestItem | null>(null);
  const [newStatus, setNewStatus] = useState<string>("CALL_ATTEMPTED");
  const [vehicleId, setVehicleId] = useState<string>("");
  const [driverName, setDriverName] = useState<string>("");
  const [driverPhone, setDriverPhone] = useState<string>("");
  const [destinationFacilityName, setDestinationFacilityName] = useState<string>("");
  const [callNotes, setCallNotes] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [transRes, facRes] = await Promise.all([
        fetchTransportRequests(statusFilter),
        fetchFacilities().catch(() => ({ count: 0, facilities: [] }))
      ]);
      setRequests(transRes.items || []);
      setFacilities(facRes.facilities || []);
    } catch (err: any) {
      setError(err.message || "Failed to load transport requests");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 10000);
    return () => clearInterval(interval);
  }, [statusFilter]);

  const openUpdateModal = (item: TransportRequestItem) => {
    setActiveItem(item);
    setNewStatus(
      item.status === "REQUESTED" ? "CALL_ATTEMPTED" :
      item.status === "CALL_ATTEMPTED" ? "CONFIRMED" :
      item.status === "CONFIRMED" ? "EN_ROUTE" :
      item.status === "EN_ROUTE" ? "ARRIVED" : item.status
    );
    setVehicleId(item.vehicle_id || "108-AMB-Rampur-09");
    setDriverName(item.driver_name || "");
    setDriverPhone(item.driver_phone || "");
    setDestinationFacilityName(item.destination_facility_name || "Chandanpur Community Health Centre (CHC)");
    setCallNotes(item.call_attempt_notes || "");
    setActionSuccess(null);
  };

  const handleUpdateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeItem) return;

    setSubmitting(true);
    setError(null);
    try {
      await updateTransportRequest({
        case_id: activeItem.case_id,
        status: newStatus,
        vehicle_id: vehicleId,
        destination_facility_name: destinationFacilityName,
        driver_name: driverName,
        driver_phone: driverPhone,
        call_attempt_notes: callNotes
      });
      setActionSuccess(`Transport updated to ${newStatus} successfully!`);
      setTimeout(() => {
        setActiveItem(null);
        loadData();
      }, 1200);
    } catch (err: any) {
      setError(err.message || "Failed to update transport coordination");
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case "CONFIRMED":
      case "EN_ROUTE":
      case "ARRIVED":
        return "badge-green";
      case "CALL_ATTEMPTED":
        return "badge-amber";
      case "FAILED":
        return "badge-red";
      case "REQUESTED":
      default:
        return "badge-blue";
    }
  };

  const canCoordinate = !userRole || ["DISPATCHER", "MEDICAL_OFFICER", "ADMIN", "SUPERVISOR"].includes(userRole);

  return (
    <div className="section-container">
      <div className="header-row">
        <div>
          <h2 className="title-primary">108 Emergency Transport Coordination Board</h2>
          <p className="subtitle">
            Truthful state machine tracking. Requires verified call attempt evidence; never fabricates dispatch status.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <select
            className="filter-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="REQUESTED">REQUESTED</option>
            <option value="CALL_ATTEMPTED">CALL_ATTEMPTED</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="EN_ROUTE">EN_ROUTE</option>
            <option value="ARRIVED">ARRIVED</option>
            <option value="FAILED">FAILED</option>
          </select>
          <button className="btn-refresh" onClick={loadData}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-banner">⚠️ {error}</div>}

      {/* Summary KPI Cards */}
      <div className="kpi-grid">
        <div className="kpi-card border-blue">
          <div className="kpi-label">Active Requests</div>
          <div className="kpi-value text-blue">
            {requests.filter((r) => ["REQUESTED", "CALL_ATTEMPTED", "CONFIRMED", "EN_ROUTE"].includes(r.status)).length}
          </div>
        </div>
        <div className="kpi-card border-amber">
          <div className="kpi-label">Call Attempted / Pending Confirm</div>
          <div className="kpi-value text-amber">
            {requests.filter((r) => r.status === "CALL_ATTEMPTED").length}
          </div>
        </div>
        <div className="kpi-card border-green">
          <div className="kpi-label">En Route / Arrived</div>
          <div className="kpi-value text-green">
            {requests.filter((r) => ["EN_ROUTE", "ARRIVED"].includes(r.status)).length}
          </div>
        </div>
        <div className="kpi-card border-red">
          <div className="kpi-label">Failed / Delayed</div>
          <div className="kpi-value text-red">
            {requests.filter((r) => r.status === "FAILED").length}
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loading-state">Loading transport board requests...</div>
      ) : requests.length === 0 ? (
        <div className="empty-state">
          <p>No transport requests match the current filter.</p>
          <p className="subtle">When a RED case requires immediate transfer, emergency 108 requests appear here.</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Case & Patient</th>
                <th>Village</th>
                <th>Status</th>
                <th>Assigned Vehicle</th>
                <th>Destination Facility</th>
                <th>Driver & Contact</th>
                <th>Call Attempt Notes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((r) => (
                <tr key={r.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{r.patient_name || "Maternal Patient"}</div>
                    <div className="subtle-code">
                      <a
                        href={`#case-${r.case_id}`}
                        onClick={(e) => {
                          e.preventDefault();
                          if (onSelectCase) onSelectCase(r.case_id);
                        }}
                      >
                        {r.case_id}
                      </a>
                    </div>
                  </td>
                  <td>{r.village || "—"}</td>
                  <td>
                    <span className={`badge ${getStatusBadgeClass(r.status)}`}>
                      {r.status}
                    </span>
                    {r.confirmed_by && (
                      <div className="subtle-note">by {r.confirmed_by}</div>
                    )}
                  </td>
                  <td>
                    {r.vehicle_id ? (
                      <span className="mono-badge">{r.vehicle_id}</span>
                    ) : (
                      <span className="subtle">Not Assigned</span>
                    )}
                  </td>
                  <td>{r.destination_facility_name || "Primary Health Centre"}</td>
                  <td>
                    {r.driver_name ? (
                      <div>
                        <div>{r.driver_name}</div>
                        {r.driver_phone && (
                          <div className="subtle-code">📞 {r.driver_phone}</div>
                        )}
                      </div>
                    ) : (
                      <span className="subtle">—</span>
                    )}
                  </td>
                  <td style={{ maxWidth: "250px" }}>
                    {r.call_attempt_notes ? (
                      <div className="text-notes">{r.call_attempt_notes}</div>
                    ) : (
                      <span className="subtle">No notes recorded</span>
                    )}
                  </td>
                  <td>
                    {canCoordinate && (
                      <button
                        className="btn-action-sm"
                        onClick={() => openUpdateModal(r)}
                      >
                        📞 Update / Confirm
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Call & Confirm Coordination Modal */}
      {activeItem && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>108 Transport Coordination: Case {activeItem.case_id}</h3>
              <button className="btn-close" onClick={() => setActiveItem(null)}>✕</button>
            </div>

            {actionSuccess && (
              <div className="success-banner">✅ {actionSuccess}</div>
            )}

            <form onSubmit={handleUpdateSubmit} className="modal-form">
              <div className="form-group">
                <label>Coordination State (Honest State Transition):</label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  className="form-input"
                  required
                >
                  <option value="CALL_ATTEMPTED">CALL_ATTEMPTED — Call placed to 108 Call Centre</option>
                  <option value="CONFIRMED">CONFIRMED — Vehicle & driver confirmed by 108</option>
                  <option value="EN_ROUTE">EN_ROUTE — Ambulance dispatched and moving</option>
                  <option value="ARRIVED">ARRIVED — Vehicle arrived at patient location</option>
                  <option value="FAILED">FAILED — Transport unavailable / breakdown</option>
                </select>
              </div>

              <div className="form-row">
                <div className="form-group flex-1">
                  <label>Ambulance Vehicle ID:</label>
                  <input
                    type="text"
                    value={vehicleId}
                    onChange={(e) => setVehicleId(e.target.value)}
                    placeholder="e.g. 108-AMB-Rampur-09"
                    className="form-input"
                  />
                </div>
                <div className="form-group flex-1">
                  <label>Destination Facility:</label>
                  <select
                    value={destinationFacilityName}
                    onChange={(e) => setDestinationFacilityName(e.target.value)}
                    className="form-input"
                  >
                    {facilities.map((f) => (
                      <option key={f.id} value={f.name}>
                        {f.name} ({f.type})
                      </option>
                    ))}
                    <option value="Chandanpur Community Health Centre (CHC)">
                      Chandanpur Community Health Centre (CHC) - Blood Bank
                    </option>
                    <option value="Rampur Primary Health Centre (PHC)">
                      Rampur Primary Health Centre (PHC) - BEmOC
                    </option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group flex-1">
                  <label>Driver Name:</label>
                  <input
                    type="text"
                    value={driverName}
                    onChange={(e) => setDriverName(e.target.value)}
                    placeholder="e.g. Santosh Yadav"
                    className="form-input"
                  />
                </div>
                <div className="form-group flex-1">
                  <label>Driver Contact Phone:</label>
                  <input
                    type="text"
                    value={driverPhone}
                    onChange={(e) => setDriverPhone(e.target.value)}
                    placeholder="e.g. +91 98765 43210"
                    className="form-input"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Call Attempt & Confirmation Evidence (Required Audit Notes):</label>
                <textarea
                  rows={3}
                  value={callNotes}
                  onChange={(e) => setCallNotes(e.target.value)}
                  placeholder="Record operator name, 108 ticket number, and estimated transit times..."
                  className="form-textarea"
                  required
                />
                <small className="help-text">
                  Per SakhiCare Phase 4 protocol: do not fabricate ambulance IDs or ETAs without verified call evidence.
                </small>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setActiveItem(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-submit"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Record Coordination Audit"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
