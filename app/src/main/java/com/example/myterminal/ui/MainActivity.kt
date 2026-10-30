package com.example.myterminal.ui

import android.os.Bundle
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
    private lateinit var session: TerminalSession
    private lateinit var adapter: TerminalAdapter
    private lateinit var list: RecyclerView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list = findViewById(R.id.recyclerViewTerminal)
        input = findViewById(R.id.editTextCommand)
        adapter = TerminalAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        session = TerminalSession(filesDir, cacheDir)
        adapter.addLog(LogEntry("MyTerminal — Android system shell", LogType.OUTPUT))
        adapter.addLog(LogEntry("HOME: ${filesDir.absolutePath}", LogType.OUTPUT))
        adapter.addLog(LogEntry("Type help to view examples.", LogType.OUTPUT))
        input.hint = "${session.currentDirectory} $"

        findViewById<Button>(R.id.btnExecute).setOnClickListener { runCommand() }
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

    private fun runCommand() {
        val command = input.text.toString()
        if (command.isBlank()) return
        input.text.clear()

        session.execute(
            input = command,
            onEntry = { entry ->
                runOnUiThread {
                    if (entry.text == "\u0000CLEAR") {
                        adapter.clear()
                    } else {
                        adapter.addLog(entry)
                        if (adapter.itemCount > 0) list.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            },
            onDirectoryChanged = { directory ->
                runOnUiThread { input.hint = "$directory $" }
            }
        )
    }

    override fun onDestroy() {
        if (::session.isInitialized) session.close()
        super.onDestroy()
    }
}
