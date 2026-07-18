# Open Questions

---

## Resolved

| ID | Decision | Date |
|----|----------|------|
| Q1 | Exactly **2 devices** (one per floor) | 2026-07-18 |
| Q4 | **Ping only in v1**; voice deferred | 2026-07-18 |
| Q5 | Voice (later): **must tap Accept** | 2026-07-18 |
| Q6 | **Yes** — sender sees Coming / Dismissed | 2026-07-18 |
| Q8 | **Sideload APK only** | 2026-07-18 |
| Q9 | **Shared home PIN** | 2026-07-18 |
| Q10 | **Pocket-phone capable** (not station-only) | 2026-07-18 |
| Q11 | No cloud / no accounts | 2026-07-18 |
| Q14 | App name: **HomePing** | 2026-07-18 |
| Q15 | Two-device home: giant Ping + status | 2026-07-18 |
| Q16 | **High-priority notification + strong sound** (not full-screen takeover) | 2026-07-18 |
| Q17 | General elder-friendly; no special sensory program | 2026-07-18 |

### Implications

- **UI:** One primary **Ping** action; peer friendly name + online/offline; after ping, show waiting / Coming / Dismissed / no response.
- **v1 scope:** LAN discovery, PIN, reliable ping + ack, customizable sounds, high-priority alerts, elder UI. No voice/media.
- **Alert UX:** Heads-up / high-priority notification channel with strong custom sound — not lock-screen full-screen intent takeover (simpler permissions; may revisit if misses are common).
- **Reliability (hard mode):** Design for phones in pockets with normal battery use. Requires foreground service, careful Doze strategy, user-facing “allow background” guidance, and honest limits documentation.
- **Protocol:** Extensible message types so Phase 2 voice can slot in.

---

## Still open (non-blocking defaults proposed)

| ID | Topic | Proposed default |
|----|-------|------------------|
| Q7 | Min Android version | **API 26** (Android 8.0) — notification channels; drop ancient devices |
| Q12 | Custom sounds | Built-in pack + optional file import if easy |
| Q13 | Language | English only v1 |
| Q18 | Target selection | Always the other paired peer (no picker) |
| Q19 | Offline peer | Clear “not available” + Retry; no SMS fallback |
| Q20 | Voice UI placeholder | Hide Talk until Phase 2 (no disabled tease button) |
| Q21 | PIN length | 4–6 digits, set on first run, same on both devices |

If defaults are wrong, say so before implementation.
