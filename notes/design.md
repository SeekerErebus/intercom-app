# HomePing — Design Document

**Status:** Draft for review  
**Date:** 2026-07-18  
**App name:** HomePing  
**Platform:** Android (sideload APK)  
**v1 goal:** Reliable same-Wi‑Fi attention pings between two household devices, with elder-friendly UX.

---

## 1. Problem & goals

### Problem
Mom habitually **calls** her child’s phone to get attention between floors (walking downstairs is hard for her). From the child’s side, every ringing phone is ambiguous: in-house “I need you,” or she’s out and needs a real call? HomePing separates those signals on purpose:

- **HomePing** = home, same Wi‑Fi, attention between floors  
- **Phone call** = away, or needs an actual conversation  

v1 is a single large **Ping** (with Coming/Dismissed), not a voice intercom — that is enough to replace the ambiguous phone-call-as-doorbell habit.

### Goals (v1)
1. Two devices on the same LAN can find each other after a one-time PIN setup.
2. Either device can send a **ping**; the other plays a strong customizable sound and shows a high-priority notification.
3. Receiver can reply **Coming** or **Dismiss**; sender sees that outcome (or timeout).
4. UI is large-target, plain-language, minimal steps.
5. Works without internet or accounts.
6. Usable as **normal pocket phones** (not only wall-powered stations)—within honest Android limits.

### Non-goals (v1)
- Voice / video (Phase 2)
- More than two active household peers (architecture may allow more later; UX is two-device)
- Cloud relay, accounts, Play Store listing
- Cross-network / cellular fallback
- Message history

### Success metrics (informal)
- Ping → audible/visible alert on peer in ~1–2s when both apps are “ready” on same Wi‑Fi.
- Mom can send a ping without help after setup.
- Family installer can complete setup on both devices in under ~15 minutes.

---

## 2. Users & scenarios

| Role | Description |
|------|-------------|
| **Senior daily user** | Sends/receives pings; rarely opens settings |
| **Family installer** | Installs APK, grants permissions, sets names + PIN, tests both directions, tunes battery settings |

### Primary scenarios
1. **Attention request:** Upstairs taps Ping → downstairs phone notifies loudly → downstairs taps Coming → upstairs sees “Coming!”
2. **False alarm / not now:** Receiver taps Dismiss → sender sees “Dismissed”.
3. **Nobody available:** Peer offline or no response within timeout → sender sees clear failure state.
4. **First-time setup:** Installer sets name + shared PIN on both phones; devices pair; test ping.

---

## 3. Product decisions (locked)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Device count (v1 UX) | Exactly 2 | Giant single Ping button; no target list |
| v1 feature set | Ping + ack only | Ship reliability first; voice later |
| Trust | Shared home PIN | Blocks casual guest-LAN abuse; simple |
| Voice accept (Phase 2) | Manual Accept | Safer open-mic policy |
| Distribution | Sideload APK | Private household; faster iteration |
| Incoming alert | High-priority notification + strong sound | Avoid full-screen intent permission complexity; revisit if miss rate high |
| Power model | Pocket phones | Harder reliability problem; drives FGS + Doze design |
| A11y | General elder-friendly | Large targets, contrast, plain language |
| Name | HomePing | Clear household attention product |

---

## 4. User experience

### 4.1 Information architecture
```
First run → Setup (name, PIN, permissions)
Main      → Peer status + Ping + last result
Incoming  → Notification actions: Coming | Dismiss
Settings  → Name, PIN, sounds, battery help, about
```

### 4.2 Main screen (daily)
- App title: **HomePing**
- **Peer card:** friendly name, Online / Offline / Reachable?
- **Primary button:** `Ping [PeerName]` (very large)
- After send: status line — *Ringing…* / *They said Coming!* / *Dismissed* / *No response* / *Not available*
- Secondary: **Settings** (gear or text button, not competing with Ping)
- Persistent system notification while service is on: *HomePing is ready* (low importance channel)

