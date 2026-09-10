package com.sakhicare.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Master Key provider abstraction for AES-GCM envelope encryption.
 */
interface MasterKeyProvider {
    fun encrypt(plainBytes: ByteArray): Pair<ByteArray, ByteArray>
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray
}

/**
 * Hardware-backed Android Keystore Master Key Provider for production.
 */
class AndroidKeystoreMasterKeyProvider : MasterKeyProvider {
    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "SakhiCareMasterDbKey"
        private const val AES_GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        val keyEntry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Keystore entry is not a SecretKeyEntry")
        return keyEntry.secretKey
    }

    override fun encrypt(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainBytes)
        return Pair(ciphertext, iv)
    }

    override fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(encryptedData)
    }
}

/**
 * Software AES-GCM Master Key Provider used for isolated Robolectric unit tests.
 */
class SoftwareMasterKeyProvider(
    private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
) : MasterKeyProvider {
    override fun encrypt(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Pair(cipher.doFinal(plainBytes), cipher.iv)
    }

    override fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedData)
    }
}

/**
 * Hardware-backed Android Keystore Master Key Manager
 *
 * Implements AES-256-GCM Keystore key generation, wrapping a 256-bit cryptographically
 * secure random passphrase for SQLCipher database encryption.
 *
 * Follows fail-closed security: does not fallback to unencrypted storage or hardcoded keys.
 */
class SecureKeyStorage(
    private val context: Context,
    private val masterKeyProvider: MasterKeyProvider = AndroidKeystoreMasterKeyProvider()
) {

    companion object {
        private const val PREFS_NAME = "sakhicare_secure_keystore_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "enc_db_passphrase"
        private const val KEY_GCM_IV = "enc_db_iv"
        private const val PASSPHRASE_BYTE_LENGTH = 32 // 256-bit passphrase
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves or generates the 256-bit SQLCipher database passphrase.
     * Guaranteed to return a valid 32-byte key or throws SecurityException (fail-closed).
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val encryptedPassphraseB64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivB64 = prefs.getString(KEY_GCM_IV, null)

        if (encryptedPassphraseB64 != null && ivB64 != null) {
            return try {
                masterKeyProvider.decrypt(
                    encryptedData = Base64.decode(encryptedPassphraseB64, Base64.NO_WRAP),
                    iv = Base64.decode(ivB64, Base64.NO_WRAP)
                )
            } catch (e: Exception) {
                throw SecurityException("Failed to decrypt database passphrase via Android Keystore. Failing closed.", e)
            }
        }

        // Generate fresh 256-bit random passphrase
        val freshPassphrase = ByteArray(PASSPHRASE_BYTE_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }

        try {
            val (ciphertext, iv) = masterKeyProvider.encrypt(freshPassphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_GCM_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit()
            return freshPassphrase
        } catch (e: Exception) {
            throw SecurityException("Failed to secure database passphrase in Android Keystore.", e)
        }
    }

    /**
     * Wipes stored encrypted keys for explicit administrative reset only.
     */
    @Synchronized
    fun wipePassphrase() {
        prefs.edit().clear().commit()
    }
}
