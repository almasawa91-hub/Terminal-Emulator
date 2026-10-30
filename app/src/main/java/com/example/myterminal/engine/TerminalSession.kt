package com.example.myterminal.engine

import com.example.myterminal.model.LogEntry
import com.example.myterminal.model.LogType
import java.io.File

/** UI-facing session; app-private files storage is the shell's initial HOME. */
class TerminalSession(internalDirectory: File, cacheDirectory: File) {
    private val homeDirectory = internalDirectory.canonicalFile
    private val executor = CommandExecutor(homeDirectory, cacheDirectory.canonicalFile)

    @Volatile
    var currentDirectory: String = homeDirectory.absolutePath
        private set

    fun execute(
        input: String,
        onEntry: (LogEntry) -> Unit,
        onDirectoryChanged: (String) -> Unit
    ) {
        val command = input.trim()
        if (command.isEmpty()) return
        onEntry(LogEntry("$currentDirectory $ $command", LogType.COMMAND))

        when (command) {
            "clear" -> {
                executor.executeLocal {
                    onEntry(LogEntry("\u0000CLEAR", LogType.OUTPUT))
                }
                return
            }
            "help" -> {
                executor.executeLocal {
                    onEntry(LogEntry(
                        "Android system shell: /system/bin/sh\n" +
                            "Examples: pwd, ls, cd, cat, date, ping, mkdir, rm, cp, mv, echo, grep, find, chmod, df, du, ps, env, whoami, uname\n" +
                            "HOME: $homeDirectory\n" +
                            "Commands run with this app's Android UID and permissions.",
                        LogType.OUTPUT
                    ))
                }
                return
            }
        }

        executor.execute(
            command = command,
            onStdout = { line -> onEntry(LogEntry(line, LogType.OUTPUT)) },
            onStderr = { line -> onEntry(LogEntry(line, LogType.ERROR)) },
            onComplete = { directory, _ ->
                currentDirectory = directory
                onDirectoryChanged(directory)
            }
        )
    }

    fun interrupt() = executor.interrupt()

    fun close() = executor.close()
}
