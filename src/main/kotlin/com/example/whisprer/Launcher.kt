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
import kotlin.system.exitProcess

/**
 * The entry point for the application that initializes and starts the main activities.
 *
 * This method performs the following:
 * - Configures a logger to suppress default global screen warnings by limiting the log level to `Level.WARNING`.
 * - Launches the `Whisprer` JavaFX application in a separate thread, which provides a voice-to-text transcription interface.
 * - Sets up a global shortcut listener using the `setupGlobalShortcut` function. The shortcut allows users to toggle
 *   recording and transcription functionalities with a specific key combination.
 */
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

/**
 * Sets up a global keyboard shortcut listener for toggling a recording feature.
 *
 * The method initializes the native hook for global key event listening, defines
 * a key listener for the specific combination of `Ctrl + Alt + Shift + P` to toggle
 * the recording state, and manages resource cleanup during application shutdown.
 *
 * Key behavior:
 * - When the shortcut `Ctrl + Alt + Shift + P` is pressed:
 *   - If recording is not active, it starts recording.
 *   - If recording is active, it stops recording and initiates transcription.
 *
 * The function ensures proper resource handling by unregistering the
 * native hook upon application shutdown using a shutdown hook. Error handling
 * is implemented to provide meaningful feedback in case of failure during setup
 * or execution.
 *
 * Throws:
 * - `NativeHookException`: If the native hook cannot be registered.
 * - General exceptions encountered during initialization or runtime.
 */
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
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println("❌ An error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

/**
 * Initiates the recording process for capturing audio input.
 *
 * The method begins recording audio data by utilizing an audio recorder instance.
 * If the operation is successful, a message is logged to indicate the start of recording.
 * In case of an error, an appropriate exception message is displayed, and the stack trace is printed.
 *
 * This method is intended to be used to start an audio recording session and relies on the
 * `audioRecorder` object for handling the recording logic.
 *
 * It is expected to handle issues such as unavailable audio devices or other runtime exceptions
 * without disrupting the application flow. Such errors are logged for troubleshooting purposes.
 */
private fun startRecording() {
    println("\n🎤 Recording started... (Press Ctrl+Alt+Shift+P to stop)")
    try {
        audioRecorder.startRecording()
    } catch (e: Exception) {
        println("❌ Failed to start recording: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Stops the ongoing audio recording, saves the recorded audio to a file, and initiates transcription.
 *
 * This method uses `audioRecorder` to stop the recording and save the audio file
 * in the WAVE format. Once the recording is successfully stopped, it processes
 * the saved audio file for transcription. If an error occurs during any of these
 * operations, the exception is logged.
 *
 * The transcription process is handled using `processAudioFile`, which performs
 * asynchronous transcription and manages the lifecycle of the audio file
 * (cleanup upon completion).
 *
 * Errors during recording or processing result in exception handling and detailed logs.
 */
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

/**
 * Processes the provided audio file by transcribing its content using the transcription service.
 * If the transcription is successful, the result is copied to the system clipboard.
 *
 * @param audioFile The audio file to be processed. Must*/
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