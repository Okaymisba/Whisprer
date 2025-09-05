package com.example.whisprer.service

import com.example.whisprer.util.Env
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@Serializable
data class TranscribeResponse(
    val transcript: String, val remainingCredits: Int
)

class TranscriptionService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            requestTimeout = 60_000
        }
    }

    private val supabaseUrl = "https://acqfqnnnnhfotppqortg.supabase.co/functions/v1/transcribe-audio"
    private val supabaseAnonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFjcWZxbm5ubmhmb3RwcHFvcnRnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY2MzgyMDksImV4cCI6MjA3MjIxNDIwOX0.CfDV7O3l4MId1ZckxHMHCgI-44F-TQXFaf58I51whQM"

    private val apiKey: String? by lazy {
        runCatching { Env.getOrThrow("WHISPRER_API_KEY") }.getOrNull()
    }

    fun isApiKeySet(): Boolean = apiKey != null

    suspend fun transcribeAudio(audioFile: File): Result<String> {
        if (!isApiKeySet()) {
            return Result.failure(IllegalStateException("API key is not set. Please set it in the settings."))
        }

        return try {
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.getEncoder().encodeToString(audioBytes)
            val format = audioFile.extension.ifEmpty { "webm" }

            val requestBody = """
                {
                    "audioBase64": "$audioBase64",
                    "audioFormat": "$format"
                }
            """.trimIndent()

            val response = client.post(supabaseUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $supabaseAnonKey")
                header("whisprer-api-key", apiKey?.trim() ?: "")
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            println("Raw API Response: $responseBody")

            if (response.status.isSuccess()) {
                val result = Json.decodeFromString<TranscribeResponse>(responseBody)
                val cleanedTranscript = result.transcript.split(", ").map { it.trim() }.distinct().joinToString(" ")
                Result.success(cleanedTranscript)
            } else {
                Result.failure(Exception("Transcription failed: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                if (audioFile.exists()) {
                    audioFile.delete()
                }
            } catch (e: Exception) {
                println("Failed to delete audio file: ${e.message}")
            }
        }
    }
}
