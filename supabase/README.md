# Supabase setup

SakhiCare uses Supabase for production Postgres, Auth, and Storage. The Android APK and Care Desk must never contain a Supabase service-role key.

The connected project is `sngvedrflxkwsrjwmcab` (`https://sngvedrflxkwsrjwmcab.supabase.co`).

## Required production values

Configure these only in the backend/runtime secret store:

- `APP_ENV=production`
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_JWT_SECRET`
- `SUPABASE_DATABASE_URL` or `DATABASE_URL` using the Supabase pooled Postgres connection string
- `SUPABASE_STORAGE_BUCKET=sakhicare-audio`
- `CORS_ORIGINS` with the exact Care Desk origin(s)
- `SMS_GATEWAY_URL`, `SMS_GATEWAY_API_KEY`, `EMERGENCY_MO_PHONE`, and `SUPERVISOR_PHONE`
- `ONESIGNAL_APP_ID` and `ONESIGNAL_REST_API_KEY`
- Clinical reviewer metadata after formal protocol approval

The portal may receive only the public Supabase URL and anon key. Never put `SUPABASE_SERVICE_ROLE_KEY`, database passwords, JWT secrets, or provider API keys in the portal or APK.

## Apply the schema

1. Open the authenticated Supabase project SQL editor.
2. Run `supabase/migrations/0001_sakhicare.sql`, then `0002_workers_and_auth_alignment.sql` for an existing project.
3. Create the `sakhicare-audio` private Storage bucket if it was not created by the migration.
4. Create each Auth user, add a reviewed row in `public.user_profiles`, and add the matching ASHA row in `public.workers` using the Auth user UUID as `workers.id`.
5. Register real referral facilities in `public.facilities`; do not use sample facility codes.
6. Set the backend environment values and start the backend.

The repository does not contain real worker accounts, facilities, phone numbers,
provider credentials, or clinical approval metadata. Those must be provisioned by
the deployment owner; the application intentionally fails fast in production when
the required runtime values are missing.

## Key boundaries

- Browser/portal and the Android APK: `SUPABASE_URL` plus the public publishable/anon key only. These values identify the project but cannot perform privileged database or Storage operations.
- Backend secret store only: `SUPABASE_DATABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`, `ONESIGNAL_REST_API_KEY`, and `SMS_GATEWAY_API_KEY`.
- Never commit a `.env` file, database password, service-role key, JWT secret, SMS key, or OneSignal REST key. A missing provider key must remain visibly `NOT_CONFIGURED`; it must never be reported as sent.

The migration is designed for the backend service role to access the tables while row-level security remains enabled. The backend still performs facility and role checks for every API request.

## Authentication model

Supabase Auth owns credentials and sessions. `public.user_profiles` owns SakhiCare role/facility assignment. The backend validates the Supabase JWT and resolves the profile before allowing case, audio, notification, or transport operations.
