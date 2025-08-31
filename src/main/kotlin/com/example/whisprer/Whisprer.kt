package com.example.whisprer

import com.example.whisprer.service.TranscriptionService
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

class Whisprer : Application() {
    private var isRecording = false
    private lateinit var recordButton: Button
    private lateinit var statusLabel: Label
    private val audioRecorder = AudioRecorder()
    private val transcriptionService = TranscriptionService()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun start(primaryStage: Stage) {
        val micText = Text("🎙")
        micText.font = Font.font(32.0)

        recordButton = Button().apply {
            graphic = micText
            style = """
                -fx-background-color: #2196F3;
                -fx-background-radius: 50%;
                -fx-min-width: 100px;
                -fx-min-height: 100px;
                -fx-padding: 0;
            """.trimIndent()
            setOnAction { onRecordButtonClick() }
        }

        statusLabel = Label("Click the microphone to start recording")
        statusLabel.style = "-fx-font-size: 14px; -fx-text-fill: #666;"

        val root = VBox(20.0, recordButton, statusLabel).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 20; -fx-background-color: #f5f5f5;"
        }

        primaryStage.scene = Scene(root, 300.0, 300.0)
        primaryStage.title = "Whisprer"
        primaryStage.show()
    }

    private fun onRecordButtonClick() {
        if (!isRecording) {
            // Start recording
            isRecording = true
            (recordButton.graphic as? Text)?.text = "⏹"
            statusLabel.text = "Recording..."
            recordButton.style = """
                -fx-background-color: #f44336;
                -fx-background-radius: 50%;
                -fx-min-width: 100px;
                -fx-min-height: 100px;
                -fx-padding: 0;
            """.trimIndent()
            audioRecorder.startRecording()
        } else {
            // Stop recording and process
            isRecording = false
            (recordButton.graphic as? Text)?.text = "🎙"
            recordButton.style = """
                -fx-background-color: #2196F3;
                -fx-background-radius: 50%;
                -fx-min-width: 100px;
                -fx-min-height: 100px;
                -fx-padding: 0;
            """.trimIndent()

            val outputFile = File("recording_${System.currentTimeMillis()}.wav")
            audioRecorder.stopRecording(outputFile)

            statusLabel.text = "Transcribing..."
            processAudioFile(outputFile)
        }
    }

    private fun processAudioFile(audioFile: File) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    transcriptionService.transcribeAudio(audioFile)
                }

                result.onSuccess { transcript ->
                    Platform.runLater {
                        // Copy to clipboard
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val selection = StringSelection(transcript)
                        clipboard.setContents(selection, null)

                        // Show confirmation message
                        statusLabel.text = "Transcription copied to clipboard!"

                        // Optional: Show a temporary notification
                        // val notification = Notification("Transcription Complete", "The transcription has been copied to your clipboard.")
                        // notification.show(Stage().scene.window)
                    }
                }.onFailure { error ->
                    statusLabel.text = "Transcription failed"
                    showError("Transcription Error", "Failed to transcribe audio: ${error.message}")
                }
            } catch (e: Exception) {
                statusLabel.text = "Error during transcription"
                showError("Error", "An error occurred: ${e.message}")
            }
        }
    }

    private fun showError(title: String, message: String) {
        Platform.runLater {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = title
            alert.headerText = null
            alert.contentText = message
            alert.showAndWait()
        }
    }

    override fun stop() {
        coroutineScope.cancel()
        super.stop()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Whisprer::class.java, *args)
        }
    }
}