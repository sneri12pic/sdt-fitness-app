package com.stepandemianenko.sdtfitness.auth.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredSession(
    val remoteUserId: String,
    val email: String,
    val displayName: String?,
    val authProvider: String,
    val refreshToken: String,
    val accessTokenExpiresAtMillis: Long
)

interface SessionCrypto {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(cipherText: ByteArray): ByteArray
}

interface SessionStorage {
    fun read(): ByteArray?
    fun write(bytes: ByteArray)
    fun clear()
}

class SecureSessionStore(
    private val crypto: SessionCrypto,
    private val storage: SessionStorage
) {

    constructor(context: Context) : this(
        crypto = AndroidKeystoreSessionCrypto(),
        storage = NoBackupFileSessionStorage(context)
    )

    fun save(session: StoredSession) {
        val properties = Properties().apply {
            setProperty(KEY_REMOTE_USER_ID, session.remoteUserId)
            setProperty(KEY_EMAIL, session.email)
            session.displayName?.let { setProperty(KEY_DISPLAY_NAME, it) }
            setProperty(KEY_AUTH_PROVIDER, session.authProvider)
            setProperty(KEY_REFRESH_TOKEN, session.refreshToken)
            setProperty(KEY_ACCESS_TOKEN_EXPIRES_AT, session.accessTokenExpiresAtMillis.toString())
        }

        val plainText = ByteArrayOutputStream().use { output ->
            properties.store(output, null)
            output.toByteArray()
        }
        storage.write(crypto.encrypt(plainText))
    }

    fun load(): StoredSession? {
        val encrypted = storage.read() ?: return null
        val properties = Properties()
        return try {
            properties.load(ByteArrayInputStream(crypto.decrypt(encrypted)))
            StoredSession(
                remoteUserId = properties.getProperty(KEY_REMOTE_USER_ID).orEmpty(),
                email = properties.getProperty(KEY_EMAIL).orEmpty(),
                displayName = properties.getProperty(KEY_DISPLAY_NAME)?.takeIf { it.isNotBlank() },
                authProvider = properties.getProperty(KEY_AUTH_PROVIDER, "password"),
                refreshToken = properties.getProperty(KEY_REFRESH_TOKEN).orEmpty(),
                accessTokenExpiresAtMillis = properties.getProperty(KEY_ACCESS_TOKEN_EXPIRES_AT)
                    ?.toLongOrNull() ?: 0L
            ).takeIf {
                it.remoteUserId.isNotBlank() &&
                    it.email.isNotBlank() &&
                    it.refreshToken.isNotBlank()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        storage.clear()
    }

    private companion object {
        const val KEY_REMOTE_USER_ID = "remoteUserId"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_AUTH_PROVIDER = "authProvider"
        const val KEY_REFRESH_TOKEN = "refreshToken"
        const val KEY_ACCESS_TOKEN_EXPIRES_AT = "accessTokenExpiresAtMillis"
    }
}

class NoBackupFileSessionStorage(context: Context) : SessionStorage {
    private val file = File(context.noBackupFilesDir, FILE_NAME)

    override fun read(): ByteArray? {
        return if (file.exists()) file.readBytes() else null
    }

    override fun write(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    override fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }

    private companion object {
        const val FILE_NAME = "sdt_auth_session.bin"
    }
}

class AndroidKeystoreSessionCrypto : SessionCrypto {
    override fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(plainText)
        val iv = cipher.iv
        val encodedIv = Base64.getEncoder().encodeToString(iv)
        val encodedCipherText = Base64.getEncoder().encodeToString(cipherText)
        return "$encodedIv:$encodedCipherText".toByteArray(Charsets.UTF_8)
    }

    override fun decrypt(cipherText: ByteArray): ByteArray {
        val encoded = cipherText.toString(Charsets.UTF_8)
        val parts = encoded.split(":", limit = 2)
        require(parts.size == 2) { "Invalid session payload." }
        val iv = Base64.getDecoder().decode(parts[0])
        val encrypted = Base64.getDecoder().decode(parts[1])
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "sdt_fitness_auth_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
