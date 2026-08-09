package com.finalplayer.app.data.preferences.base

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class DataStorePreference<T>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T
) : Preference<T> {
    override fun get(): T = try {
        runBlocking(Dispatchers.IO) {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[key] ?: defaultValue }
                .first()
        }
    } catch (_: Exception) {
        defaultValue
    }

    override fun set(value: T) {
        try {
            runBlocking(Dispatchers.IO) {
                dataStore.edit { it[key] = value }
            }
        } catch (_: Exception) {
        }
    }

    override fun asFlow(): Flow<T> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[key] ?: defaultValue }

    override fun changes(): Flow<T> =
        asFlow().distinctUntilChanged()
}

