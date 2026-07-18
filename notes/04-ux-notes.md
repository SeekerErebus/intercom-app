# UX Notes — Elder-Friendly Intercom

## Design principles

1. **One glance, one tap** — Daily use should not require menus.
2. **Forgiving** — Mis-taps easy to undo; no destructive defaults.
3. **Loud and obvious state** — Connected / ringing / idle should be unmistakable (color + text + icon + optional speech).
4. **Setup ≠ daily use** — Complex pairing happens once (family helper). Senior daily UI stays dumb-simple.
5. **Fail visible** — “Can’t reach Kitchen” is better than silent failure.

## Daily home screen (draft concepts)

### Concept A — Two-floor household
```
┌────────────────────────────┐
│  Home Intercom             │
│                            │
│  ┌──────────────────────┐  │
│  │                      │  │
│  │   PING DOWNSTAIRS    │  │  ← huge primary
│  │                      │  │
│  └──────────────────────┘  │
│                            │
│  ┌──────────┐ ┌─────────┐  │
│  │  Talk    │ │ Settings│  │  ← secondary
│  └──────────┘ └─────────┘  │
│                            │
│  Status: Connected ✓       │
└────────────────────────────┘
```

### Concept B — Multi-device list
```
┌────────────────────────────┐
│  Who do you need?          │
│                            │
│  ┌──────────────────────┐  │
│  │ Kitchen          ●   │  │  online
│  │ [ PING ]  [ TALK ]   │  │
│  └──────────────────────┘  │
│  ┌──────────────────────┐  │
│  │ Upstairs office  ○   │  │  offline
│  │ (not available)      │  │
│  └──────────────────────┘  │
└────────────────────────────┘
```

## Incoming ping (receiver)

- Full-screen activity over lock screen when permitted
- Very large text: **“Mom is calling for you”** / **“Ping from Upstairs”**
- Buttons: **OK / Coming** (primary) · **Dismiss**
- Sound loops until ack/dismiss or timeout (e.g. 60s) with max sensible volume policy
- Optional: flash screen pulse for hearing-impaired

## Voice call UI

- Entire screen is the call: peer name, big red hang up, mute, PTT pad if half-duplex
- No tiny icon chrome
- Connecting / no answer states in plain language

## Settings (helper-oriented, still large)

- This device’s name (text field + save)
- Default ping target (if multi-device)
- Alert sound picker + **Play test**
- Pairing / home PIN
- “Intercom is on” explanation + battery optimization deep link
- About / version

## Visual system (draft)

- Prefer **high contrast**: dark text on light cream/white OR white text on deep blue/charcoal
- Primary action: saturated, not pastel
- Avoid relying on color alone (icon + label)
- Respect system font scale; layout must not break at 1.5–2×
- Minimum primary button height ~96dp; secondary ~72dp
- Avoid gesture-only navigation for critical actions

## Copy tone

| Avoid | Prefer |
|-------|--------|
| Peer offline | Not available right now |
| Establish session | Call… |
| Acknowledge | Tell them you’re coming |
| NSD discovery failed | Can’t find other devices. Check Wi‑Fi. |

## Setup flow (family installer)

1. Install app on all devices
2. Grant mic + notifications + full-screen intent (explain why)
3. Name this device
4. Enter home PIN / pair
5. Wait until other devices appear
6. Test ping both directions
7. Disable battery restrictions
8. Optional: set as default “station” (keep charged)

## Risk: “it stopped working”

Common causes to design messaging around:
- Phone killed the background service
- Switched to guest Wi‑Fi
- VPN on
- Router client isolation
- Name/IP changed after reboot (should self-heal via discovery)
