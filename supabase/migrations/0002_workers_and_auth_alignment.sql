-- Production follow-up for projects that already applied 0001.
-- Worker ids are the Supabase Auth user ids used by ASHA devices during sync.
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

alter table public.workers enable row level security;

-- Existing deployments may have the legacy nullable worker reference. New rows
-- are validated by the backend against the authenticated worker profile.
create index if not exists workers_facility_status_idx
  on public.workers (facility_id, status);
