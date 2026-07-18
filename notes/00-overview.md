# Intercom App — Planning Overview

**Status:** Design draft ready (`design.md`) — awaiting final confirmation  
**Date:** 2026-07-18  
**Repo state:** Barely initialized (git only); planning notes only

## Problem

**Concrete household pain (why this exists):** Mom is in the habit of calling her child’s phone to get attention between floors (she is not fit enough to walk downstairs easily). From the child’s side, an incoming phone call is ambiguous: is this an in-house “come here / I need you” ping, or is she out of the house and actually needs a phone conversation? The reverse direction rarely happens. HomePing on both phones separates those signals:

- **HomePing ping** → she is home (same Wi‑Fi) and wants attention between floors.
- **Phone call** → she is away, or needs a real conversation.

Same-Wi‑Fi is an intentional product boundary: if she is not on the home network, she should use the phone — that is a feature of the separation, not a bug.

v1 delivers:

1. **Ping** — “I need you” attention signal to the other device on the same Wi‑Fi, with Coming/Dismissed feedback.
2. **Voice channel** — deferred to Phase 2; not required to solve the call-ambiguity problem.

## Success criteria (draft)

- Someone upstairs can, in under ~2 seconds of interaction, alert someone downstairs (and vice versa).
- Works when the receiving phone is in another room, screen off, app not in the foreground.
- UI is usable without good eyesight, fine motor control, or tech literacy.
- Zero cloud dependency for core function (same-home Wi‑Fi only).
- Devices have human-friendly names (e.g. “Kitchen”, “Mom’s room”, “Basement”).

## Out of scope (initial assumption — confirm)

- Internet / cellular fallback when Wi‑Fi is down
- Multi-home / remote family members
- Video
- Text chat / messaging history
- iOS (Android first)
- Public app store polish on day one (may ship sideload/family install first)

## Core product pillars

| Pillar | Intent |
|--------|--------|
| Same-LAN peer discovery | Find other app instances on the home Wi‑Fi without accounts |
| Named devices | Configure friendly names ahead of time |
| Ping | Loud/clear attention signal + simple dismiss/ack |
| Voice | Low-latency talk path over LAN |
| Custom sounds | Choose (and maybe import) alert tones |
| Elder-friendly UI | Huge targets, high contrast, few steps, plain language |

## Document map

| File | Purpose |
|------|---------|
| `00-overview.md` | This file — vision and map |
| `01-requirements.md` | Functional / non-functional requirements (living) |
| `02-open-questions.md` | Decisions still needed from product owner |
| `03-tech-options.md` | Stack and protocol tradeoffs |
| `04-ux-notes.md` | UI/UX principles for accessibility & age |
| `design.md` | Formal design doc (v1 HomePing — ping-only, two devices) |
