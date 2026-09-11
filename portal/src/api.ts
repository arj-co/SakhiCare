/**
 * SakhiCare Care Desk Portal — Typed API Client & Contract Models
 * Connects to versioned `/api/v1` endpoints with token-based Bearer auth and fallback resilience.
 */

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8000";

export interface DangerSigns {
  bleeding?: boolean;
  fever?: boolean;
  headache?: boolean;
  reduced_fetal_movement?: boolean;
  convulsions_or_vision_loss?: boolean;
  [key: string]: boolean | undefined;
}

export interface CaseAssessment {
  risk_level: "RED" | "AMBER" | "GREEN";
  risk_score: number;
  blood_pressure?: string | null;
  haemoglobin?: number | null;
  danger_signs?: DangerSigns;
  primary_factors: string[];
  unmeasured_vitals?: string[];
  clinical_rationale: string;
  recommended_protocol: string;
  asha_safe_actions?: string[];
  clinician_directed_actions?: string[];
  requires_immediate_ambulance?: boolean;
  requires_blood_transfusion_alert?: boolean;
}

export interface VoiceArtifact {
  artifact_id: string;
  filename: string;
  sha256: string;
  duration_seconds: number;
  language: string;
  transcript?: string | null;
  upload_status: string;
  retention_due_at?: number;
}

export interface CaseEvent {
  event_id?: string;
  id?: string;
  event_type: string;
  actor_id: string;
  actor_role: string;
  summary: string;
  details?: Record<string, unknown> | null;
  occurred_at: number;
}

export interface PregnancyCase {
  case_id: string;
  patient_id?: string;
  patient_name: string;
  village: string;
  age_years?: number | null;
  gestational_age_weeks?: number | null;
  gravida?: number | null;
  para?: number | null;
  travel_constraints?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  location_accuracy_m?: number | null;
  location_captured_at?: number | null;
  worker_id?: string | null;
  facility_id?: string | null;
  sync_status: string;
  doctor_advisory?: string | null;
  ambulance_status?: string | null;
  created_at?: number;
  updated_at?: number;
  assessment?: CaseAssessment | null;
  audio_artifact?: VoiceArtifact | null;
  timeline?: CaseEvent[];
}

export interface Facility {
  id: string;
  name: string;
  type: string;
  catchment_area?: string | null;
  contact_phone?: string | null;
}

export interface Worker {
  id: string;
  name: string;
  role: string;
  phone: string;
  facility_id: string;
  locale: string;
  status: string;
  last_seen_at?: number;
}

export interface UserProfile {
  id: string;
  username: string;
  full_name: string;
  role: "MEDICAL_OFFICER" | "SUPERVISOR" | "DISPATCHER" | "ADMIN";
  facility_id?: string | null;
  email?: string;
}

// Current auth token memory & localStorage
let currentToken: string | null = localStorage.getItem("sakhicare_auth_token") || null;

export function setAuthToken(token: string | null) {
  currentToken = token;
  if (token) {
    localStorage.setItem("sakhicare_auth_token", token);
  } else {
    localStorage.removeItem("sakhicare_auth_token");
  }
}