### 4.3 Incoming ping
- **High-priority** notification channel (`ping_alerts`)
- Title: `Ping from [Name]`
- Body: short instruction
- Actions: **Coming** · **Dismiss**
- Custom sound (user-selected), vibration pattern allowed
- Tapping notification opens in-app alert screen with the same two giant buttons (notification actions alone are small on some OEMs)

### 4.4 Setup wizard (once)
1. Welcome + “same Wi‑Fi required”
2. Permissions: Notifications (required), maybe exact alarm if used; mic **not** required in v1
3. This device’s name (preset suggestions: Upstairs, Downstairs, Kitchen, … + custom)
4. Home PIN (4–6 digits) + confirm
5. “Looking for the other phone…” with troubleshooting tips
6. When peer found and PIN matches: success + **Send test ping**
7. Battery optimization deep-link / checklist (critical for pocket reliability)

### 4.5 Settings
- Device name
- Home PIN (change; must re-sync understanding on both devices)
- Alert sound picker + **Play test**
- Peer info (name, last seen)
- “Improve reliability” (battery unrestricted, vendor tips)
- About / version

### 4.6 Visual principles
- Large primary controls (~96dp height)
- High contrast; support font scale 1.0–2.0 without clipping critical actions
- Plain language (see `04-ux-notes.md`)
- Color + text for Online/Offline (not color alone)

---

## 5. System architecture

### 5.1 High-level
```
┌──────────────────────────────────────────┐
│ Compose UI                               │
│ Main · Setup · Settings · Alert screen   │
└──────────────────┬───────────────────────┘
                   │ ViewModels
┌──────────────────▼───────────────────────┐
│ Domain                                   │
│ Pairing · Presence · PingSession · Prefs │
└──────┬─────────────┬─────────────────────┘
       │             │
┌──────▼─────┐ ┌─────▼──────┐ ┌────────────────┐
│ Discovery  │ │ Transport  │ │ Alert / Sound  │
│ NSD+beacon │ │ TLS-ish or │ │ Notifications  │
│            │ │ PIN-auth   │ │                │
│            │ │ TCP JSON   │ │                │
└────────────┘ └────────────┘ └────────────────┘
         ▲              ▲
         │   Foreground service keeps process + listener alive
```

### 5.2 Process model
- **`HomePingService` (foreground service)** runs whenever the user has HomePing “enabled” (default on after setup).
  - Low-importance ongoing notification: service identity.
  - Owns discovery advertisements, inbound listen socket, and outbound reconnect to peer.
- UI is a thin client of service state (bound service or shared repository via singleton/DI).

### 5.3 Two-device pairing model
- Each device generates a stable **`deviceId`** (UUID, persisted).
- User sets **`displayName`** and shared **`homePin`**.
- Devices discover via LAN; authenticate with PIN-derived proof (see Security).
- Once authenticated, each remembers the peer’s `deviceId` + last display name as **paired peer**.
- v1 UI assumes a single paired peer. If multiple PIN-matching peers appear, prefer the stored `deviceId`; settings can “forget peer / re-pair”.

### 5.4 Connectivity strategy (pocket-phone hard mode)

Android will kill idle apps and restrict networking in Doze. HomePing mitigates without cloud push:

| Mechanism | Role |
|-----------|------|
| Foreground service | Keeps process eligible to run and listen |
| Persistent TCP to peer (when both online) | Low-latency ping path; detect disconnect quickly |
| NSD (mDNS) advertise + resolve | Find peer after IP change / restart |
| UDP beacon (optional fallback) | Presence pulse if NSD flaky on OEM |
| Exponential reconnect | Survive Wi‑Fi blips |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prompt once; explain why |
| Documentation | OEM-specific battery whitelist tips |

**Honest limitation:** Some OEMs (aggressive Chinese skins, etc.) may still delay or kill background work. v1 documents this; reliability checklist is part of setup. We do **not** add cloud push in v1.

