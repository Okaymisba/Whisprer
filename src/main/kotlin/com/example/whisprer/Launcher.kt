package com.example.whisprer

import com.example.whisprer.service.TranscriptionService
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import javafx.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    val logger = Logger.getLogger(GlobalScreen::class.java.`package`.name)
    logger.level = Level.WARNING

    logger.useParentHandlers = false

    Thread {
        Application.launch(Whisprer::class.java)
    }.start()

    setupGlobalShortcut()
}

private val transcriptionService = TranscriptionService()
private val audioRecorder = AudioRecorder()
private var audioFile: File? = null

fun setupGlobalShortcut() {
    try {
        GlobalScreen.registerNativeHook()

        val keyListener = object : NativeKeyListener {
            private var isRecording = false

            override fun nativeKeyPressed(event: NativeKeyEvent) {
                if ((event.modifiers and (NativeKeyEvent.CTRL_MASK or NativeKeyEvent.ALT_MASK or NativeKeyEvent.SHIFT_MASK) != 0) && event.keyCode == NativeKeyEvent.VC_P) {
                    if (!isRecording) {
                        startRecording()
                    } else {
                        stopRecordingAndTranscribe()
                    }
                    isRecording = !isRecording
                }
            }

            override fun nativeKeyReleased(event: NativeKeyEvent) {}
            override fun nativeKeyTyped(event: NativeKeyEvent) {}
        }

        GlobalScreen.addNativeKeyListener(keyListener)

        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                GlobalScreen.unregisterNativeHook()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        println("✅ Global shortcut listener is active. Press Ctrl+Alt+Shift+P to toggle recording.")

    } catch (e: NativeHookException) {
        System.err.println("❌ Failed to register native hook: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    } catch (e: Exception) {
        System.err.println("❌ An error occurred: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

private fun startRecording() {
    println("\n🎤 Recording started... (Press Ctrl+Alt+Shift+P to stop)")
    try {
        audioRecorder.startRecording()
    } catch (e: Exception) {
        println("❌ Failed to start recording: ${e.message}")
        e.printStackTrace()
    }
}

private fun stopRecordingAndTranscribe() {
    println("\n⏹️ Stopping recording and starting transcription...")
    try {
        val outputFile = File("recording_${System.currentTimeMillis()}.wav")
        audioRecorder.stopRecording(outputFile)

        processAudioFile(outputFile)
    } catch (e: Exception) {
        println("❌ Failed to stop recording or process transcription: ${e.message}")
        e.printStackTrace()
    }
}

private fun processAudioFile(audioFile: File) {
    println("🔍 Processing audio file: ${audioFile.name}")

    if (!transcriptionService.isApiKeySet()) {
        println("❌ API key not set. Please configure it in the settings.")
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = transcriptionService.transcribeAudio(audioFile)

            result.onSuccess { transcript ->
                val selection = StringSelection(transcript)
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, null)
                println("✅ Transcription complete! Result copied to clipboard.")
            }.onFailure { error ->
                System.err.println("❌ Transcription failed: ${error.message}")
                error.printStackTrace()
            }
        } catch (e: Exception) {
            System.err.println("❌ Error during transcription: ${e.message}")
            e.printStackTrace()
        } finally {
            // Clean up the audio file
            try {
                if (audioFile.exists()) {
                    audioFile.delete()
                    println("🗑️ Deleted temporary audio file: ${audioFile.name}")
                }
            } catch (e: Exception) {
                System.err.println("⚠️ Failed to delete temporary audio file: ${e.message}")
            }
        }
    }
}