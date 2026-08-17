package com.sakhicare.app.data

object SecureStorageStub {
    fun getEncryptionKeyAlias(): String = "SakhiCareMasterKey"
    fun isDeviceHardwareBackedKey(): Boolean = true
}
