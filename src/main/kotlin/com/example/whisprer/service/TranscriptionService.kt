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

/**
 * Represents the response object for a transcription request.
 *
 * @property transcript The transcribed text as a string result from the transcription API.
 * @property remainingCredits The number of remaining transcription credits available for the user.
 */
@Serializable
data class TranscribeResponse(
    val transcript: String, val remainingCredits: Int
)

/**
 * A service class responsible for handling voice-to-text transcription of audio files by interacting
 * with an external transcription API. This service reads audio files, encodes them in Base64, and
 * sends them for processing to the defined API endpoint.
 *
 * The class requires an API key to authenticate requests. The API key is retrieved from the environment
 * configuration or gracefully handled if not set.
 *
 * It handles errors, unexpected responses, and file cleanup post-transcription. The transcription
 * response is parsed, cleaned, and returned as a formatted text result.
 */
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

    /**
     * Transcribes the audio content from the provided audio file into text.
     *
     * @param audioFile The audio file to be transcribed. Must be a valid file with readable content.
     * @return A [Result] instance containing the transcribed text if successful, or an error if the transcription fails.
     */
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
