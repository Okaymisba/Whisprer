package com.example.whisprer.util

import java.util.prefs.Preferences

/**
 * Utility object for managing environment-like variables using the `Preferences` API.
 *
 * The `Env` object allows storing and retrieving key-value pairs persistently. It provides
 * utility methods to get the value associated with a key, or throw an exception if the key
 * is not set, as well as to set or update a key-value pair.
 *
 * The stored values are persisted using the `Preferences` API and are immediately written
 * to disk upon updates.
 */
object Env {
    private val prefs: Preferences = Preferences.userNodeForPackage(Env::class.java)

    /**
     * Retrieves the value associated with the specified key from the environment configuration.
     * If the key is not found, throws an IllegalStateException.
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the given key.
     * @throws IllegalStateException if the key is not set in the environment.
     */
    fun getOrThrow(key: String): String {
        return prefs.get(key, null)
            ?: throw IllegalStateException("Environment variable $key is not set. Please set it using Env.set()")
    }

    /**
     * Stores a key-value pair in the preferences and ensures the changes are
     * immediately written to disk.
     *
     * @param key The key of the preference to be stored.
     * @param value The value associated with the specified key.
     */
    fun set(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }
}
