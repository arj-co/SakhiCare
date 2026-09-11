import React, { useState, useEffect } from "react";
import type { TransportRequestItem, Facility } from "../api";
import {
  fetchTransportRequests,
  updateTransportRequest,
  fetchFacilities
} from "../api";
import { 
  Truck, 
  PhoneCall, 
  AlertCircle, 
  CheckCircle2, 
  RefreshCw, 
  X, 
  MapPin,
  Phone
} from "lucide-react";

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
      }, 1000);
    } catch (err: any) {
      setError(err.message || "Failed to update transport coordination");
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "CONFIRMED":
      case "EN_ROUTE":
      case "ARRIVED":
        return <span className="badge badge-green">{status}</span>;
      case "CALL_ATTEMPTED":
        return <span className="badge badge-amber">CALL ATTEMPTED</span>;
      case "FAILED":
        return <span className="badge badge-red">FAILED</span>;
      case "REQUESTED":
      default:
        return <span className="badge badge-blue">REQUESTED</span>;
    }
  };

  const canCoordinate = !userRole || ["DISPATCHER", "MEDICAL_OFFICER", "ADMIN", "SUPERVISOR"].includes(userRole);

  const activeCount = requests.filter((r) => ["REQUESTED", "CALL_ATTEMPTED", "CONFIRMED", "EN_ROUTE"].includes(r.status)).length;
  const pendingConfirmCount = requests.filter((r) => r.status === "CALL_ATTEMPTED").length;
  const enRouteCount = requests.filter((r) => ["EN_ROUTE", "ARRIVED"].includes(r.status)).length;
  const failedCount = requests.filter((r) => r.status === "FAILED").length;

  return (
    <div>
      {/* Title & Actions */}
      <div className="page-title-row">
        <div>
          <h1 className="page-title">108 Emergency Transport Coordination Board</h1>
          <p className="page-subtitle">
            Live coordination with 108 ambulance dispatch and district emergency response teams.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <select
            className="form-control"
            style={{ width: "auto", fontSize: "0.8125rem", padding: "6px 12px" }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="REQUESTED">REQUESTED</option>
            <option value="CALL_ATTEMPTED">CALL ATTEMPTED</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="EN_ROUTE">EN ROUTE</option>
            <option value="ARRIVED">ARRIVED</option>
            <option value="FAILED">FAILED</option>
          </select>

          <button className="btn btn-secondary btn-sm" onClick={loadData}>
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {error && <div className="alert alert-danger">⚠️ {error}</div>}

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Active Transfers</span>
            <span className="stat-value">{activeCount}</span>
          </div>
          <div className="stat-icon-wrapper stat-icon-blue">
            <Truck size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Call Placed / Pending</span>
            <span className="stat-value" style={{ color: "var(--amber-primary)" }}>{pendingConfirmCount}</span>
          </div>
          <div className="stat-icon-wrapper stat-icon-amber">
            <PhoneCall size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">En Route / Arrived</span>
            <span className="stat-value" style={{ color: "var(--green-primary)" }}>{enRouteCount}</span>
          </div>
          <div className="stat-icon-wrapper stat-icon-green">
            <CheckCircle2 size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Failed / Delayed</span>
            <span className="stat-value" style={{ color: "var(--red-primary)" }}>{failedCount}</span>
          </div>
          <div className="stat-icon-wrapper stat-icon-red">
            <AlertCircle size={22} />
          </div>
        </div>
      </div>

      {/* Transport Table */}
      <div className="table-card">
        {loading ? (
          <div className="empty-state">Loading 108 ambulance coordination queue...</div>
        ) : requests.length === 0 ? (
          <div className="empty-state">
            <Truck size={36} className="empty-state-icon" />
            <h3>No active transport requests</h3>
            <p>When high-risk emergency cases trigger 108 ambulance dispatch, they appear here.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Patient & Case</th>
                  <th>Location</th>
                  <th>Status</th>
                  <th>Vehicle ID</th>
                  <th>Destination Hospital</th>
                  <th>Driver Contact</th>
                  <th>Call Notes</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: "var(--text-main)" }}>
                        {r.patient_name || "Emergency Patient"}
                      </div>
                      <a
                        href={`#case-${r.case_id}`}
                        className="case-id-code"
                        onClick={(e) => {
                          e.preventDefault();
                          if (onSelectCase) onSelectCase(r.case_id);
                        }}
                      >
                        {r.case_id}
                      </a>
                    </td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                        <MapPin size={13} color="var(--primary-blue)" />
                        <span>{r.village || "—"}</span>
                      </div>
                    </td>
                    <td>
                      {getStatusBadge(r.status)}
                      {r.confirmed_by && (
                        <div style={{ fontSize: "0.7rem", color: "var(--text-muted)", marginTop: "2px" }}>
                          by {r.confirmed_by}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="case-id-code">
                        {r.vehicle_id || "Unassigned"}
                      </span>
                    </td>
                    <td>{r.destination_facility_name || "Community Health Centre"}</td>
                    <td>
                      {r.driver_name ? (
                        <div>
                          <div style={{ fontWeight: 500 }}>{r.driver_name}</div>
                          {r.driver_phone && (
                            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", display: "flex", alignItems: "center", gap: "3px" }}>
                              <Phone size={11} /> {r.driver_phone}
                            </div>
                          )}
                        </div>
                      ) : (
                        <span style={{ color: "var(--text-light)" }}>—</span>
                      )}
                    </td>
                    <td style={{ maxWidth: "240px" }}>
                      <div style={{ fontSize: "0.8125rem", color: "var(--text-body)" }}>
                        {r.call_attempt_notes || "—"}
                      </div>
                    </td>
                    <td>
                      {canCoordinate && (
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => openUpdateModal(r)}
                        >
                          <PhoneCall size={12} />
                          <span>Update</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Call & Confirm Modal */}
      {activeItem && (
        <div className="modal-overlay">
          <div className="modal-dialog" style={{ maxWidth: "600px" }}>
            <div className="modal-header">
              <h3 className="modal-title">
                <Truck size={18} color="var(--primary-blue)" />
                <span>108 Transport: Case {activeItem.case_id}</span>
              </h3>
              <button className="btn-close" onClick={() => setActiveItem(null)}>
                <X size={18} />
              </button>
            </div>

            {actionSuccess && (
              <div className="alert alert-success" style={{ margin: "16px 24px 0 24px" }}>
                {actionSuccess}
              </div>
            )}

            <form onSubmit={handleUpdateSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Coordination Status Transition:</label>
                  <select
                    value={newStatus}
                    onChange={(e) => setNewStatus(e.target.value)}
                    className="form-control"
                    required
                  >
                    <option value="CALL_ATTEMPTED">CALL ATTEMPTED — Call placed to 108 Call Centre</option>
                    <option value="CONFIRMED">CONFIRMED — Ambulance assigned by 108 dispatch</option>
                    <option value="EN_ROUTE">EN ROUTE — Ambulance moving toward patient</option>
                    <option value="ARRIVED">ARRIVED — Ambulance on scene</option>
                    <option value="FAILED">FAILED — Transport delayed / unavailable</option>
                  </select>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Ambulance Vehicle ID:</label>
                    <input
                      type="text"
                      className="form-control"
                      value={vehicleId}
                      onChange={(e) => setVehicleId(e.target.value)}
                      placeholder="e.g. 108-AMB-Rampur-09"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Destination Facility:</label>
                    <select
                      className="form-control"
                      value={destinationFacilityName}
                      onChange={(e) => setDestinationFacilityName(e.target.value)}
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
                  <div className="form-group">
                    <label className="form-label">Driver Name:</label>
                    <input
                      type="text"
                      className="form-control"
                      value={driverName}
                      onChange={(e) => setDriverName(e.target.value)}
                      placeholder="Santosh Yadav"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Driver Phone:</label>
                    <input
                      type="text"
                      className="form-control"
                      value={driverPhone}
                      onChange={(e) => setDriverPhone(e.target.value)}
                      placeholder="+91 98765 43210"
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Call Notes & Verification Evidence:</label>
                  <textarea
                    rows={3}
                    className="form-control"
                    value={callNotes}
                    onChange={(e) => setCallNotes(e.target.value)}
                    placeholder="Enter operator name, ticket number, estimated ETA..."
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setActiveItem(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Save Coordination Update"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
