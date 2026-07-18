# Technical Options

## Recommended high-level stack (tentative)

| Layer | Recommendation | Rationale |
|-------|----------------|-----------|
| App | **Kotlin + Jetpack Compose** | Modern Android default, good a11y hooks |
| Min SDK | **26 or 29** (TBD with devices) | Foreground services, notification channels |
| Discovery | **NSD / mDNS** (`_intercom._tcp`) | Standard LAN discovery; optional UDP beacon fallback |
| Control plane | **TCP + simple JSON messages** | Ping, ack, call offer/answer, presence |
| Media | **WebRTC** (or raw Opus/UDP if we want thinner) | Audio, AEC, AGC; well-trodden on Android |
| Signaling | **Custom over LAN TCP/WebSocket** | No cloud signaling server |
| Background | **Foreground service** + high-priority FGS type | Survive background; show persistent “Intercom on” notification |
| Local store | **DataStore** | Name, sound prefs, paired peers |
| DI / arch | **Simple: single-module MVVM** | Small app; avoid over-engineering |
| Build | **Gradle Kotlin DSL**, AGP current stable | — |

## Architecture sketch

```
┌─────────────────────────────────────────────────────────┐
│  UI (Compose)                                           │
│  Home · Device buttons · Incoming alert · Settings      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  App services                                           │
│  Presence · Session · Alert · Preferences               │
└───────┬─────────────────────┬───────────────────────────┘
        │                     │
┌───────▼────────┐   ┌────────▼────────┐   ┌──────────────┐
│ Discovery      │   │ Signaling       │   │ Media        │
│ NSD / mDNS     │   │ TCP JSON        │   │ WebRTC audio │
│ + optional UDP │   │ ping/ack/call   │   │              │
└────────────────┘   └─────────────────┘   └──────────────┘
```

## Discovery options

| Approach | Pros | Cons |
|----------|------|------|
| **mDNS / NSD** | Standard, works offline, no central server | Some OEMs flaky; needs retry; not great across AP isolation |
| **UDP broadcast/multicast beacon** | Simple presence pulse | Multicast often broken on consumer routers; battery if naive |
| **Manual IP entry** | Always works | Terrible UX for elderly setup |
| **QR pairing + stored host** | Reliable once set | Setup friction; IPs change with DHCP |

**Likely approach:** mDNS primary + periodic UDP beacon fallback + remember last-known peers by stable device ID (not IP).

## Voice options

| Approach | Pros | Cons |
|----------|------|------|
| **WebRTC (audio only)** | Echo cancellation, jitter buffer, mature | Heavier dependency; signaling to implement |
| **Raw PCM/Opus over UDP** | Small, controllable | We reinvent AEC/jitter; quality risk on speakerphone |
| **OS telecom / ConnectionService** | Integrates with phone UI | Overkill; not really a phone call |
| **Third-party rooms (LiveKit etc.)** | Fast media | Cloud dependency — **reject for v1** |

**Likely approach:** WebRTC audio-only with LAN signaling. For v1 push-to-talk, AEC is less critical but still helpful on speaker.

## Ping delivery options

| Approach | Pros | Cons |
|----------|------|------|
| Direct TCP to peer | Simple, ackable | Peer must be reachable; FGS must be listening |
| UDP “ring” packet | Fast | Lossy; need retries + TCP follow-up |
| Local notification only | Easy | Won’t wake if process dead |

**Likely approach:** TCP (or persistent connection) for reliable ping + full-screen intent / high-priority notification. Keep process alive via FGS while “Intercom enabled”.

## Security options

| Approach | Pros | Cons |
|----------|------|------|
| Trust LAN | Zero setup | Guest network / malicious app risk low but real |
| Shared PIN in every message | Simple household lock | PIN shoulder-surfing; not strong crypto |
| Pairing + device allowlist | Clear trust model | Setup steps for family installer |
| mTLS between peers | Strong | Heavy for family app |

**Likely approach:** Pairing code or shared home PIN + device allowlist. Encrypt optional later.

## Android-specific gotchas (must design for)

1. **Background execution limits** — need foreground service with correct type (`microphone` when in call, `connectedDevice` / data-sync style when idle listening — verify current Play policy FGS types).
2. **Notification channels** — separate channels: presence service (low), ping alert (max), call (max).
3. **Full-screen intents** — needed for lock-screen takeover; restricted on newer Android (permission).
4. **OEM battery savers** — document “disable battery optimization” for this app on Mom’s phone.
5. **Wi‑Fi AP/client isolation** — some mesh/guest networks block device-to-device; document “use main LAN, not guest”.
6. **Microphone permission** — runtime; deny → ping-only mode.
7. **Speakerphone default** — intercom expects loudspeaker, not earpiece.

## Project structure (proposed)

```
intercom-app/
  notes/                 # planning & design notes
  app/                   # Android application module
  README.md
```

Single-module until proven need for `core`, `media`, etc.

## Alternatives considered and rejected (for now)

- **Flutter / React Native** — fine, but native Kotlin is better for FGS, audio session, and OEM edge cases.
- **MQTT broker on a Pi** — extra hardware to maintain; fails if Pi is down.
- **Cloud relay “for reliability”** — opposite of offline-home goal; privacy and complexity.
- **SMS fallback** — different product; out of scope v1.
