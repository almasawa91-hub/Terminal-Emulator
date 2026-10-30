package com.example.myterminal.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myterminal.R
import com.example.myterminal.engine.TerminalSession
import com.example.myterminal.model.LogEntry
import com.example.myterminal.model.LogType

class MainActivity : AppCompatActivity() {
    private data class TerminalTab(
        val terminal: TerminalSession,
        val logs: MutableList<LogEntry>
    )

    private val tabs = mutableListOf<TerminalTab>()
    private var activeTabIndex = 0
    private val commandHistory = mutableListOf<String>()
    private var historyPosition = 0

    private lateinit var adapter: TerminalAdapter
    private lateinit var list: RecyclerView
    private lateinit var input: EditText
    private lateinit var sessionLabel: Button

    private val activeTab: TerminalTab
        get() = tabs[activeTabIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list = findViewById(R.id.recyclerViewTerminal)
        input = findViewById(R.id.editTextCommand)
        sessionLabel = findViewById(R.id.btnNextSession)
        adapter = TerminalAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        createSession()
        findViewById<Button>(R.id.btnNewSession).setOnClickListener { createSession() }
        sessionLabel.setOnClickListener { switchSession() }
        findViewById<Button>(R.id.btnExecute).setOnClickListener { runCommand() }

        input.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when {
                keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                    if (commandHistory.isNotEmpty()) {
                        historyPosition = (historyPosition - 1).coerceAtLeast(0)
                        input.setText(commandHistory[historyPosition])
                        input.setSelection(input.text.length)
                    }
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (commandHistory.isNotEmpty()) {
                        historyPosition = (historyPosition + 1).coerceAtMost(commandHistory.size)
                        input.setText(commandHistory.getOrElse(historyPosition) { "" })
                        input.setSelection(input.text.length)
                    }
                    true
                }
                keyCode == KeyEvent.KEYCODE_C && event.isCtrlPressed -> {
                    activeTab.terminal.interrupt()
                    appendEntry(activeTab, LogEntry("^C (shell reset requested)", LogType.ERROR))
                    true
                }
                else -> false
            }
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEND
            ) {
                runCommand()
                true
            } else false
        }
    }

    private fun createSession() {
        val terminal = TerminalSession(filesDir, cacheDir)
        val tab = TerminalTab(terminal, mutableListOf())
        tabs.add(tab)
        activeTabIndex = tabs.lastIndex
        tab.logs.add(LogEntry("MyTerminal — Android system shell", LogType.OUTPUT))
        tab.logs.add(LogEntry("Session ${tabs.size} | HOME: ${filesDir.absolutePath}", LogType.OUTPUT))
        tab.logs.add(LogEntry("Type help to view examples.", LogType.OUTPUT))
        renderActiveTab()
    }

    private fun switchSession() {
        if (tabs.size < 2) {
            appendEntry(activeTab, LogEntry("Only one session is open. Use + to create another.", LogType.OUTPUT))
            return
        }
        activeTabIndex = (activeTabIndex + 1) % tabs.size
        renderActiveTab()
    }

    private fun renderActiveTab() {
        adapter.replaceLogs(activeTab.logs)
        sessionLabel.text = "جلسة ${activeTabIndex + 1}/${tabs.size} ›"
        input.hint = "${activeTab.terminal.currentDirectory} $"
        if (adapter.itemCount > 0) list.scrollToPosition(adapter.itemCount - 1)
    }

    private fun appendEntry(tab: TerminalTab, entry: LogEntry) {
        if (entry.text == "\u0000CLEAR") {
            tab.logs.clear()
        } else {
            tab.logs.add(entry)
        }
        if (tabs.getOrNull(activeTabIndex) === tab) {
            adapter.replaceLogs(tab.logs)
            if (adapter.itemCount > 0) list.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun runCommand() {
        val command = input.text.toString()
        if (command.isBlank()) return
        input.text.clear()
        commandHistory.add(command)
        historyPosition = commandHistory.size

        val tab = activeTab
        tab.terminal.execute(
            input = command,
            onEntry = { entry -> runOnUiThread { appendEntry(tab, entry) } },
            onDirectoryChanged = { directory ->
                runOnUiThread {
                    if (tabs.getOrNull(activeTabIndex) === tab) input.hint = "$directory $"
                }
            }
        )
    }

    override fun onDestroy() {
        tabs.forEach { it.terminal.close() }
        super.onDestroy()
    }
}
