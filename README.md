# MyTerminal

A native Android terminal interface written in Kotlin. It starts a persistent
`/system/bin/sh` process using `ProcessBuilder` and keeps that same shell
between commands, so shell built-ins such as `cd` and exported variables persist.

## Implemented

- Native black terminal screen with monospace text and scrollable history.
- Command input, execute button, and keyboard action.
- Persistent Android system shell (`/system/bin/sh`).
- Separate live readers for stdout (white) and stderr (red).
- Green command echo and persistent working directory.
- App-private `filesDir` as the initial working directory and `HOME`.
- The shell runs with the Android app's UID and normal Android permissions.
- Commands available on the device can be used, including `ls`, `pwd`,
  `cat`, `date`, `ping`, `mkdir`, and `rm`, subject to Android version,
  executable availability, and device restrictions.

## Android limitations

This is an Android system-shell terminal, not a complete Linux distribution
like Termux. It does not bundle a package manager, GNU userland, root access,
or a PTY/interactive terminal stack. Some commands (especially `ping`) may
be missing or restricted by the device. Setting `filesDir` as HOME does not
create a filesystem jail; Android's app UID sandbox and permissions still
determine which files and system resources the process can access.

## Build in Android Studio

1. Open this repository in Android Studio.
2. Use JDK 17 and install Android SDK Platform 35.
3. Sync Gradle and run the `app` configuration on an Android device/emulator.

GitHub Actions builds a debug APK on pushes to `main`.
