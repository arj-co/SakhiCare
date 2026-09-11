# SakhiCare UI/UX rules

These rules are for the Android ASHA app and the React + Vite Care Desk portal. The product is for a low-connectivity, high-stakes workflow. Clarity, trust, speed, and recoverability take priority over visual novelty.

Voice is intentionally simple: it records what the ASHA says, organizes it into a draft form, and sends the original sound with the confirmed fields. It is not a local LLM, not a diagnosis engine, and not an automatic clinical decision-maker.

## 1. Product language and trust

1. Never show a fake success state. Use precise states: `Saved on this phone`, `Waiting to send`, `Sent to Care Desk`, `Acknowledged`, `Delivery not confirmed`, `Failed — retry`, and `Demo data`.
2. Never say “doctor notified”, “ambulance dispatched”, “live”, “AI”, “offline”, or “encrypted” unless the underlying event is true and verifiable.
3. Every clinical recommendation shows its source/version and the boundary of ASHA scope. Separate “Do now” from “Medical officer decides”.
4. A danger-sign result is decision support, not a diagnosis. Use “Urgent referral recommended” rather than a definitive diagnosis.
5. Never hide uncertainty. Missing, stale, low-confidence, or unconfirmed inputs are visible.
6. Use respectful Hindi/English and the selected local language; avoid English-only abbreviations without a plain-language explanation.
7. Do not display full patient names or detailed clinical data in notifications unless explicitly required and configured.

## 2. ASHA app information architecture

Primary navigation should be limited to:

- **Home** — start assessment, urgent follow-ups, pending sends, connectivity, and last sync.
- **People** — local woman/case list and search.
- **Tasks** — medical-officer guidance, follow-ups, and transport updates.
- **Help** — approved danger-sign reference, language/audio settings, and support contact.

Do not make an AI copilot a primary navigation item. Clinical support belongs inside the active case and the approved Help library. Voice belongs to the assessment flow as an input method.

## 3. Onboarding rules

1. Onboarding is a first-run flow, not a marketing carousel.
2. Ask only what is needed: language, worker identity, facility/catchment, local unlock, permissions, protocol pack, and consent.
3. Do not prefill a real-looking worker name, village, case, BP, Hb, or phone number.
4. Explain offline behaviour in one screen: “You can record a case without internet. It will be sent when a network is available.”
5. Explain what requires connectivity: medical-officer response, SMS/push delivery, facility lookup, and transport confirmation.
6. Show the installed protocol-pack version and last update date.
7. Offer a guided practice case clearly labelled `Practice — not a real patient`; never mix it with production cases.
8. If registration cannot complete offline, allow a limited local setup only when the deployment policy permits it, and show `Worker verification pending`.

## 4. Home screen rules

1. The first action is `Start maternal check`, not a dashboard of decorative metrics.
2. Show one honest connectivity strip with: current state, pending count, and last successful sync time.
3. Put urgent follow-up tasks above summary statistics.
4. Use one primary action per screen. Do not make four equally prominent cards compete for attention.
5. Empty states must explain the next action, for example: “No assessments yet. Start a maternal check.”
6. Do not show invented totals such as “24 mothers” or “5 villages”. Calculate them from stored records.
7. A manual network toggle is allowed only in test builds and must never be present in the release UI.

## 5. Assessment flow rules

1. Use a short progressive flow, not one long form. Recommended order:
   - identify or create local case;
   - immediate danger signs;
   - gestational age and pregnancy history;
   - BP and other available measurements;
   - travel/transport constraints;
   - confirm and submit.
2. Ask emergency questions in plain language and pair each with an optional audio prompt.
3. Every measurement has unit, timestamp, source, and a `Not measured` option.
4. Do not convert blank BP to `120/80` or blank Hb to a normal value.
5. For BP, support repeat measurement and show a simple technique reminder. Flag implausible values for correction.
6. Voice capture must show recording duration, microphone state, consent, pause/stop, and delete-before-submit controls.
7. Save the original audio as an encrypted case attachment immediately, even when transcription is unavailable.
8. Show the transcript, organize it into editable fields, and highlight every extracted field. The worker must confirm before it changes the assessment.
9. Voice never auto-submits a case and never invents a missing name, village, BP, or Hb. Unrecognized content stays in the transcript/audio, not in a guessed field.
10. Keep the worker's progress when navigating back or losing connectivity.
11. Use large tap targets, one-handed layout, high contrast, and persistent Back/Save draft controls.

## 6. Risk result rules

1. Always show three blocks in this order: `What we found`, `What to do now`, `What happens next`.
2. RED result:
   - state the specific danger signs and measurements;
   - show the first-response actions approved for ASHA workers;
   - show `Call medical officer` and `Arrange transport` as separate actions;
   - show whether each action is `Not started`, `Attempted`, `Confirmed`, or `Failed`;
   - make delay and escalation visible.
