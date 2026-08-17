package com.hmx.shield.core.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption for vault file contents. IV is prepended to ciphertext.
 * All operations run on a single background dispatcher; callers must not block UI.
 */
@Singleton
class CryptoManager @Inject constructor(
    private val keyStoreManager: KeyStoreManager
) {
    private val transformation = "AES/GCM/NoPadding"
    private val ivLength = 12

    fun encrypt(plainBytes: ByteArray, alias: String = com.hmx.shield.core.Constants.KEY_VAULT_AES): String {
        val key: SecretKey = keyStoreManager.getOrCreateAesKey(alias)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)
        val out = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(encrypted, 0, out, iv.size, encrypted.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, alias: String = com.hmx.shield.core.Constants.KEY_VAULT_AES): ByteArray {
        val key: SecretKey = keyStoreManager.getOrCreateAesKey(alias)
        val raw = Base64.decode(cipherText, Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, ivLength)
        val encrypted = raw.copyOfRange(ivLength, raw.size)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}
