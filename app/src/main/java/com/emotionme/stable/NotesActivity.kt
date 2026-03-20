package com.emotionme.stable

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
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
        val spinner = findViewById<Spinner>(R.id.spinnerNote)

        btnBack.setOnClickListener { finish() }

        val periods = listOf("Сегодня",
            "Неделя",
            "Текущий месяц",
            "3 месяца"
        )

        val adapter = ArrayAdapter(this, R.layout.item_spinner, periods)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        spinner.adapter = adapter

        fun fromForPosition(pos: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            when (pos) {
                0 -> {}
                1 -> cal.add(Calendar.DAY_OF_YEAR, -7)
                2 -> cal.add(Calendar.MONTH, -1)
                3 -> cal.add(Calendar.MONTH, -3)
            }
            return cal.timeInMillis
        }

        @SuppressLint("SetTextI18n")
        fun load(from: Long) {
            Thread {
                val entries = db.moodDao().getAll(userId)
                    .filter { it.note.isNotBlank() && it.timestamp >= from }

                runOnUiThread {
                    container.removeAllViews()

                    if (entries.isEmpty()) {
                        val empty = TextView(this).apply {
                            text = "Заметок за этот период нет\n\n(╯°□°）╯︵ ┻━┻ "
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

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                load(fromForPosition(pos))
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
