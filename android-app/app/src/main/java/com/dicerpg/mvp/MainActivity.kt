package com.dicerpg.mvp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val personaInput = findViewById<EditText>(R.id.personaInput)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val diceInput = findViewById<EditText>(R.id.diceInput)
        val logView = findViewById<TextView>(R.id.logView)

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val persona = personaInput.text.toString().ifBlank { "Eu" }
            val msg = messageInput.text.toString().ifBlank { "..." }
            append(logView, "[$persona] $msg")
            messageInput.text.clear()
        }

        findViewById<Button>(R.id.rollButton).setOnClickListener {
            try {
                val expr = diceInput.text.toString().ifBlank { "1d20" }
                val result = DiceRoller.roll(expr)
                append(logView, "[DADO] ${result.expression} -> ${result.rolls} ${fmt(result.modifier)} = ${result.total}")
            } catch (e: Exception) {
                append(logView, "[ERRO] ${e.message}")
            }
        }
    }

    private fun fmt(modifier: Int): String = if (modifier >= 0) "+$modifier" else "$modifier"

    private fun append(view: TextView, line: String) {
        val current = view.text.toString()
        view.text = if (current.isBlank()) line else "$current\n$line"
    }
}
