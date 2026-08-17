package com.hmx.shield.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the AndroidKeyStore. Creates and returns AES and HMAC
 * keys that never leave the secure hardware boundary.
 */
@Singleton
class KeyStoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateAesKey(alias: String): javax.crypto.SecretKey {
        keyStore.getKey(alias, null)?.let { return it as javax.crypto.SecretKey }
        val generator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    fun getOrCreateHmacKey(alias: String): javax.crypto.SecretKey {
        keyStore.getKey(alias, null)?.let { return it as javax.crypto.SecretKey }
        val generator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }
}
