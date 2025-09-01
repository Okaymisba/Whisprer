package com.example.whisprer.service

import com.example.whisprer.util.Env
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable
data class TranscribeResponse(
    val transcript: String, val remainingCredits: Int
)

class TranscriptionService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        engine {
            requestTimeout = 60_000 // 60 seconds timeout
        }
    }

    private val apiKey = Env.getOrThrow("WHISPRER_API_KEY")
    private val supabaseUrl = Env.getOrThrow("SUPABASE_URL")
    private val supabaseAnonKey = Env.getOrThrow("SUPABASE_ANON_KEY")

    suspend fun transcribeAudio(audioFile: File): Result<String> {
        return try {
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.getEncoder().encodeToString(audioBytes)
            val format = audioFile.extension.ifEmpty { "webm" }

            // Create a raw JSON string for the request body
            val requestBody = """
                {
                    "audioBase64": "$audioBase64",
                    "audioFormat": "$format"
                }
            """.trimIndent()

            val response = client.post(supabaseUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $supabaseAnonKey")
                header("whisprer-api-key", apiKey.trim())
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val result = response.body<TranscribeResponse>()
                Result.success(result.transcript)
            } else {
                Result.failure(Exception("Transcription failed: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error during transcription: ${e.message}"))
        }
    }
}
