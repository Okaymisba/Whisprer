package com.example.whisprer.util

import java.util.prefs.Preferences

object Env {
    private val prefs: Preferences = Preferences.userNodeForPackage(Env::class.java)

    fun getOrThrow(key: String): String {
        return prefs.get(key, null)
            ?: throw IllegalStateException("Environment variable $key is not set. Please set it using Env.set()")
    }

    fun set(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush() // Ensure the preferences are written to disk immediately
    }
}
