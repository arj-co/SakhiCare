package com.sakhicare.app.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecureKeyStorageTest {

    private lateinit var context: Context
    private lateinit var secureKeyStorage: SecureKeyStorage
    private lateinit var testMasterKeyProvider: SoftwareMasterKeyProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testMasterKeyProvider = SoftwareMasterKeyProvider()
        secureKeyStorage = SecureKeyStorage(context, testMasterKeyProvider)
        secureKeyStorage.wipePassphrase()
    }

    @Test
    fun testPassphraseGenerationIs256Bit() {
        val passphrase = secureKeyStorage.getOrCreateDatabasePassphrase()
        assertNotNull(passphrase)
        assertEquals(32, passphrase.size) // 32 bytes = 256 bits
    }

    @Test
    fun testPassphrasePersistenceAcrossCalls() {
        val passphrase1 = secureKeyStorage.getOrCreateDatabasePassphrase()
        val passphrase2 = secureKeyStorage.getOrCreateDatabasePassphrase()
        assertArrayEquals("Subsequent calls must retrieve identical decrypted passphrase", passphrase1, passphrase2)
    }

    @Test
    fun testWipePassphraseGeneratesFreshKey() {
        val passphrase1 = secureKeyStorage.getOrCreateDatabasePassphrase()
        secureKeyStorage.wipePassphrase()
        val passphrase2 = secureKeyStorage.getOrCreateDatabasePassphrase()
        assertFalse("Wiping passphrase must trigger fresh generation on next access", passphrase1.contentEquals(passphrase2))
    }

    @Test(expected = SecurityException::class)
    fun testFailClosedWhenDecryptionFails() {
        // Save corrupted data to simulate tampering or keystore key loss
        val prefs = context.getSharedPreferences("sakhicare_secure_keystore_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("enc_db_passphrase", "corrupted_base64")
            .putString("enc_db_iv", "corrupted_iv")
            .commit()

        // Must throw SecurityException rather than falling back to unencrypted or hardcoded keys
        secureKeyStorage.getOrCreateDatabasePassphrase()
    }
}
