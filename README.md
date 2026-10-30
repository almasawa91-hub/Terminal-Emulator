# MyTerminal

Android terminal emulator starter written in Kotlin.

## Current scope
- Executes commands through Android's `/system/bin/sh` inside the app's sandbox.
- Displays stdout, stderr, and entered commands separately.
- Supports app-local directory navigation with `cd`.
- Does **not** provide root access or a full Linux distribution.

## Build
Open this project in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
./gradlew assembleDebug
```

The first implementation is intentionally a safe, sandboxed MVP. A persistent interactive PTY, Ctrl/Tab key handling, and package environments are future milestones.
