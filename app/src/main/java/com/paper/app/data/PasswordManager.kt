package com.paper.app.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores a salted PBKDF2 hash of the user's password. The password itself never
 * touches disk. In v2 the derived key will also encrypt the journal file itself;
 * the iteration count and salt are stored so the same KDF can be reused for that.
 */
class PasswordManager(context: Context) {

    private val prefs = context.getSharedPreferences("paper_auth", Context.MODE_PRIVATE)

    fun isPasswordSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPassword(password: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verify(password: String): Boolean {
        val salt = Base64.decode(prefs.getString(KEY_SALT, null) ?: return false, Base64.NO_WRAP)
        val stored = Base64.decode(prefs.getString(KEY_HASH, null) ?: return false, Base64.NO_WRAP)
        val candidate = derive(password, salt)
        // Constant-time comparison
        if (candidate.size != stored.size) return false
        var diff = 0
        for (i in stored.indices) diff = diff or (stored[i].toInt() xor candidate[i].toInt())
        return diff == 0
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val SALT_BYTES = 16
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
    }
}
