"""
SakhiCare Care Desk & Support Command Center
HTML5 / CSS3 / Vanilla JS single-page clinical operations portal with Server-Sent Events (SSE).
"""

CARE_DESK_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SakhiCare Care Desk | Maternal Triage Command Center</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0D9488;
            --primary-dark: #0F766E;
            --primary-light: #CCFBF1;
            --coral-accent: #E8647C;
            --coral-light: #FFF0F3;
            --bg: #F8FAFC;
            --surface: #FFFFFF;
            --text-main: #0F172A;
            --text-muted: #64748B;
            --border: #E2E8F0;
            --red-alert: #EF4444;
            --red-bg: #FEE2E2;
            --amber-alert: #F59E0B;
            --amber-bg: #FEF3C7;
            --green-ok: #10B981;
            --green-bg: #D1FAE5;
            --shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
            --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.08), 0 2px 4px -1px rgba(0,0,0,0.04);
            --shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.08), 0 4px 6px -2px rgba(0,0,0,0.03);
            --radius-md: 14px;
            --radius-lg: 20px;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg);
            color: var(--text-main);
            line-height: 1.5;
            min-height: 100vh;
        }

        /* ── Header ── */
        header {
            background: linear-gradient(135deg, #0F766E 0%, #0D9488 50%, #14B8A6 100%);
            color: white;
            padding: 16px 28px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 4px 20px rgba(13, 148, 136, 0.25);
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .brand-area {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .brand-icon {
            width: 44px;
            height: 44px;
            background: rgba(255, 255, 255, 0.2);
            backdrop-filter: blur(8px);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            border: 1px solid rgba(255, 255, 255, 0.3);
        }

        .brand-text h1 {
            font-size: 20px;
            font-weight: 800;
            letter-spacing: -0.5px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .brand-text p {
            font-size: 12px;
            opacity: 0.85;
            font-weight: 500;
        }

        .header-controls {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .live-badge {
            background: rgba(16, 185, 129, 0.2);
            border: 1px solid #10B981;
            color: #D1FAE5;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .live-dot {
            width: 8px;
            height: 8px;
            background: #10B981;
            border-radius: 50%;
            animation: pulse-dot 1.5s infinite;
        }

        @keyframes pulse-dot {
            0%, 100% { transform: scale(1); opacity: 1; }
            50% { transform: scale(1.4); opacity: 0.6; }
        }

        .btn-header {
            background: rgba(255, 255, 255, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: white;
            padding: 8px 14px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .btn-header:hover {
            background: rgba(255, 255, 255, 0.25);
        }

        /* ── Container Layout ── */
        .container {
            max-width: 1400px;
            margin: 24px auto;
            padding: 0 20px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        /* ── Metrics Grid ── */
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
        }

        .metric-card {
            background: var(--surface);
            padding: 18px 20px;
            border-radius: var(--radius-md);
            border: 1px solid var(--border);
            box-shadow: var(--shadow-sm);
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: transform 0.2s;
        }

        .metric-card:hover {
            transform: translateY(-2px);
        }

        .metric-label {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-muted);
            margin-bottom: 4px;
        }

        .metric-value {
            font-size: 28px;
            font-weight: 800;
            letter-spacing: -0.5px;
        }

        .metric-icon {
            width: 48px;
            height: 48px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        .card-red .metric-icon { background: var(--red-bg); color: var(--red-alert); }
        .card-amber .metric-icon { background: var(--amber-bg); color: var(--amber-alert); }
        .card-green .metric-icon { background: var(--green-bg); color: var(--green-ok); }
        .card-primary .metric-icon { background: var(--primary-light); color: var(--primary); }

        /* ── Toolbar & Filters ── */
        .toolbar {
            background: var(--surface);
            padding: 16px 20px;
            border-radius: var(--radius-md);
            border: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
        }

        .filter-group {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
        }

        .filter-btn {
            background: var(--bg);
            border: 1px solid var(--border);
            padding: 8px 14px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s;
            color: var(--text-muted);
        }

        .filter-btn.active {
            background: var(--primary);
            color: white;
            border-color: var(--primary);
        }

        .filter-btn.btn-red.active { background: var(--red-alert); border-color: var(--red-alert); }
        .filter-btn.btn-amber.active { background: var(--amber-alert); border-color: var(--amber-alert); }
        .filter-btn.btn-green.active { background: var(--green-ok); border-color: var(--green-ok); }

        .search-box {
            position: relative;
            min-width: 260px;
        }

        .search-box input {
            width: 100%;
            padding: 8px 14px 8px 36px;
            border-radius: 10px;
            border: 1px solid var(--border);
            font-size: 13px;
            outline: none;
            font-family: inherit;
        }

        .search-box input:focus {
            border-color: var(--primary);
        }

        .search-box span {
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 14px;
        }

        /* ── Emergency Alert Banner ── */
        #emergency-banner {
            display: none;
            background: linear-gradient(90deg, #DC2626 0%, #EF4444 100%);
            color: white;
            padding: 16px 20px;
            border-radius: var(--radius-md);
            box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35);
            align-items: center;
            justify-content: space-between;
            animation: pulse-alert 2s infinite;
        }

        @keyframes pulse-alert {
            0%, 100% { box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35); }
            50% { box-shadow: 0 4px 25px rgba(239, 68, 68, 0.65); }
        }

        /* ── Cases Grid ── */
        .cases-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
            gap: 16px;
        }

        .case-card {
            background: var(--surface);
            border-radius: var(--radius-lg);
            border: 1px solid var(--border);
            box-shadow: var(--shadow-sm);
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            transition: all 0.2s;
            position: relative;
            overflow: hidden;
        }

        .case-card:hover {
            box-shadow: var(--shadow-md);
            border-color: #CBD5E1;
        }

        .case-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
        }

        .case-card.risk-RED::before { background: var(--red-alert); }
        .case-card.risk-AMBER::before { background: var(--amber-alert); }
        .case-card.risk-GREEN::before { background: var(--green-ok); }

        .case-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }

        .patient-id {
            font-family: 'JetBrains Mono', monospace;
            font-size: 11px;
            font-weight: 600;
            color: var(--text-muted);
            background: var(--bg);
            padding: 2px 8px;
            border-radius: 6px;
            display: inline-block;
            margin-bottom: 4px;
        }

        .patient-name {
            font-size: 18px;
            font-weight: 700;
            color: var(--text-main);
        }

        .village-tag {
            font-size: 13px;
            color: var(--text-muted);
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .triage-badge {
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 800;
            letter-spacing: 0.3px;
            text-transform: uppercase;
        }

        .triage-badge.RED { background: var(--red-bg); color: var(--red-alert); }
        .triage-badge.AMBER { background: var(--amber-bg); color: var(--amber-alert); }
        .triage-badge.GREEN { background: var(--green-bg); color: var(--green-ok); }

        .vitals-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
            background: var(--bg);
            padding: 10px 14px;
            border-radius: 12px;
        }

        .vital-item .label {
            font-size: 11px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
        }

        .vital-item .val {
            font-size: 15px;
            font-weight: 700;
            color: var(--text-main);
        }

        .danger-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .danger-tag {
            font-size: 11px;
            font-weight: 600;
            padding: 3px 8px;
            border-radius: 6px;
            background: var(--red-bg);
            color: var(--red-alert);
        }

        .danger-tag.none {
            background: var(--green-bg);
            color: var(--green-ok);
        }

        /* ── Advisory & Dispatch status ── */
        .advisory-box {
            background: var(--coral-light);
            border-left: 3px solid var(--coral-accent);
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 12px;
        }

        .advisory-box .title {
            font-weight: 700;
            color: var(--coral-accent);
            margin-bottom: 2px;
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 8px;
            background: #E0E7FF;
            color: #4338CA;
        }

        .actions-bar {
            display: flex;
            gap: 8px;
            margin-top: 4px;
        }

        .btn-action {
            flex: 1;
            padding: 9px 12px;
            border-radius: 10px;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            border: 1px solid var(--border);
            background: var(--surface);
            color: var(--text-main);
            transition: all 0.15s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
        }

        .btn-action:hover {
            background: var(--bg);
        }

        .btn-action.primary {
            background: var(--primary);
            color: white;
            border-color: var(--primary);
        }

        .btn-action.primary:hover {
            background: var(--primary-dark);
        }

        .btn-action.ambulance {
            background: var(--red-alert);
            color: white;
            border-color: var(--red-alert);
        }

        .btn-action.ambulance:hover {
            background: #DC2626;
        }

        /* ── Modals ── */
        .modal-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.6);
            backdrop-filter: blur(4px);
            z-index: 200;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .modal-card {
            background: var(--surface);
            border-radius: var(--radius-lg);
            max-width: 540px;
            width: 100%;
            box-shadow: var(--shadow-lg);
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .modal-title {
            font-size: 18px;
            font-weight: 800;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .modal-textarea {
            width: 100%;
            height: 100px;
            padding: 12px;
            border-radius: 10px;
            border: 1px solid var(--border);
            font-family: inherit;
            font-size: 14px;
            outline: none;
            resize: none;
        }

        .modal-textarea:focus {
            border-color: var(--primary);
        }

        .modal-btns {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }

        /* ── JSON Viewer Modal ── */
        .json-pre {
            background: #0F172A;
            color: #38BDF8;
            padding: 16px;
            border-radius: 12px;
            max-height: 400px;
            overflow: auto;
            font-family: 'JetBrains Mono', monospace;
            font-size: 12px;
            line-height: 1.4;
        }

        /* ── Empty State ── */
        .empty-state {
            grid-column: 1 / -1;
            text-align: center;
            padding: 60px 20px;
            background: var(--surface);
            border-radius: var(--radius-lg);
            border: 1px dashed var(--border);
        }

        .empty-state .icon { font-size: 40px; margin-bottom: 10px; }
        .empty-state h3 { font-size: 18px; font-weight: 700; color: var(--text-main); }
        .empty-state p { font-size: 13px; color: var(--text-muted); }
    </style>
</head>
<body>

    <!-- ── Header ── -->
    <header>
        <div class="brand-area">
            <div class="brand-icon">🌸</div>
            <div class="brand-text">
                <h1>SakhiCare Care Desk <span style="font-size: 13px; background: rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 12px;">सखीकेयर</span></h1>
                <p>Maternal Health Support Desk & Emergency Response Hub</p>
            </div>
        </div>
        <div class="header-controls">
            <div class="live-badge">
                <div class="live-dot"></div>
                <span>LIVE STREAM ACTIVE</span>
            </div>
            <button class="btn-header" onclick="openBroadcastModal()">📢 Broadcast Alert</button>
            <button class="btn-header" onclick="testOneSignalPush()">⚡ Test Push API</button>
            <button class="btn-header" onclick="fetchCases()">🔄 Refresh</button>
        </div>
    </header>

    <!-- ── Main Container ── -->
    <div class="container">

        <!-- ── Emergency Alert Banner ── -->
        <div id="emergency-banner">
            <div style="display: flex; align-items: center; gap: 12px;">
                <span style="font-size: 26px;">🚨</span>
                <div>
                    <h3 style="font-size: 16px; font-weight: 800;" id="alert-title">EMERGENCY: High-Risk Maternal Case Detected</h3>
                    <p style="font-size: 13px; opacity: 0.9;" id="alert-desc">Immediate tele-advisory or emergency ambulance required.</p>
                </div>
            </div>
            <button onclick="dismissAlert()" style="background: white; color: #DC2626; border: none; padding: 6px 14px; border-radius: 8px; font-weight: 700; cursor: pointer;">Acknowledge</button>
        </div>

        <!-- ── Metrics Grid ── -->
        <div class="metrics-grid">
            <div class="metric-card card-primary">
                <div>
                    <div class="metric-label">Total Sync Cases</div>
                    <div class="metric-value" id="count-total">0</div>
                </div>
                <div class="metric-icon">📋</div>
            </div>
            <div class="metric-card card-red">
                <div>
                    <div class="metric-label">Emergency (RED)</div>
                    <div class="metric-value" id="count-red" style="color: var(--red-alert);">0</div>
                </div>
                <div class="metric-icon">🚨</div>
            </div>
            <div class="metric-card card-amber">
                <div>
                    <div class="metric-label">High Priority (AMBER)</div>
                    <div class="metric-value" id="count-amber" style="color: var(--amber-alert);">0</div>
                </div>
                <div class="metric-icon">⚠️</div>
            </div>
            <div class="metric-card card-green">
                <div>
                    <div class="metric-label">Normal (GREEN)</div>
                    <div class="metric-value" id="count-green" style="color: var(--green-ok);">0</div>
                </div>
                <div class="metric-icon">✅</div>
            </div>
        </div>

        <!-- ── Toolbar ── -->
        <div class="toolbar">
            <div class="filter-group">
                <button class="filter-btn active" onclick="setFilter('ALL', this)">All Cases</button>
                <button class="filter-btn btn-red" onclick="setFilter('RED', this)">🚨 Red Queue (<span id="btn-count-red">0</span>)</button>
                <button class="filter-btn btn-amber" onclick="setFilter('AMBER', this)">⚠️ Amber (<span id="btn-count-amber">0</span>)</button>
                <button class="filter-btn btn-green" onclick="setFilter('GREEN', this)">🟢 Green (<span id="btn-count-green">0</span>)</button>
            </div>
            <div class="search-box">
                <span>🔍</span>
                <input type="text" id="searchInput" placeholder="Search patient, ID or village..." oninput="renderCases()">
            </div>
        </div>

        <!-- ── Cases Feed ── -->
        <div class="cases-grid" id="cases-container">
            <div class="empty-state">
                <div class="icon">🌸</div>
                <h3>Listening for Frontline Synchronizations</h3>
                <p>As soon as an ASHA worker syncs from the SakhiCare mobile app, cases will stream here in real-time.</p>
            </div>
        </div>

    </div>

    <!-- ── Advisory Modal ── -->
    <div class="modal-overlay" id="advisory-modal">
        <div class="modal-card">
            <div class="modal-title">
                <span>💬 Send Clinical Support Advisory</span>
                <span style="cursor: pointer; font-size: 20px;" onclick="closeModal('advisory-modal')">&times;</span>
            </div>
            <p style="font-size: 13px; color: var(--text-muted);" id="advisory-patient-info">Patient: Rani Devi (SC-101)</p>
            
            <div style="display: flex; flex-direction: column; gap: 6px;">
                <label style="font-size: 12px; font-weight: 700; color: var(--text-main);">Clinical / Logistic Advice for Frontline Worker:</label>
                <textarea class="modal-textarea" id="advisory-text" placeholder="e.g. Administer oral labetalol 100mg stat, maintain left lateral tilt, transfer to CHC Rampur immediately."></textarea>
            </div>

            <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                <button type="button" class="filter-btn" style="font-size: 11px;" onclick="setPreset('Emergency referral: Shift to CHC Rampur immediately. Ambulance dispatched.')">🚑 Shift to CHC</button>
                <button type="button" class="filter-btn" style="font-size: 11px;" onclick="setPreset('Administer oral hydration and monitor BP every 15 minutes.')">💧 Hydration & BP Monitor</button>
                <button type="button" class="filter-btn" style="font-size: 11px;" onclick="setPreset('High Risk: Arrange 108 emergency transport. Keep patient warm and stable.')">🚨 Emergency 108</button>
            </div>

            <div class="modal-btns">
                <button class="btn-action" onclick="closeModal('advisory-modal')">Cancel</button>
                <button class="btn-action primary" onclick="submitAdvisory()">📲 Send Push to ASHA Device</button>
            </div>
        </div>
    </div>

    <!-- ── Broadcast Modal ── -->
    <div class="modal-overlay" id="broadcast-modal">
        <div class="modal-card">
            <div class="modal-title">
                <span>📢 OneSignal Broadcast Notification</span>
                <span style="cursor: pointer; font-size: 20px;" onclick="closeModal('broadcast-modal')">&times;</span>
            </div>
            <div style="display: flex; flex-direction: column; gap: 10px;">
                <div>
                    <label style="font-size: 12px; font-weight: 700;">Broadcast Title:</label>
                    <input type="text" id="bc-title" value="SakhiCare Advisory: High Maternal Surge Alert" style="width: 100%; padding: 8px; border-radius: 8px; border: 1px solid var(--border); font-family: inherit;">
                </div>
                <div>
                    <label style="font-size: 12px; font-weight: 700;">Message Content:</label>
                    <textarea class="modal-textarea" id="bc-msg" placeholder="Enter message to push to all frontline health workers..."></textarea>
                </div>
            </div>
            <div class="modal-btns">
                <button class="btn-action" onclick="closeModal('broadcast-modal')">Cancel</button>
                <button class="btn-action primary" onclick="submitBroadcast()">🚀 Broadcast Now via API</button>
            </div>
        </div>
    </div>

    <!-- ── FHIR Modal ── -->
    <div class="modal-overlay" id="fhir-modal">
        <div class="modal-card" style="max-width: 720px;">
            <div class="modal-title">
                <span>📋 HL7 FHIR R4 Clinical Bundle</span>
                <span style="cursor: pointer; font-size: 20px;" onclick="closeModal('fhir-modal')">&times;</span>
            </div>
            <pre class="json-pre" id="fhir-json">Loading FHIR bundle...</pre>
            <div class="modal-btns">
                <button class="btn-action" onclick="copyFhirJson()">📋 Copy JSON</button>
                <button class="btn-action primary" onclick="closeModal('fhir-modal')">Close</button>
            </div>
        </div>
    </div>

    <!-- ── Audio Chime for Emergency Alerts ── -->
    <audio id="alert-sound" src="data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbqWE1MU6e4/S5cDYyT5fn9blxNjNPl+f1uXE2M0+X5/W5cTYzT5fn9blxNjNPl+f1uXE2M0+X5/W5cTY="></audio>

    <script>
        let allCases = [];
        let currentFilter = 'ALL';
        let activePatientId = null;

        // ── Fetch Cases on Load ──
        async function fetchCases() {
            try {
                const res = await fetch('/cases');
                const data = await res.json();
                allCases = data.cases || [];
                updateMetrics();
                renderCases();
            } catch (err) {
                console.error("Failed to load cases:", err);
            }
        }

        // ── Server-Sent Events (SSE) Live Stream ──
        function initLiveStream() {
            const eventSource = new EventSource('/api/v1/live-stream');
            
            eventSource.onmessage = (e) => {
                try {
                    const eventData = JSON.parse(e.data);
                    if (eventData.type === 'NEW_CASE') {
                        const newCase = eventData.case;
                        // Replace or prepend
                        const idx = allCases.findIndex(c => c.patient_id === newCase.patient_id);
                        if (idx >= 0) {
                            allCases[idx] = newCase;
                        } else {
                            allCases.unshift(newCase);
                        }
                        updateMetrics();
                        renderCases();

                        if (newCase.risk_level === 'RED') {
                            triggerEmergencyAlert(newCase);
                        }
                    } else if (eventData.type === 'CASE_UPDATED') {
                        const updated = eventData.case;
                        const idx = allCases.findIndex(c => c.patient_id === updated.patient_id);
                        if (idx >= 0) {
                            allCases[idx] = updated;
                            renderCases();
                        }
                    }
                } catch (err) {
                    console.log("SSE parsing message:", err);
                }
            };

            eventSource.onerror = () => {
                console.log("SSE reconnecting in 5s...");
                eventSource.close();
                setTimeout(initLiveStream, 5000);
            };
        }

        function triggerEmergencyAlert(c) {
            const banner = document.getElementById('emergency-banner');
            const title = document.getElementById('alert-title');
            const desc = document.getElementById('alert-desc');
            
            title.innerText = `🚨 EMERGENCY: High-Risk Case (${c.patient_id}) in ${c.village}`;
            desc.innerText = `Patient ${c.patient_name} | BP: ${c.blood_pressure} | Immediate response required!`;
            banner.style.display = 'flex';

            try {
                document.getElementById('alert-sound').play();
            } catch(e) {}
        }

        function dismissAlert() {
            document.getElementById('emergency-banner').style.display = 'none';
        }

        // ── Metrics Calculation ──
        function updateMetrics() {
            const total = allCases.length;
            const red = allCases.filter(c => c.risk_level === 'RED').length;
            const amber = allCases.filter(c => c.risk_level === 'AMBER').length;
            const green = allCases.filter(c => c.risk_level === 'GREEN').length;

            document.getElementById('count-total').innerText = total;
            document.getElementById('count-red').innerText = red;
            document.getElementById('count-amber').innerText = amber;
            document.getElementById('count-green').innerText = green;

            document.getElementById('btn-count-red').innerText = red;
            document.getElementById('btn-count-amber').innerText = amber;
            document.getElementById('btn-count-green').innerText = green;
        }

        // ── Filter & Search ──
        function setFilter(filter, btn) {
            currentFilter = filter;
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderCases();
        }

        function renderCases() {
            const container = document.getElementById('cases-container');
            const search = (document.getElementById('searchInput').value || '').toLowerCase().trim();

            let filtered = allCases.filter(c => {
                if (currentFilter !== 'ALL' && c.risk_level !== currentFilter) return false;
                if (search) {
                    const matchName = (c.patient_name || '').toLowerCase().includes(search);
                    const matchId = (c.patient_id || '').toLowerCase().includes(search);
                    const matchVillage = (c.village || '').toLowerCase().includes(search);
                    if (!matchName && !matchId && !matchVillage) return false;
                }
                return true;
            });

            if (filtered.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <div class="icon">🔍</div>
                        <h3>No matching cases found</h3>
                        <p>Try clearing filters or search query.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = filtered.map(c => {
                const dangerSigns = c.danger_signs || {};
                const activeSigns = Object.keys(dangerSigns).filter(k => dangerSigns[k]);
                
                const dangerHtml = activeSigns.length > 0
                    ? activeSigns.map(s => `<span class="danger-tag">⚠️ ${s.replace('_', ' ').toUpperCase()}</span>`).join('')
                    : `<span class="danger-tag none">✓ No Danger Signs</span>`;

                const advisoryHtml = c.doctor_advisory 
                    ? `<div class="advisory-box"><div class="title">💬 Care Desk Guidance:</div><div>"${c.doctor_advisory}"</div></div>`
                    : '';

                const dispatchHtml = c.ambulance_status 
                    ? `<div class="status-pill">🚑 ${c.ambulance_status}</div>`
                    : '';

                return `
                    <div class="case-card risk-${c.risk_level}">
                        <div class="case-header">
                            <div>
                                <div class="patient-id">${c.patient_id}</div>
                                <div class="patient-name">${c.patient_name}</div>
                                <div class="village-tag">📍 Village: ${c.village}</div>
                            </div>
                            <span class="triage-badge ${c.risk_level}">${c.risk_level} RISK</span>
                        </div>

                        <div class="vitals-row">
                            <div class="vital-item">
                                <div class="label">Blood Pressure</div>
                                <div class="val">${c.blood_pressure}</div>
                            </div>
                            <div class="vital-item">
                                <div class="label">Haemoglobin</div>
                                <div class="val">${c.haemoglobin} g/dL</div>
                            </div>
                        </div>

                        <div class="danger-tags">
                            ${dangerHtml}
                        </div>

                        ${advisoryHtml}
                        ${dispatchHtml}

                        <div class="actions-bar">
                            <button class="btn-action primary" onclick="openAdvisoryModal('${c.patient_id}', '${c.patient_name}')">💬 Advisory</button>
                            <button class="btn-action ambulance" onclick="dispatchAmbulance('${c.patient_id}')">🚑 108 Dispatch</button>
                            <button class="btn-action" onclick="viewFhirBundle('${c.patient_id}')">📋 FHIR</button>
                        </div>
                    </div>
                `;
            }).join('');
        }

        // ── Advisory Modal Logic ──
        function openAdvisoryModal(patientId, patientName) {
            activePatientId = patientId;
            document.getElementById('advisory-patient-info').innerText = `Patient: ${patientName} (${patientId})`;
            document.getElementById('advisory-text').value = '';
            document.getElementById('advisory-modal').style.display = 'flex';
        }

        function setPreset(text) {
            document.getElementById('advisory-text').value = text;
        }

        async function submitAdvisory() {
            const text = document.getElementById('advisory-text').value.trim();
            if (!text) {
                alert("Please enter clinical or logistic advice.");
                return;
            }

            try {
                const res = await fetch(`/api/v1/cases/${activePatientId}/advisory`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        advisory_text: text,
                        sender: "Care Desk Lead (Dr. Sharma)"
                    })
                });
                const data = await res.json();
                closeModal('advisory-modal');
                alert(`✅ Guidance pushed to ASHA worker device via OneSignal!\nNotification ID: ${data.notification_id}`);
                fetchCases();
            } catch (err) {
                alert("Failed to submit advisory: " + err);
            }
        }

        // ── Ambulance Dispatch ──
        async function dispatchAmbulance(patientId) {
            if (!confirm(`Confirm 108 Emergency Ambulance dispatch for case ${patientId}?`)) return;
            try {
                const res = await fetch(`/api/v1/cases/${patientId}/dispatch`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ vehicle_id: "108-AMB-Rampur-04" })
                });
                const data = await res.json();
                alert(`🚑 Ambulance 108 Dispatched!\nStatus: ${data.ambulance_status}`);
                fetchCases();
            } catch(err) {
                alert("Dispatch failed: " + err);
            }
        }

        // ── Broadcast Modal ──
        function openBroadcastModal() {
            document.getElementById('broadcast-modal').style.display = 'flex';
        }

        async function submitBroadcast() {
            const title = document.getElementById('bc-title').value.trim();
            const msg = document.getElementById('bc-msg').value.trim();
            if (!msg) {
                alert("Please enter message content.");
                return;
            }

            try {
                const res = await fetch('/api/v1/notifications/broadcast', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ title: title, message: msg, segment: "All" })
                });
                const data = await res.json();
                closeModal('broadcast-modal');
                alert(`📢 Broadcast sent to all active devices via OneSignal API!\nID: ${data.id}`);
            } catch(err) {
                alert("Broadcast failed: " + err);
            }
        }

        // ── Quick Push API Test ──
        async function testOneSignalPush() {
            try {
                const res = await fetch('/api/v1/notifications/send-emergency-alert', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        patient_id: "SC-TEST-999",
                        patient_name: "Kavita Devi",
                        village: "Rampur PHC",
                        blood_pressure: "160/105",
                        danger_signs: { bleeding: true, fever: true, headache: true, reduced_fetal_movement: false },
                        risk_level: "RED"
                    })
                });
                const data = await res.json();
                alert(`⚡ OneSignal REST API Push Dispatched!\nDelivery: ${data.delivery}\nNotification ID: ${data.id}\nCheck server logs to inspect live payload.`);
            } catch(err) {
                alert("API test failed: " + err);
            }
        }

        // ── FHIR Modal ──
        async function viewFhirBundle(patientId) {
            document.getElementById('fhir-modal').style.display = 'flex';
            document.getElementById('fhir-json').innerText = "Loading FHIR R4 Resource...";
            try {
                const res = await fetch(`/fhir/export/${patientId}`);
                const data = await res.json();
                document.getElementById('fhir-json').innerText = JSON.stringify(data, null, 2);
            } catch(err) {
                document.getElementById('fhir-json').innerText = "Failed to load FHIR bundle: " + err;
            }
        }

        function copyFhirJson() {
            const text = document.getElementById('fhir-json').innerText;
            navigator.clipboard.writeText(text);
            alert("FHIR R4 JSON copied to clipboard!");
        }

        function closeModal(id) {
            document.getElementById(id).style.display = 'none';
        }

        // Initialize on page load
        fetchCases();
        initLiveStream();
    </script>
</body>
</html>
"""
