package com.emotionme.stable

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)
        val container = findViewById<LinearLayout>(R.id.notesContainer)
        val btnBack = findViewById<Button>(R.id.btnBackNotes)
        val rg = findViewById<RadioGroup>(R.id.rgPeriodNotes)

        btnBack.setOnClickListener { finish() }

        fun load(from: Long) {
            Thread {
                val entries = db.moodDao().getAll(userId)
                    .filter { it.note.isNotBlank() && it.timestamp >= from }

                runOnUiThread {
                    container.removeAllViews()

                    if (entries.isEmpty()) {
                        val empty = TextView(this).apply {
                            text = "Заметок за этот период нет (>.<)"
                            textSize = 20f
                            setPadding(32, 64, 32, 32)
                            gravity = Gravity.CENTER
                            setTextColor(ContextCompat.getColor(context, R.color.black))
                        }
                        container.addView(empty)
                        return@runOnUiThread
                    }

                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                    entries.forEach { entry ->
                        val card = layoutInflater.inflate(R.layout.item_note, container, false)
                        card.findViewById<TextView>(R.id.tvNoteDate).text =
                            sdf.format(Date(entry.timestamp))
                        card.findViewById<TextView>(R.id.tvNoteMood).text = entry.mood
                        card.findViewById<TextView>(R.id.tvNoteText).text = entry.note
                        container.addView(card)
                    }
                }
            }.start()
        }

        rg.setOnCheckedChangeListener { _, id ->
            val now = System.currentTimeMillis()
            when (id) {
                R.id.rbNoteDay -> load(now - 24 * 60 * 60 * 1000L)
                R.id.rbNoteWeek -> load(now - 7 * 24 * 60 * 60 * 1000L)
                R.id.rbNoteMonth -> load(now - 30 * 24 * 60 * 60 * 1000L)
            }
        }

        rg.check(R.id.rbNoteDay)
    }
}