package com.example.myterminal.engine

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Owns one persistent /system/bin/sh process and streams both output channels. */
class CommandExecutor(
    private val homeDirectory: File,
    private val temporaryDirectory: File
) {
    private data class PendingCommand(
        val marker: String,
        val onStdout: (String) -> Unit,
        val onStderr: (String) -> Unit,
        val onComplete: (String, Int) -> Unit,
        val finished: CountDownLatch = CountDownLatch(1)
    ) {
        private var stdoutDone = false
        private var stderrDone = false
        private var completed = false
        private var directory = ""
        private var exitCode = 1

        @Synchronized
        fun markStdout(path: String, status: Int) {
            directory = path
            exitCode = status
            stdoutDone = true
            finishIfReady()
        }

        @Synchronized
        fun markStderr() {
            stderrDone = true
            finishIfReady()
        }

        @Synchronized
        fun fail(message: String, fallbackDirectory: String) {
            if (completed) return
            completed = true
            onStderr(message)
            onComplete(fallbackDirectory, 127)
            finished.countDown()
        }

        private fun finishIfReady() {
            if (completed || !stdoutDone || !stderrDone) return
            completed = true
            onComplete(directory, exitCode)
            finished.countDown()
        }
    }

    private val commandQueue = Executors.newSingleThreadExecutor()
    @Volatile private var process: Process? = null
    @Volatile private var shellInput: BufferedWriter? = null
    @Volatile private var pending: PendingCommand? = null
    @Volatile private var closed = false

    init {
        homeDirectory.mkdirs()
        temporaryDirectory.mkdirs()
    }

    fun execute(
        command: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onComplete: (String, Int) -> Unit
    ) {
        commandQueue.execute commandTask@{
            if (closed) return@commandTask
            try {
                ensureShell()
                val marker = "__MYTERMINAL_${java.util.UUID.randomUUID().toString().replace("-", "")}__"
                val task = PendingCommand(marker, onStdout, onStderr, onComplete)
                pending = task

                val input = shellInput ?: throw IllegalStateException("Shell input is unavailable")
                input.write(command)
                input.newLine()
                // Capture $? before printf changes the shell's last exit status.
                input.write("__myterm_status=\$?; printf '\\n$marker\\t%s\\t%s\\n' \"\$PWD\" \"\$__myterm_status\" >&8; printf '\\n$marker\\n' >&9")
                input.newLine()
                input.flush()

                // Serialize commands while reader threads continue streaming output.
                task.finished.await()
                if (pending === task) pending = null
            } catch (error: Exception) {
                pending?.let {
                    it.fail("Terminal: ${error.message ?: "Unable to execute command"}", homeDirectory.absolutePath)
                }
                pending = null
                destroyShell()
            }
        }
    }


    /** Run a UI-side built-in in the same queue as shell commands. */
    fun executeLocal(action: () -> Unit) {
        commandQueue.execute {
            if (!closed) action()
        }
    }

    private fun isShellAlive(): Boolean {
        val current = process ?: return false
        return try {
            current.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    private fun ensureShell() {
        if (isShellAlive() && shellInput != null) return
        destroyShell()
        val builder = ProcessBuilder("/system/bin/sh").directory(homeDirectory)
        builder.environment().apply {
            put("HOME", homeDirectory.absolutePath)
            put("PWD", homeDirectory.absolutePath)
            put("TMPDIR", temporaryDirectory.absolutePath)
            put("PATH", "/system/bin:/system/xbin")
            put("TERM", "xterm-256color")
        }
        val started = builder.start()
        process = started
        shellInput = BufferedWriter(OutputStreamWriter(started.outputStream))
        // Keep private protocol descriptors so user redirections do not swallow completion markers.
        shellInput?.write("exec 8>&1 9>&2")
        shellInput?.newLine()
        shellInput?.flush()
        Thread({ readStdout(started) }, "myterminal-stdout").apply { isDaemon = true; start() }
        Thread({ readStderr(started) }, "myterminal-stderr").apply { isDaemon = true; start() }
    }

    private fun readStdout(shell: Process) {
        try {
            BufferedReader(InputStreamReader(shell.inputStream)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    val task = pending
                    if (task != null && line.startsWith(task.marker + "\t")) {
                        val fields = line.split('\t', limit = 3)
                        val directory = fields.getOrNull(1)?.takeIf { it.isNotBlank() }
                            ?: homeDirectory.absolutePath
                        val exitCode = fields.getOrNull(2)?.toIntOrNull() ?: 1
                        task.markStdout(directory, exitCode)
                    } else {
                        task?.onStdout(line)
                    }
                }
            }
        } catch (_: Exception) {
            // Process teardown is handled below.
        } finally {
            if (process === shell) {
                pending?.let { task ->
                    task.fail("Terminal: shell process ended", homeDirectory.absolutePath)
                }
            }
        }
    }

    private fun readStderr(shell: Process) {
        try {
            BufferedReader(InputStreamReader(shell.errorStream)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    val task = pending
                    if (task != null && line == task.marker) {
                        task.markStderr()
                    } else {
                        task?.onStderr(line)
                    }
                }
            }
        } catch (_: Exception) {
            // The shell may be closing.
        }
    }

    private fun destroyShell() {
        try { shellInput?.close() } catch (_: Exception) {}
        shellInput = null
        try { process?.destroy() } catch (_: Exception) {}
        process = null
    }

    fun close() {
        closed = true
        commandQueue.shutdownNow()
        try {
            commandQueue.awaitTermination(300, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        pending?.finished?.countDown()
        pending = null
        destroyShell()
    }
}
