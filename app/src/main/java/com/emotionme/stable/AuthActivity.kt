package com.emotionme.stable

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import androidx.core.content.edit

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.getUser(this) != 0L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_auth)

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()
        val executor = Executors.newSingleThreadExecutor()

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Заполните поля \uD83D\uDCAD", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executor.execute {
                val user = userDao.login(login, password)

                runOnUiThread {
                    if (user != null) {
                        SessionManager.saveUser(this, user.id)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Неверный логин или пароль ❌", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Registration
        btnRegister.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Заполните поля ❌", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executor.execute {
                val exists = userDao.getByLogin(login)

                if (exists != null) {
                    runOnUiThread {
                        Toast.makeText(this, "Пользователь уже существует ❌", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    val id = userDao.insert(
                        User(
                            login = login,
                            password = password
                        )
                    )

                    runOnUiThread {
                        SessionManager.saveUser(this, id)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}