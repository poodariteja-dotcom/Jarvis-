package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

enum class RetentionPeriod(val label: String, val millis: Long) {
    ONE_DAY("24 Hours", 24L * 60 * 60 * 1000),
    SEVEN_DAYS("7 Days", 7L * 24 * 60 * 60 * 1000),
    THIRTY_DAYS("30 Days", 30L * 24 * 60 * 60 * 1000),
    FOREVER("Forever", -1L)
}

class JarvisPreferences(private val context: Context) {
    companion object {
        val KEY_RETENTION_PERIOD = stringPreferencesKey("retention_period")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_WAKE_WORD_PHRASE = stringPreferencesKey("wake_word_phrase")
        val KEY_CONTINUOUS_LISTENING = booleanPreferencesKey("continuous_listening")
        val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val KEY_USER_HONORIFIC = stringPreferencesKey("user_honorific")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_RATE = floatPreferencesKey("voice_rate")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak_response")
    }

    val retentionPeriod: Flow<RetentionPeriod> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RETENTION_PERIOD] ?: RetentionPeriod.SEVEN_DAYS.name
        try {
            RetentionPeriod.valueOf(raw)
        } catch (e: Exception) {
            RetentionPeriod.SEVEN_DAYS
        }
    }

    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_ENABLED] ?: true
    }

    val wakeWordPhrase: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_PHRASE] ?: "Hey Jarvis"
    }

    val continuousListening: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTINUOUS_LISTENING] ?: false
    }

    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK] ?: false
    }

    val userHonorific: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_HONORIFIC] ?: "Sir"
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Tony"
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_API_KEY] ?: ""
    }

    val voicePitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_PITCH] ?: 0.95f // Slightly deeper, refined Jarvis tone
    }

    val voiceRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_RATE] ?: 1.05f // Crisp, articulate delivery
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SPEAK] ?: true
    }

    suspend fun setRetentionPeriod(period: RetentionPeriod) {
        context.dataStore.edit { it[KEY_RETENTION_PERIOD] = period.name }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setWakeWordPhrase(phrase: String) {
        context.dataStore.edit { it[KEY_WAKE_WORD_PHRASE] = phrase }
    }

    suspend fun setContinuousListening(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CONTINUOUS_LISTENING] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setUserHonorific(honorific: String) {
        context.dataStore.edit { it[KEY_USER_HONORIFIC] = honorific }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[KEY_CUSTOM_API_KEY] = key }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.dataStore.edit { it[KEY_VOICE_PITCH] = pitch }
    }

    suspend fun setVoiceRate(rate: Float) {
        context.dataStore.edit { it[KEY_VOICE_RATE] = rate }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAutoSpeak(autoSpeak: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPEAK] = autoSpeak }
    }
}
