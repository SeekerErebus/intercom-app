# HomePing

Home-Wi‑Fi attention pings between two household phones.

**Why:** When Mom needs someone on the other floor, a normal phone call is ambiguous — is she home and just needs attention, or is she out and needs a real call? HomePing is the in-house signal. A phone call can mean “away / need to talk.”

**v1 scope:** Ping + Coming/Dismissed on the same Wi‑Fi. No cloud accounts. Voice is later.

Design notes live in [`notes/`](notes/), especially [`notes/design.md`](notes/design.md).

## Status

PR2 — device identity, DataStore preferences, first-run setup wizard (name + home PIN), settings. Networking not implemented yet.

## Requirements

- JDK 17+
- Android SDK (platform 35, build-tools 35 recommended)
- Android device or emulator, **API 26+**

This repo was developed against a user-local toolchain at `~/.local/android-dev` when present:

```fish
set -x JAVA_HOME ~/.local/android-dev/jdk-17
set -x ANDROID_HOME ~/.local/android-dev/android-sdk
set -x PATH $JAVA_HOME/bin $PATH
```

Create `local.properties` (gitignored) if the Android Gradle Plugin cannot find the SDK:

```properties
sdk.dir=/home/YOU/.local/android-dev/android-sdk
```

## Build

```fish
# from repo root
./gradlew :app:assembleDebug
```

Debug APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Install (sideload)

```fish
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the phone: allow install from the computer / unknown sources as prompted.

## Project layout

```
app/src/main/java/com/homeping/app/
  MainActivity.kt
  ui/MainScreen.kt          # large Ping button shell
  ui/theme/                 # high-contrast, large type
notes/                      # product & design docs
```

## Implementation plan (summary)

See **PR Plan** in `notes/design.md`: setup → foreground service → discovery → PIN auth → ping E2E → reliability → packaging.
