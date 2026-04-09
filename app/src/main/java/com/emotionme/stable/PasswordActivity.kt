package com.emotionme.stable

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class PasswordActivity : AppCompatActivity() {

    private var doubleClick: Long = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_password)

        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val btnBack = findViewById<TextView>(R.id.btnBackPassword)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - doubleClick < 2000) {
                performAction()
                doubleClick = 0
            } else {
                doubleClick = currentTime
                Snackbar.make(
                    btnSave,
                    "Нажми ещё раз, чтобы подтвердить изменение пароля",
                    Snackbar.LENGTH_SHORT
                )
                    .setDuration(2000)
                    .show()
            }
        }
    }

    private fun performAction() {

        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        val etNew = findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val newPass = etNew.text.toString().trim()
        val confirmPass = etConfirm.text.toString().trim()
        val root = btnSave.rootView

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Snackbar.make(root, "Заполните все поля", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Snackbar.make(root, "Пароли не совпадают", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 5) {
            Snackbar.make(
                root,
                "Пароль должен содержать не менее 5 символов",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        Thread {
            val user = db.userDao().getById(userId)

            if (user == null) {
                runOnUiThread { finish() }
                return@Thread
            }

            if (newPass == user.password) {
                runOnUiThread {
                    Snackbar.make(
                        root,
                        "Новый пароль должен отличаться",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                return@Thread
            }

            db.userDao().updatePassword(userId, newPass)
            runOnUiThread {
                Toast.makeText(this, "Пароль успешно изменён ✅", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }
}