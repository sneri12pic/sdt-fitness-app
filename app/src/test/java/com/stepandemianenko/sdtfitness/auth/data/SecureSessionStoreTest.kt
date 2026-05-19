package com.stepandemianenko.sdtfitness.auth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureSessionStoreTest {

    @Test
    fun saveAndLoad_roundTripsStoredSession() {
        val storage = InMemorySessionStorage()
        val store = SecureSessionStore(
            crypto = PrefixSessionCrypto,
            storage = storage
        )

        val session = StoredSession(
            remoteUserId = "remote-1",
            email = "user@example.com",
            displayName = "Example User",
            authProvider = "password",
            refreshToken = "refresh-token",
            accessTokenExpiresAtMillis = 1234L
        )

        store.save(session)

        assertEquals(session, store.load())
    }

    @Test
    fun clear_removesStoredSession() {
        val storage = InMemorySessionStorage()
        val store = SecureSessionStore(
            crypto = PrefixSessionCrypto,
            storage = storage
        )

        store.save(
            StoredSession(
                remoteUserId = "remote-1",
                email = "user@example.com",
                displayName = null,
                authProvider = "password",
                refreshToken = "refresh-token",
                accessTokenExpiresAtMillis = 1234L
            )
        )

        store.clear()

        assertNull(store.load())
    }

    private class InMemorySessionStorage : SessionStorage {
        private var bytes: ByteArray? = null

        override fun read(): ByteArray? = bytes

        override fun write(bytes: ByteArray) {
            this.bytes = bytes
        }

        override fun clear() {
            bytes = null
        }
    }

    private object PrefixSessionCrypto : SessionCrypto {
        override fun encrypt(plainText: ByteArray): ByteArray {
            return ENCRYPTED_PREFIX + plainText
        }

        override fun decrypt(cipherText: ByteArray): ByteArray {
            return cipherText.drop(ENCRYPTED_PREFIX.size).toByteArray()
        }

        private val ENCRYPTED_PREFIX = "encrypted:".toByteArray()
    }
}