### 5.5 Module layout (single Android app module)
```
app/
  ui/          # Compose screens
  service/     # HomePingService
  discovery/   # NSD + beacon
  net/         # framing, messages, session
  ping/        # ping state machine
  data/        # DataStore prefs
  alert/       # notification channels, sounds
```

---

## 6. Protocol (v1)

### 6.1 Transport
- **TCP**, length-prefixed JSON messages (4-byte big-endian length + UTF-8 JSON).
- Port: fixed default e.g. **7529** (document; allow override only in debug).
- Optional later: upgrade framing; keep message `type` field stable.

### 6.2 Discovery
- mDNS/NSD service type: **`_homeping._tcp`**
- TXT records (or equivalent attributes): `deviceId`, `name` (display), `ver` (protocol version)
- Do **not** put PIN in TXT.

### 6.3 Message types (v1)

| type | direction | purpose |
|------|-----------|---------|
| `hello` | either | Identify `deviceId`, `displayName`, `protocolVersion` |
| `auth` | client→server / mutual | Prove knowledge of home PIN |
| `auth_ok` / `auth_fail` | response | Pairing result |
| `ping` | A→B | Attention request (`pingId`, timestamp) |
| `ping_delivered` | B→A | Optional immediate accept-of-delivery |
| `ping_ack` | B→A | `Coming` or `Dismissed` (`pingId`, `response`) |
| `ping_cancel` | A→B | Sender cancels while ringing |
| `presence` | either | Optional heartbeat on TCP |

Phase 2 stubs (defined, unused in v1 UI): `call_offer`, `call_accept`, `call_reject`, `call_end` — reserved names so we do not paint into a corner.

### 6.4 Ping state machine

**Sender**
```
Idle → Sending → Ringing → { Coming | Dismissed | Timeout | Failed | Cancelled } → Idle
```

**Receiver**
```
Idle → Alerting → { Coming | Dismissed | Timeout | Cancelled } → Idle
```

- Default **timeout:** 60 seconds (configurable later).
- Only one active ping session per device pair in v1 (new ping supersedes or is rejected—**prefer supersede** with new `pingId` and cancel previous alert).
- Acks are idempotent per `pingId`.

### 6.5 Auth (shared PIN)
- Both devices hold the same PIN.
- On TCP connect, mutual auth:
  - Exchange nonces in `hello`.
  - `auth` sends `SHA-256(PIN || nonce_peer || nonce_self || deviceId)` (or HMAC with PIN as key)—exact construction specified in implementation notes; goal is **not** to send PIN in cleartext on the LAN.
- Failed auth → disconnect; do not show up as paired.
- This is **household deterrent**, not high-assurance security against a determined LAN attacker.

---

## 7. Notifications & sounds

### Channels
| Channel ID | Importance | Use |
|------------|------------|-----|
| `service` | Low | Ongoing “HomePing is ready” |
| `ping_alerts` | **High** | Incoming ping |

