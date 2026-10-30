package com.example.myterminal.engine

import com.example.myterminal.model.LogEntry
import com.example.myterminal.model.LogType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Initial sandboxed command interpreter. It deliberately implements a small set
 * of file and information commands instead of invoking an unrestricted system shell.
 */
class TerminalSession(internalDir: File) {
    var currentDirectory: File = internalDir.canonicalFile
        private set
    private val homeDirectory = internalDir.canonicalFile

    fun handleCommand(input: String, callback: (LogEntry) -> Unit) {
        val command = input.trim()
        if (command.isEmpty()) return
        callback(LogEntry("${getPrompt()} $command", LogType.COMMAND))
        val parts = command.split(Regex("\\s+"), limit = 2)
        val name = parts[0]
        val arg = parts.getOrNull(1)?.trim()?.trim('"', '\'').orEmpty()
        try {
            when (name) {
                "pwd" -> out(currentDirectory.absolutePath, callback)
                "ls" -> {
                    val dir = if (arg.isBlank()) currentDirectory else resolve(arg)
                    val entries = dir.listFiles()?.sortedBy { it.name }
                    if (entries == null) err("ls: cannot read directory", callback)
                    else entries.forEach { out(it.name + if (it.isDirectory) "/" else "", callback) }
                }
                "cd" -> {
                    val destination = when (arg) {
                        "", "~" -> homeDirectory
                        "-" -> currentDirectory
                        else -> resolve(arg)
                    }.canonicalFile
                    if (destination.isDirectory && destination.canRead()) currentDirectory = destination
                    else err("cd: not a readable directory: $arg", callback)
                }
                "mkdir" -> {
                    if (arg.isBlank()) err("mkdir: missing operand", callback)
                    else if (!resolve(arg).mkdirs() && !resolve(arg).isDirectory) err("mkdir: cannot create '$arg'", callback)
                }
                "touch" -> {
                    if (arg.isBlank()) err("touch: missing operand", callback)
                    else if (!resolve(arg).exists() && !resolve(arg).createNewFile()) err("touch: cannot create '$arg'", callback)
                }
                "cat" -> {
                    if (arg.isBlank()) err("cat: missing operand", callback)
                    else {
                        val file = resolve(arg)
                        if (!file.isFile || !file.canRead()) err("cat: cannot read '$arg'", callback)
                        else file.useLines { lines -> lines.forEach { out(it, callback) } }
                    }
                }
                "date" -> out(SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault()).format(Date()), callback)
                "clear" -> callback(LogEntry("\u0000CLEAR", LogType.OUTPUT))
                "help" -> out("Available: pwd, ls [path], cd [path], mkdir <name>, touch <file>, cat <file>, date, clear, help", callback)
                else -> err("$name: command not available in this initial sandboxed build", callback)
            }
        } catch (e: Exception) {
            err("${e.localizedMessage ?: "Command failed"}", callback)
        }
    }

    private fun resolve(path: String): File {
        val expanded = when {
            path == "~" -> homeDirectory
            path.startsWith("~/") -> File(homeDirectory, path.substring(2))
            else -> File(path)
        }
        return (if (expanded.isAbsolute) expanded else File(currentDirectory, path)).canonicalFile
    }

    private fun out(text: String, callback: (LogEntry) -> Unit) = callback(LogEntry(text, LogType.OUTPUT))
    private fun err(text: String, callback: (LogEntry) -> Unit) = callback(LogEntry(text, LogType.ERROR))
    fun getPrompt() = "${currentDirectory.absolutePath} $"
}