3. AMBER result:
   - show the review deadline and follow-up task;
   - avoid language that sounds like an immediate emergency unless a RED trigger exists;
   - allow the worker to record the planned facility and family decision.
4. GREEN result:
   - say `No danger sign detected from the information entered`;
   - list what was not measured;
   - provide routine ANC follow-up without implying zero risk.
5. Never rely on colour alone. Pair RED/AMBER/GREEN with text, icon, and urgency label.
6. Risk rationale must be deterministic, human-readable, and tied to the rule-pack version.

## 7. Offline and sync rules

1. Saving locally is a separate event from syncing remotely. Use separate copy and separate icons.
2. The pending queue is visible from Home and the case detail.
3. Show the last successful sync time, not just “Online”.
4. On failure, keep the case and show a retry action plus a plain-language reason.
5. Never discard a record because the server is unavailable.
6. Duplicate submissions must collapse through an idempotency key and remain understandable to the worker.
7. Sync status changes must come from server acknowledgement, not a local button click.
8. When an advisory arrives, show who sent it, when, which case it belongs to, and whether the worker acknowledged reading it.

## 8. SMS, alerts, and transport rules

1. SMS is a backup notification channel, not clinical proof of receipt.
2. Show delivery states separately from clinical states. `SMS sent` does not mean `medical officer acknowledged`.
3. Keep SMS minimal: case reference, urgency, area, short sign summary, and callback route.
4. Never put a full clinical history, Aadhaar number, or unnecessary name in an SMS.
5. Transport actions require a confirmation step with destination, caller, time, channel, and provider reference when available.
6. Never display an invented ambulance number or ETA. If manually entered, label it `Entered by [role] at [time]`.
7. If the primary alert fails, show the next escalation action instead of repeatedly animating a failed alert.
8. Do not use sound as the only alert. Use text, vibration, and a persistent queue item with accessible controls.

## 9. Care Desk portal rules

1. The default landing page is an urgency-sorted queue, not a marketing dashboard.
2. The queue must expose: risk, minutes since received, village/area, ASHA worker, triggered signs, sync status, acknowledgement, transport state, and next action.
3. RED cases remain visually prominent until acknowledged; acknowledgement must be explicit and audited.
4. Case detail uses a timeline: assessment received → officer acknowledged → guidance sent → transport requested → arrival/referral outcome.
5. Keep clinical information and dispatch information in separate sections with role-based visibility.
6. Every mutation uses a confirmation, actor name, timestamp, and reason where appropriate.
7. Portal filtering must work by facility, worker, risk, age, status, date, and transport state; search must not be the only way to find an urgent case.
8. If the live stream disconnects, show `Reconnecting` and fall back to refresh/polling. Do not show a green “Live” badge while stale.
9. Admin screens manage workers, facilities, protocol versions, notification templates, and audit logs. They do not edit clinical history silently.
10. Never put an unrestricted “broadcast to all” control next to case actions. Require role permission and a review step.

## 10. Accessibility and field conditions

1. Minimum touch target: 48dp on Android and 44px on web.
2. Support large text without clipping; test at 200% text size on the portal and Android accessibility font scale.
3. Maintain WCAG AA contrast; do not use pale amber text on white.
4. Support screen readers with labels that include action and context, for example “Arrange transport for case SC-123”.
5. Use icons with text, not emoji as the only semantic signal.
6. Avoid dense tables on narrow screens; use stacked cards with a stable information order.
7. Design for glare, intermittent power, low-end phones, slow networks, and accidental taps.
8. Do not rely on animation for urgency. Respect reduced-motion settings.

## 11. Clinical safety guardrails

1. Rules are versioned, reviewed, and shipped as a signed protocol pack.
2. Every rule test names the clinical scenario and expected action.
3. A negative result must include “based on the information entered” and list missing data.
4. Medication doses, IV procedures, transfusion decisions, and diagnoses are clinician-owned content. ASHA cards should focus on recognition, positioning/safety, communication, referral, and monitoring within approved scope.
5. Any future model-generated text must be clearly marked, constrained by the approved protocol library, and never override deterministic danger-sign rules. The initial product does not require a local model.
6. Translations are clinically reviewed; do not machine-translate emergency instructions directly into production.
7. Add a visible emergency contact path that works without the portal when possible (configured call/SMS/phonebook route).

## 12. Demo and release hygiene

1. Production contains no demo fixtures, seeded patients, fallback users, or fabricated clinical records. Test fixtures may run only when `SAKHICARE_TEST_MODE=true`.
2. Test toggles, fake network state, fake delivery IDs, and canned speech transcripts are unavailable in release builds.
3. The README, screenshots, API docs, and UI labels must match actual behaviour.
4. The release checklist must include: offline restart, encrypted storage, sync retry, duplicate submission, unknown case `404`, role restrictions, alert failure, and no-network transport behaviour.
5. A feature is complete only when its empty, loading, offline, failed, acknowledged, and success states are designed and tested.
