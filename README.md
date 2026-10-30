# MyTerminal

Native Android terminal interface built with Kotlin and Android Views. Each
terminal tab owns an independent persistent `/system/bin/sh` process.

## Features

- Black terminal UI with monospace command and output text.
- Command entry button and keyboard action.
- Persistent shell per session; `cd` and exported shell variables persist.
- Live line-based stdout (white) and stderr (red) readers.
- Separate command history navigated with the keyboard up/down keys.
- Multiple terminal sessions using `+ جلسة` and the session switch button.
- `Ctrl+C` requests interruption by terminating the active shell process;
  this resets that session's shell. Android may not terminate every detached
  child process because this implementation does not yet use a PTY/process group.
- App-private `filesDir` as the initial working directory and `HOME`.
- Built-in `help` and `clear` commands.

## Shell command availability

Commands are executed by the Android device's `/system/bin/sh`. Availability
and permissions depend on the Android version and device image. Common examples
include `pwd`, `ls`, `cat`, `date`, `mkdir`, `rm`, `cp`, `mv`, `grep`,
`find`, `chmod`, `df`, `du`, `ps`, `env`, `whoami`, and `uname`.
Some devices do not include `ping`, or restrict its use.

## Important limitations

This is a system-shell terminal, not a full Linux distribution such as Termux.
It does not bundle a package manager or GNU/Linux userland, does not provide
root, and does not yet implement a PTY/terminal emulator. As a result, full-screen
interactive programs (for example editors, shells that prompt for passwords,
and programs that require raw terminal input) are not supported. Job control,
terminal escape-sequence rendering, and guaranteed process-group Ctrl+C are
also not implemented. App-private HOME is the initial directory, not a
filesystem jail; Android's application UID sandbox and permissions still apply.

## Build in Android Studio

1. Open the repository in Android Studio.
2. Use JDK 17 and install Android SDK Platform 35.
3. Sync Gradle and run the `app` configuration on an Android device/emulator.

GitHub Actions builds a debug APK on pushes to `main`.