export function getAuthToken(): string | null {
  return currentToken;
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  };

  if (currentToken) {
    headers["Authorization"] = `Bearer ${currentToken}`;
  }

  const url = `${API_BASE}${endpoint}`;
  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}: ${response.statusText}`;
    try {
      const errJson = await response.json();
      if (errJson.detail) {
        errorDetail = typeof errJson.detail === "string" ? errJson.detail : JSON.stringify(errJson.detail);
      }
    } catch {
      // ignore
    }
    throw new Error(errorDetail);
  }

  return response.json() as Promise<T>;
}

// Auth Endpoints
export async function login(username: string, password: string): Promise<{ access_token: string; user: UserProfile }> {
  const data = await request<{ access_token: string; token_type: string; user: UserProfile }>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  setAuthToken(data.access_token);
  return data;
}

export async function loginWithSupabase(email: string, password: string): Promise<UserProfile> {
  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL as string | undefined;
  const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;
  if (!supabaseUrl || !anonKey) throw new Error("Supabase Auth is not configured for this Care Desk build");
  const response = await fetch(`${supabaseUrl}/auth/v1/token?grant_type=password`, {
    method: "POST",
    headers: { "Content-Type": "application/json", apikey: anonKey },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error("Supabase sign-in failed");
  const session = await response.json() as { access_token?: string };
  if (!session.access_token) throw new Error("Supabase did not return an access token");
  setAuthToken(session.access_token);
  return getMe();
}

export async function getMe(): Promise<UserProfile> {
  return request<UserProfile>("/api/v1/auth/me");
}

// Case Endpoints
export async function fetchCases(filters?: { risk_level?: string; facility_id?: string; status?: string }): Promise<{ count: number; cases: PregnancyCase[] }> {
  const params = new URLSearchParams();
  if (filters?.risk_level && filters.risk_level !== "ALL") params.append("risk_level", filters.risk_level);
  if (filters?.facility_id && filters.facility_id !== "ALL") params.append("facility_id", filters.facility_id);
  if (filters?.status && filters.status !== "ALL") params.append("status", filters.status);

  const query = params.toString() ? `?${params.toString()}` : "";
  return request<{ count: number; cases: PregnancyCase[] }>(`/api/v1/cases${query}`);
}

export async function fetchCaseDetail(caseId: string): Promise<PregnancyCase> {
  return request<PregnancyCase>(`/api/v1/cases/${encodeURIComponent(caseId)}`);
}

export async function acknowledgeCase(caseId: string, advisoryText: string, referralFacilityId?: string): Promise<PregnancyCase> {
  return request<PregnancyCase>(`/api/v1/cases/${encodeURIComponent(caseId)}/acknowledge`, {
    method: "POST",
    body: JSON.stringify({ advisory_text: advisoryText, referral_facility_id: referralFacilityId }),
  });
}

export async function updateTransport(caseId: string, vehicleId: string, destinationFacility: string, driverPhone?: string): Promise<PregnancyCase> {
  return request<PregnancyCase>(`/api/v1/cases/${encodeURIComponent(caseId)}/transport`, {
    method: "POST",
    body: JSON.stringify({
      vehicle_id: vehicleId,
      destination_facility: destinationFacility,
      driver_phone: driverPhone,
    }),
  });
}

export async function fetchCaseEvents(caseId: string): Promise<{ case_id: string; count: number; events: CaseEvent[] }> {
  return request<{ case_id: string; count: number; events: CaseEvent[] }>(`/api/v1/cases/${encodeURIComponent(caseId)}/events`);
}

// Roster Endpoints
export async function fetchFacilities(): Promise<{ count: number; facilities: Facility[] }> {
  return request<{ count: number; facilities: Facility[] }>("/api/v1/facilities");
}

export async function fetchWorkers(): Promise<{ count: number; workers: Worker[] }> {
  return request<{ count: number; workers: Worker[] }>("/api/v1/workers");
}

// Audio Stream URL
export function getAudioStreamUrl(caseId: string): string {
  return `${API_BASE}/api/v1/cases/${encodeURIComponent(caseId)}/audio`;
}

// FHIR Export URL
export async function fetchFhirBundle(patientId: string): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>(`/fhir/export/${encodeURIComponent(patientId)}`);
}

// ── Phase 4 Models & Endpoints ──

export interface TransportRequestItem {
  id: string;
  case_id: string;
  patient_name?: string;
  village?: string;
  status: "REQUESTED" | "CALL_ATTEMPTED" | "CONFIRMED" | "EN_ROUTE" | "ARRIVED" | "FAILED";
  vehicle_id?: string | null;
  destination_facility_id?: string | null;
  destination_facility_name?: string | null;
  driver_name?: string | null;
  driver_phone?: string | null;
  call_attempt_notes?: string | null;
  confirmed_by?: string | null;
  created_at: number;
  updated_at: number;
}

export interface FacilityRecommendation {
  recommended_facility_id: string;
  recommended_facility_name: string;
  level: "PHC" | "CHC" | "DH";
  capabilities: string[];
  rationale: string;
}

export interface ClinicalProtocolMetadata {
  rule_pack_version: string;
  standard: string;
  status: string;
  clinical_reviewer: {
    name: string;
    qualifications: string;
    designation: string;
    approved_at: string;
  };
  danger_signs_definitions: Array<{
    code: string;
    title: string;
    triage: "RED" | "AMBER" | "GREEN";
  }>;
  scope_of_practice: {
    asha_safe_actions: string[];
    clinician_directed_actions: string[];
  };
}

export interface NotificationLog {
  id: string;
  case_id: string;
  channel: "SMS" | "PUSH" | "VOICE_CALL";
  recipient: string;
  template_type: string;
  content_preview: string;
  status: "QUEUED" | "SENT" | "DELIVERED" | "FAILED" | "NOT_CONFIGURED";
  provider_ref?: string | null;
  error_message?: string | null;
  escalated_to?: string | null;
  created_at: number;
  delivered_at?: number | null;
}

export async function fetchTransportRequests(status?: string): Promise<{ count: number; items: TransportRequestItem[] }> {
  const query = status && status !== "ALL" ? `?status=${encodeURIComponent(status)}` : "";
  return request<{ count: number; items: TransportRequestItem[] }>(`/api/v1/transport/requests${query}`);
}

export async function updateTransportRequest(payload: {
  case_id: string;
  status: string;
  vehicle_id?: string;
  destination_facility_id?: string;
  destination_facility_name?: string;
  driver_name?: string;
  driver_phone?: string;
  call_attempt_notes?: string;
}): Promise<TransportRequestItem> {
  return request<TransportRequestItem>("/api/v1/transport/requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function fetchFacilityRecommendation(caseId: string): Promise<FacilityRecommendation> {
  return request<FacilityRecommendation>(`/api/v1/cases/${encodeURIComponent(caseId)}/recommend-facility`);
}

export async function fetchClinicalProtocols(): Promise<ClinicalProtocolMetadata> {
  return request<ClinicalProtocolMetadata>("/api/v1/clinical/protocols");
}

export async function fetchNotificationLogs(caseId?: string): Promise<{ count: number; logs: NotificationLog[] }> {
  const query = caseId ? `?case_id=${encodeURIComponent(caseId)}` : "";
  return request<{ count: number; logs: NotificationLog[] }>(`/api/v1/notifications/logs${query}`);
}

export async function escalateNotification(caseId: string): Promise<{ case_id: string; escalation_triggered: boolean; notifications: any[] }> {
  return request<{ case_id: string; escalation_triggered: boolean; notifications: any[] }>(`/api/v1/notifications/escalate/${encodeURIComponent(caseId)}`, {
    method: "POST",
  });
}
