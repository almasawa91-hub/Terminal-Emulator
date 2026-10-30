package com.example.myterminal.model

enum class LogType { COMMAND, OUTPUT, ERROR }
data class LogEntry(val text: String, val type: LogType)