### Sounds
- Ship 4–6 built-in alert tones optimized to be noticeable (not only soft UI beeps).
- User selects default in Settings; **Play test** uses the alert channel.
- v1 optional: import via `ACTION_GET_CONTENT` audio/* if low cost; otherwise built-in only for first cut.
- Respect: we set a strong default sound on the **ping_alerts** channel; user can still lower system volume (document that volume must be audible).

### Vibration
- Distinct pattern on incoming ping when vibration enabled at system level.

---

## 8. Data persistence

**DataStore preferences**
- `deviceId`
- `displayName`
- `homePinHash` or encrypted PIN storage (PIN needed for auth computation—store carefully; Android Keystore-backed if practical)
- `pairedPeerId`, `pairedPeerName`
- `alertSoundId`
- `setupComplete`
- `batteryPromptShown`

No room database required for v1.

---

## 9. Permissions (v1)

| Permission | Why |
|------------|-----|
| `INTERNET` / `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | LAN comms |
| `CHANGE_WIFI_MULTICAST_STATE` | mDNS |
| `POST_NOTIFICATIONS` (API 33+) | Alerts |
| `FOREGROUND_SERVICE` + appropriate FGS type(s) | Background readiness |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Pocket reliability (optional grant) |
| `VIBRATE` | Haptics |

**Not required in v1:** `RECORD_AUDIO`, full-screen intent, contacts, location (avoid location if possible even for Wi‑Fi SSID).

---

## 10. Reliability & failure UX

| Failure | User-visible behavior |
|---------|----------------------|
| Peer offline | Ping disabled or tap shows “Not available — is their Wi‑Fi on?” |
| Send fails mid-flight | “Couldn’t reach [Name]. Retry?” |
| Timeout | “No response” |
| Not on Wi‑Fi | Banner: “Connect to home Wi‑Fi” |
| Auth fail | “Wrong home PIN on one of the phones” |
| Service killed | Notification disappears; on next open, restart service + explain battery settings |

Logging: debug logcat tags; optional in-app “last error” for installer troubleshooting (not a full log UI in v1).

---

## 11. Phase 2 (voice) — sketch only

- Reuse TCP signaling; add WebRTC audio-only.
- Call UI with Accept (required) / Reject / Hang up.
- Request mic permission at Phase 2 first use.
- PTT vs full-duplex still open; product previously leaned toward future Accept-gated calls.
- **v1 must not block this:** keep `deviceId`, connection manager, and message router extensible.

---

## 12. Tech stack

| Item | Choice |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (customized for large targets) |
| Min SDK | **26** (Android 8.0) |
| Target SDK | Current stable (e.g. 35) at implementation time |
| Async | Coroutines + Flow |
| DI | Manual or lightweight (Hilt optional; prefer simple for small app) |
| Persistence | DataStore |
| Discovery | Android NSD (`NsdManager`) |
| Build | Gradle Kotlin DSL, single `app` module |
| Tests | Unit tests for ping state machine + auth; instrumentation smoke later |

---

## 13. Security & privacy

- No accounts, no analytics cloud, no third-party trackers in v1.
- Traffic stays on LAN.
- PIN not advertised in mDNS.
- Mic not used in v1.
- Sideload: user trusts the APK source (family builds/releases).
- Threat model: curious guest on Wi‑Fi, not nation-state.

---

## 14. Testing plan

1. **Unit:** ping state machine transitions; message parse/serialize; auth vector tests.
2. **Two-emulator / two-device:** discovery, pair, ping, ack, timeout, supersede.
3. **Wi‑Fi blip:** disable/re-enable Wi‑Fi; ensure re-discover.
4. **Background:** screen off, app not foreground; send ping from peer; verify notification + sound.
5. **Battery:** leave idle 30+ minutes; verify still reachable (document OEM variance).
6. **Elder UX dogfood:** family member completes setup from checklist alone.

---

## 15. Documentation deliverables (repo)

- `README.md` — what it is, build, install APK, setup checklist
- `notes/` — planning history (this design lives here)
- In-app battery/reliability help screen

---

## 16. Key Decisions

1. **Ping-only v1** — Delivers the core “get attention” job without WebRTC complexity; voice is Phase 2.
2. **Two-device UX** — Matches the two-floor household; one giant Ping control.
3. **Shared home PIN** — Minimal trust boundary for LAN peers without accounts.
4. **High-priority notification (not full-screen intent)** — Strong alert with fewer special permissions; can escalate later if needed.
5. **Foreground service + persistent peer connection** — Required for pocket-phone receive path without cloud push.
6. **Kotlin + Compose, min API 26** — Modern Android baseline with notification channels and FGS.
7. **Sideload distribution** — Fits private family deployment and rapid iteration.
8. **Protocol reserved for calls** — Avoid rewrite when voice lands.
9. **Ack model (Coming / Dismissed)** — Reduces repeat pings and sender anxiety.
10. **Honest reliability limits** — Setup teaches battery exceptions; no false promise of SMS-grade delivery.

---

## 17. Open Questions (remaining)

Defaults below apply unless overridden:

| Topic | Default |
|-------|---------|
| Min SDK | API 26 |
| Sound import | Built-in first; import if cheap |
| Language | English |
| Ping timeout | 60s |
| PIN | 4–6 digits |
| Talk button in v1 | Hidden (no Phase 2 tease) |
| Multiple pings | Newest supersedes |

---

## 18. PR Plan

Incremental, each PR reviewable and demoable where possible.

### PR1 — Project skeleton
- **Title:** Bootstrap Android app (HomePing)
- **Affects:** Gradle project, `app` module, Compose hello main, app id, basic theme (large typography)
- **Deps:** none
- **Desc:** Empty runnable app named HomePing; CI-less local build instructions in README.

### PR2 — Preferences & setup shell
- **Title:** Device identity, DataStore, setup wizard UI
- **Affects:** `data/`, setup Compose screens, nav
- **Deps:** PR1
- **Desc:** Persist deviceId, displayName, PIN, setupComplete; wizard without networking.

### PR3 — Foreground service & notification channels
- **Title:** HomePing foreground service + channels
- **Affects:** `service/`, `alert/`, manifest permissions
- **Deps:** PR1
- **Desc:** Start/stop FGS, low-importance ongoing notification, high-importance ping channel scaffolding.

### PR4 — Discovery (NSD)
- **Title:** mDNS advertise & resolve peers
- **Affects:** `discovery/`, service integration
- **Deps:** PR2, PR3
- **Desc:** Advertise `_homeping._tcp` with deviceId/name; list discovered peers in debug/settings.

### PR5 — Transport & auth
- **Title:** TCP JSON protocol + PIN auth
- **Affects:** `net/`, pairing logic
- **Deps:** PR4
- **Desc:** Listen/connect, hello/auth, paired peer memory; reject bad PIN.

### PR6 — Ping session + UI
- **Title:** End-to-end ping with Coming/Dismissed
- **Affects:** `ping/`, main UI, notification actions, sounds
- **Deps:** PR5, PR3
- **Desc:** Send ping, alert peer, ack paths, timeout, main screen states; built-in sounds.

### PR7 — Reliability polish
- **Title:** Reconnect, battery guidance, offline UX
- **Affects:** service reconnect, settings help, banners
- **Deps:** PR6
- **Desc:** Auto-reconnect, ignore-battery prompt, clear offline/error copy, supersede behavior.

### PR8 — Hardening & packaging
- **Title:** State machine tests, release APK notes
- **Affects:** tests, README install guide, versioning
- **Deps:** PR6–7
- **Desc:** Unit tests; signed/debug APK instructions for family sideload; protocol version field locked.

### Future (not v1 PRs)
- **PRx — Voice:** WebRTC + Accept UI + mic permission (Phase 2).

---

## 19. Implementation order summary

```
PR1 Skeleton
 ├── PR2 Setup/prefs
 └── PR3 FGS/notifications
        └── PR4 Discovery
               └── PR5 Transport/auth
                      └── PR6 Ping E2E
                             └── PR7 Reliability
                                    └── PR8 Tests/packaging
```

---

## 20. Risks

| Risk | Mitigation |
|------|------------|
| OEM kills FGS / blocks LAN in Doze | Battery checklist; persistent notification; reconnect; document limits |
| mDNS broken on some routers | TCP last-known IP retry; optional UDP beacon; re-scan on network callback |
| User volume muted | Setup test sound; in-app reminder when sending if we can detect ringer mode |
| PIN desync after change | Settings copy: set same PIN on both; re-pair flow |
| Scope creep into voice | Hard v1 boundary in this doc; reserved message types only |

---

*End of design doc. Source of truth for implementation until revised.*
