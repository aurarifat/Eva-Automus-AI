package com.evai.rifatcoder.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class AiProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OMNIROUTE("OmniRoute"),
}

enum class ProviderConnectionStatus {
    NOT_CONFIGURED,
    TESTING,
    CONNECTED,
    CONNECTION_FAILED,
    CONFIGURATION_CHANGED,
}

class SecureConfigStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var keyStore: KeyStore? = null
    private var testFallbackKey: SecretKey? = null

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            keyStore = ks
            ensureSecretKeyExists(ks)
        } catch (_: Throwable) {
            // AndroidKeyStore is not present in standard host JVM or Robolectric test runners.
            // Provide a deterministic AES-256 test key so encryption/decryption works identically in unit tests.
            val seed = "eva_ai_secure_aes_test_fallback_seed_256bit".toByteArray(Charsets.UTF_8).copyOf(32)
            testFallbackKey = SecretKeySpec(seed, "AES")
        }
    }

    private fun ensureSecretKeyExists(ks: KeyStore) {
        if (!ks.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        testFallbackKey?.let { return it }
        val ks = keyStore ?: throw IllegalStateException("KeyStore is unavailable")
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Combine IV (12 bytes for GCM) + Encrypted data
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    // Active Provider
    fun getActiveProvider(): AiProviderType {
        val name = prefs.getString(KEY_ACTIVE_PROVIDER, AiProviderType.GEMINI.name) ?: AiProviderType.GEMINI.name
        return try {
            AiProviderType.valueOf(name)
        } catch (_: Exception) {
            AiProviderType.GEMINI
        }
    }

    fun setActiveProvider(type: AiProviderType) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, type.name).apply()
    }

    // Gemini
    fun saveGeminiApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            clearGeminiApiKey()
        } else {
            val encrypted = encrypt(apiKey.trim())
            prefs.edit().putString(KEY_GEMINI_KEY, encrypted).apply()
        }
    }

    fun getGeminiApiKey(): String? {
        val encrypted = prefs.getString(KEY_GEMINI_KEY, null) ?: return null
        return decrypt(encrypted)
    }

    fun hasGeminiApiKey(): Boolean {
        return !prefs.getString(KEY_GEMINI_KEY, null).isNullOrBlank()
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_KEY).apply()
        setProviderStatus(AiProviderType.GEMINI, ProviderConnectionStatus.NOT_CONFIGURED)
    }

    fun getGeminiModelId(): String {
        return prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
    }

    fun saveGeminiModelId(modelId: String) {
        val model = modelId.trim().ifEmpty { DEFAULT_GEMINI_MODEL }
        prefs.edit().putString(KEY_GEMINI_MODEL, model).apply()
    }

    // OmniRoute
    fun saveOmniRouteApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            clearOmniRouteApiKey()
        } else {
            val encrypted = encrypt(apiKey.trim())
            prefs.edit().putString(KEY_OMNIROUTE_KEY, encrypted).apply()
        }
    }

    fun getOmniRouteApiKey(): String? {
        val encrypted = prefs.getString(KEY_OMNIROUTE_KEY, null) ?: return null
        return decrypt(encrypted)
    }

    fun hasOmniRouteApiKey(): Boolean {
        return !prefs.getString(KEY_OMNIROUTE_KEY, null).isNullOrBlank()
    }

    fun clearOmniRouteApiKey() {
        prefs.edit().remove(KEY_OMNIROUTE_KEY).apply()
        setProviderStatus(AiProviderType.OMNIROUTE, ProviderConnectionStatus.NOT_CONFIGURED)
    }

    fun getOmniRouteModelId(): String {
        return prefs.getString(KEY_OMNIROUTE_MODEL, DEFAULT_OMNIROUTE_MODEL) ?: DEFAULT_OMNIROUTE_MODEL
    }

    fun saveOmniRouteModelId(modelId: String) {
        val model = modelId.trim().ifEmpty { DEFAULT_OMNIROUTE_MODEL }
        prefs.edit().putString(KEY_OMNIROUTE_MODEL, model).apply()
    }

    fun getOmniRouteBaseUrl(): String {
        return prefs.getString(KEY_OMNIROUTE_BASE_URL, DEFAULT_OMNIROUTE_BASE_URL) ?: DEFAULT_OMNIROUTE_BASE_URL
    }

    fun saveOmniRouteBaseUrl(baseUrl: String) {
        val url = baseUrl.trim().ifEmpty { DEFAULT_OMNIROUTE_BASE_URL }
        prefs.edit().putString(KEY_OMNIROUTE_BASE_URL, url).apply()
    }

    // Provider Connection Status
    fun getProviderStatus(type: AiProviderType): ProviderConnectionStatus {
        val raw = prefs.getString(KEY_STATUS_PREFIX + type.name, null)
            ?: return if (hasApiKey(type)) ProviderConnectionStatus.CONFIGURATION_CHANGED else ProviderConnectionStatus.NOT_CONFIGURED
        return try {
            ProviderConnectionStatus.valueOf(raw)
        } catch (_: Exception) {
            ProviderConnectionStatus.NOT_CONFIGURED
        }
    }

    fun setProviderStatus(type: AiProviderType, status: ProviderConnectionStatus) {
        prefs.edit().putString(KEY_STATUS_PREFIX + type.name, status.name).apply()
    }

    fun hasApiKey(type: AiProviderType): Boolean {
        return when (type) {
            AiProviderType.GEMINI -> hasGeminiApiKey()
            AiProviderType.OMNIROUTE -> hasOmniRouteApiKey()
        }
    }

    fun sanitizeError(error: Throwable): String {
        val message = error.message ?: "An unexpected connection error occurred"
        // Redact any potential keys or sensitive tokens that might appear in URLs or error bodies
        return message
            .replace(Regex("key=[a-zA-Z0-9_\\-]+"), "key=REDACTED")
            .replace(Regex("Bearer\\s+[a-zA-Z0-9_\\-\\.]+"), "Bearer REDACTED")
            .replace(Regex("x-goog-api-key:\\s*[a-zA-Z0-9_\\-]+"), "x-goog-api-key: REDACTED")
    }

    companion object {
        private const val PREFS_NAME = "eva_ai_secure_config"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "eva_ai_config_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_ACTIVE_PROVIDER = "key_active_provider"
        private const val KEY_GEMINI_KEY = "key_gemini_api_key_enc"
        private const val KEY_GEMINI_MODEL = "key_gemini_model_id"
        private const val KEY_OMNIROUTE_KEY = "key_omniroute_api_key_enc"
        private const val KEY_OMNIROUTE_MODEL = "key_omniroute_model_id"
        private const val KEY_OMNIROUTE_BASE_URL = "key_omniroute_base_url"
        private const val KEY_STATUS_PREFIX = "key_provider_status_"

        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
        const val DEFAULT_OMNIROUTE_MODEL = "omni-default"
        const val DEFAULT_OMNIROUTE_BASE_URL = "https://api.omniroute.ai/v1/"
    }
}
