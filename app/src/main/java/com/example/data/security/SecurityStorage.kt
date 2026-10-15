package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityStorage(context: Context) {

    private val keyStoreAlias = "NexusTunnelSecMasterKey_v2"
    private val prefs: SharedPreferences = context.getSharedPreferences("nexus_sec_vault", Context.MODE_PRIVATE)

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (_: Throwable) {
        null
    }

    private val fallbackKey: SecretKey = deriveFallbackKey(context)

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        if (keyStore != null) {
            try {
                if (!keyStore.containsAlias(keyStoreAlias)) {
                    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                    val spec = KeyGenParameterSpec.Builder(
                        keyStoreAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                    keyGenerator.init(spec)
                    keyGenerator.generateKey()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun deriveFallbackKey(context: Context): SecretKey {
        val seed = "NexusVaultFallback_${context.packageName}_2026"
        val sha = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(sha, "AES")
    }

    private fun getSecretKey(): SecretKey {
        if (keyStore != null) {
            try {
                val entry = keyStore.getEntry(keyStoreAlias, null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey
            } catch (_: Throwable) {}
        }
        return fallbackKey
    }

    @Synchronized
    fun putEncrypted(key: String, plainText: String?) {
        if (plainText == null) {
            prefs.edit().remove(key).apply()
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(key, encoded).apply()
        } catch (_: Throwable) {
            val encoded = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString(key, "b64:$encoded").apply()
        }
    }

    @Synchronized
    fun getDecrypted(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        if (stored.startsWith("b64:")) {
            return try {
                String(Base64.decode(stored.removePrefix("b64:"), Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
        return try {
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            if (combined.size < 12) return null
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
