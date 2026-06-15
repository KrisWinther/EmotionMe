package com.emotionme.stable

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256 // бит
    private const val SALT_LENGTH = 32 // байт

    // Генерирует криптографически случайную соль.
    // Возвращает Base64-строку для хранения в БД.
    fun generateSalt(): String {
        val bytes = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // Хэширует пароль с заданной солью.
    // Возвращает Base64-строку хэша для хранения в БД.
    fun hash(password: String, saltBase64: String): String {
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    // Проверяет пароль против сохранённого хэша и соли.
    // Использует постоянное по времени сравнение — защита от timing-атак.
    fun verify(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val actualHash = Base64.decode(hash(password, saltBase64), Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
        if (actualHash.size != expectedHash.size) return false
        var diff = 0
        for (i in actualHash.indices) {
            diff = diff or (actualHash[i].toInt() xor expectedHash[i].toInt())
        }
        return diff == 0
    }
}
