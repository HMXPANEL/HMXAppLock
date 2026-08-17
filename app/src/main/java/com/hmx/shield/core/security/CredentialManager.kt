package com.hmx.shield.core.security

import android.util.Base64
import com.hmx.shield.core.Constants
import com.hmx.shield.core.model.LockType
import javax.crypto.Mac
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and verifies the master lock secret as a salted HMAC-SHA256 digest.
 * The raw secret (PIN/pattern/password) is never persisted.
 */
@Singleton
class CredentialManager @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val keyStoreManager: KeyStoreManager
) {
    fun hasCredential(): Boolean = securePreferences.contains(Constants.PREF_CREDENTIAL_HASH)

    fun getLockType(): LockType? {
        val name = securePreferences.getString(Constants.PREF_LOCK_TYPE)
        return if (name.isBlank()) null else LockType.valueOf(name)
    }

    fun setCredential(secret: String, type: LockType) {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val hash = computeHash(salt, secret)
        securePreferences.putString(Constants.PREF_CREDENTIAL_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        securePreferences.putString(Constants.PREF_CREDENTIAL_HASH, hash)
        securePreferences.putString(Constants.PREF_LOCK_TYPE, type.name)
    }

    fun verify(secret: String): Boolean {
        if (!hasCredential()) return false
        val salt = Base64.decode(securePreferences.getString(Constants.PREF_CREDENTIAL_SALT), Base64.NO_WRAP)
        val expected = securePreferences.getString(Constants.PREF_CREDENTIAL_HASH)
        val actual = computeHash(salt, secret)
        return constantTimeEquals(expected, actual)
    }

    fun clear() {
        securePreferences.remove(Constants.PREF_CREDENTIAL_HASH)
        securePreferences.remove(Constants.PREF_CREDENTIAL_SALT)
        securePreferences.remove(Constants.PREF_LOCK_TYPE)
    }

    private fun computeHash(salt: ByteArray, secret: String): String {
        val key = keyStoreManager.getOrCreateHmacKey(Constants.KEY_CREDENTIAL_HMAC)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        mac.update(salt)
        mac.update(secret.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(mac.doFinal(), Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
