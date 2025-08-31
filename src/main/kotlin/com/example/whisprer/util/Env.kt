package com.example.whisprer.util

import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv = dotenv {
        directory = "./"
        ignoreIfMissing = true
    }

    fun get(key: String, default: String = ""): String {
        return dotenv[key, default].ifEmpty {
            System.getenv(key) ?: default
        }
    }

    fun getOrThrow(key: String): String {
        return dotenv[key] ?: System.getenv(key)
            ?: throw IllegalStateException("Missing required environment variable: $key")
    }
}
