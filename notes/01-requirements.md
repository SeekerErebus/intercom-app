# Requirements (draft)

Living document. Items marked **[?]** need confirmation.

## Functional

### Discovery & identity
- F1. Devices on the same Wi‑Fi running the app discover each other automatically.
- F2. Each device has a **friendly name** shown to others (e.g. “Upstairs”, “Kitchen tablet”).
- F3. Friendly name is configurable on-device **[?]** and persists across restarts.
- F4. Optionally group/label devices (floor, room) **[?]**.

### Ping
- F5. User can send a **ping** to the single paired peer (two-device household).
- F6. ~~Ping all~~ — not needed for v1 two-device UX.
- F7. Receiving device plays a **strong, configurable sound**, high-priority notification (+ in-app alert screen), vibrates if system allows.
- F8. Receiver can **Coming** or **Dismiss**.
- F9. Sender sees **Coming / Dismissed / timeout / failed**.
- F10. Ping works with screen off and app backgrounded **as a pocket phone** (FGS + battery guidance; OEM limits documented).

### Voice channel
- **Deferred to Phase 2** (not v1). Protocol message names reserved in `design.md`.
- F11–F15. Future: open voice channel with **manual Accept**; LAN-only media; large hang-up control.

### Sounds
- F16. Built-in set of alert sounds suitable for hard-of-hearing users (distinct, not cute chimes only).
- F17. User can pick different sounds for ping vs voice request **[?]**.
- F18. Optional custom sound import (file picker) **[?]**.
- F19. Volume behavior: respect system volume vs app “always loud” override **[?]** (safety vs courtesy).

### Settings (simple)
- F20. Set device name.
- F21. Choose alert sound(s).
- F22. Test sound button.
- F23. Optional: “Do not disturb until…” **[?]** — may be dangerous for this use case; default off / hidden.

## Non-functional

### Reliability
- NF1. Core path works with **no internet** (router only / airplane mode + Wi‑Fi).
- NF2. Reconnect after Wi‑Fi blip without reinstall or re-pairing ceremony.
- NF3. Graceful degradation: if voice fails, ping still works.

### Latency / feel
- NF4. Ping: perceptible alert on receiver within ~1–2s under normal home Wi‑Fi.
- NF5. Voice: conversational latency (target <300ms one-way if feasible on LAN).

### Accessibility / elder UX
- NF6. Minimum touch targets large (aim ≥72–96dp for primary actions).
- NF7. High contrast; avoid pure gray-on-gray; support system font scale.
- NF8. Minimal navigation depth (home screen ≈ entire daily UX).
- NF9. Plain language labels (“Call Kitchen”, not “Initiate peer session”).
- NF10. Optional spoken confirmation of actions **[?]**.

### Privacy / security
- NF11. No accounts required for v1.
- NF12. Traffic stays on local network.
- NF13. Basic protection against random LAN devices joining **[?]** (shared home PIN / pairing code).
- NF14. Mic only active during an accepted voice session (or explicit PTT).

### Platform
- NF15. Android primary. Min SDK **[?]** — suggest API 26+ (Android 8) for notification channels / foreground services, or API 29+ if we can drop older tablets.
- NF16. Phones and tablets.
- NF17. Must keep a **foreground service** (or equivalent) so discovery + receive works while backgrounded.

### Distribution
- NF18. Install method for family: Play Store / sideload APK / both **[?]**.

## Explicit non-goals (v1 draft)

- Cross-network / VPN / cellular relay
- End-to-end encryption audit theater (LAN trust model first; optional PIN)
- Message history, photos, video
- Smart home integrations (Alexa, etc.)
- Multi-user login on one device
