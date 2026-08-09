package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference
import java.security.MessageDigest

class SecurePinPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val HAS_PIN        = booleanPreferencesKey("secure_has_pin")
        val PIN_HASH       = stringPreferencesKey("secure_pin_hash")
        val PIN_LENGTH     = intPreferencesKey("secure_pin_length")
        val BIOMETRIC_ON   = booleanPreferencesKey("secure_biometric")
        val AUTO_LOCK_MIN  = intPreferencesKey("secure_auto_lock_minutes")
        val FAILED_ATTEMPTS= intPreferencesKey("secure_failed_attempts")
        val LOCKED_UNTIL   = longPreferencesKey("secure_locked_until")
        val SORT_BY        = stringPreferencesKey("secure_sort_by")
        val SORT_ASCENDING = booleanPreferencesKey("secure_sort_ascending")
        val LAYOUT_MODE    = stringPreferencesKey("secure_layout_mode")
    }

    val hasPin           = pref(HAS_PIN, false)
    val pinHash          = pref(PIN_HASH, "")
    val pinLength        = pref(PIN_LENGTH, 4)
    val biometricEnabled = pref(BIOMETRIC_ON, false)
    val autoLockMinutes  = pref(AUTO_LOCK_MIN, 5)
    val failedAttempts   = pref(FAILED_ATTEMPTS, 0)
    val lockedUntil      = pref(LOCKED_UNTIL, 0L)
    val sortBy           = pref(SORT_BY, "title")
    val sortAscending    = pref(SORT_ASCENDING, true)
    val layoutMode       = pref(LAYOUT_MODE, "list")

    fun hashPin(pin: String, salt: String = "FinalPlayer_SecureFolder"): String {
        val input = "$salt:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String): Boolean {
        val stored = pinHash.get()
        if (stored.isBlank()) return false
        return hashPin(inputPin) == stored
    }

    fun setPin(newPin: String) {
        pinHash.set(hashPin(newPin))
        pinLength.set(newPin.length)
        hasPin.set(true)
        failedAttempts.set(0)
        lockedUntil.set(0L)
    }

    fun recordFailedAttempt(): Boolean {
        val attempts = failedAttempts.get() + 1
        failedAttempts.set(attempts)
        return if (attempts >= 5) {
            // قفل لـ 30 ثانية
            lockedUntil.set(System.currentTimeMillis() + 30_000L)
            true
        } else false
    }

    fun isLocked(): Boolean =
        System.currentTimeMillis() < lockedUntil.get()

    fun remainingLockSeconds(): Long =
        ((lockedUntil.get() - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)

    fun resetFailedAttempts() {
        failedAttempts.set(0)
        lockedUntil.set(0L)
    }

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}
