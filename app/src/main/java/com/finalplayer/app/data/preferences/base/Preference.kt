package com.finalplayer.app.data.preferences.base

import kotlinx.coroutines.flow.Flow

interface Preference<T> {
    fun get(): T
    fun set(value: T)
    fun asFlow(): Flow<T>
    fun changes(): Flow<T>
}
