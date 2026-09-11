-- SakhiCare production schema for Supabase Postgres.
-- Apply through the Supabase SQL editor or Supabase CLI.

create extension if not exists pgcrypto;

create table if not exists public.facilities (
  id text primary key,
  name text not null,
  type text not null default 'PHC' check (type in ('SUB_CENTRE', 'PHC', 'CHC', 'DH')),
  catchment_area text,
  contact_phone text,
  latitude double precision,
  longitude double precision,
  capabilities_json text not null default '[]',
  created_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.user_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  role text not null check (role in ('ASHA', 'MEDICAL_OFFICER', 'SUPERVISOR', 'DISPATCHER', 'ADMIN')),
  facility_id text references public.facilities(id),
  phone text,
  locale text not null default 'hi-IN',
  is_active boolean not null default true,
  created_at bigint not null default extract(epoch from now())::bigint,
  updated_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.workers (
  id text primary key,
  name text not null,
  role text not null default 'ASHA' check (role in ('ASHA', 'SUPERVISOR')),
  phone text not null,
  facility_id text not null references public.facilities(id),
  locale text not null default 'hi-IN',
  status text not null default 'ACTIVE',
  pin_hash text,
  last_seen_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.pregnancy_cases (
  id text primary key,
  local_id text not null,
  patient_name text not null,
  village text not null,
  age_years integer,
  gestational_age_weeks integer,
  gravida integer,
  para integer,
  travel_constraints text,
  latitude double precision,
  longitude double precision,
  location_accuracy_m double precision,
  location_captured_at bigint,
  worker_id text,
  facility_id text references public.facilities(id),
  sync_status text not null default 'ACKNOWLEDGED',
  doctor_advisory text,
  ambulance_status text,
  is_demo boolean not null default false,
  created_at bigint not null default extract(epoch from now())::bigint,
  updated_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.assessments (
  id text primary key,
  case_id text not null references public.pregnancy_cases(id) on delete cascade,
  rule_pack_version text not null default 'mohfw-hrp-v1.0',
  risk_level text not null check (risk_level in ('RED', 'AMBER', 'GREEN')),
  risk_score integer not null default 10,
  primary_factors_json text not null default '[]',
  unmeasured_vitals_json text not null default '[]',
  clinical_rationale text,
  recommended_protocol text,
  asha_safe_actions_json text not null default '[]',
  clinician_directed_actions_json text not null default '[]',
  requires_immediate_ambulance boolean not null default false,
  requires_blood_transfusion_alert boolean not null default false,
  blood_pressure text,
  haemoglobin numeric,
  danger_signs_json text not null default '{}',
  created_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.voice_artifacts (
  id text primary key,
  case_id text not null references public.pregnancy_cases(id) on delete cascade,
  storage_path text,
  file_path text,
  filename text not null,
  mime_type text not null default 'audio/mp4',
  file_size_bytes bigint not null default 0,
  sha256 text not null,
  language text not null default 'hi-IN',
  duration_seconds integer not null default 0,
  transcript text,
  processing_status text not null default 'UNPROCESSED',
  upload_status text not null default 'UPLOADED',
  retention_due_at bigint,
  uploaded_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.case_events (
  id text primary key,
  case_id text not null references public.pregnancy_cases(id) on delete cascade,
  event_type text not null,
  actor_id text not null,
  actor_role text not null default 'SYSTEM',
  summary text not null,
  details_json text,
  occurred_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.outbox_items (
  id text primary key,
  case_id text not null,
  entity_type text not null default 'CASE_ASSESSMENT',
  payload_json text not null,
  attempts integer not null default 0,
  status text not null default 'ACKNOWLEDGED',
  last_error text,
  created_at bigint not null default extract(epoch from now())::bigint,
  updated_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.transport_requests (
  id text primary key,
  case_id text not null references public.pregnancy_cases(id) on delete cascade,
  status text not null default 'REQUESTED' check (status in ('REQUESTED', 'CALL_ATTEMPTED', 'CONFIRMED', 'EN_ROUTE', 'ARRIVED', 'FAILED')),
  vehicle_id text,
  destination_facility_id text references public.facilities(id),
  destination_facility_name text,
  driver_name text,
  driver_phone text,
  call_attempt_notes text,
  confirmed_by text,
  created_at bigint not null default extract(epoch from now())::bigint,
  updated_at bigint not null default extract(epoch from now())::bigint
);

create table if not exists public.notification_logs (
  id text primary key,
  case_id text references public.pregnancy_cases(id) on delete set null,
  channel text not null check (channel in ('SMS', 'PUSH', 'VOICE_CALL')),
  recipient text not null,
  template_type text not null,
  content_preview text not null,
  status text not null check (status in ('QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'UNKNOWN', 'NOT_CONFIGURED')),
  provider_ref text,
  error_message text,
  escalated_to text,
  created_at bigint not null default extract(epoch from now())::bigint,
  delivered_at bigint
);

create index if not exists pregnancy_cases_facility_risk_idx on public.pregnancy_cases (facility_id, created_at desc);
create index if not exists assessments_case_created_idx on public.assessments (case_id, created_at desc);
create index if not exists case_events_case_time_idx on public.case_events (case_id, occurred_at asc);
create index if not exists notification_logs_case_time_idx on public.notification_logs (case_id, created_at desc);

alter table public.facilities enable row level security;
alter table public.user_profiles enable row level security;
alter table public.workers enable row level security;
alter table public.pregnancy_cases enable row level security;
alter table public.assessments enable row level security;
alter table public.voice_artifacts enable row level security;
alter table public.case_events enable row level security;
alter table public.outbox_items enable row level security;
alter table public.transport_requests enable row level security;
alter table public.notification_logs enable row level security;

-- The backend uses the Supabase service role and enforces facility/role access.
-- No public/anon table policy is intentionally granted here.

insert into storage.buckets (id, name, public)
values ('sakhicare-audio', 'sakhicare-audio', false)
on conflict (id) do nothing;
