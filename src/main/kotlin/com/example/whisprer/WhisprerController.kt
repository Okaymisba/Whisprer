package com.example.whisprer

import com.example.whisprer.service.TranscriptionService
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

class WhisprerController {
    @FXML
    private lateinit var recordButton: Button

    @FXML
    private lateinit var statusLabel: Label

    @FXML
    private lateinit var transcriptArea: TextArea

    @FXML
    private lateinit var root: VBox

    private var isRecording = false
    private val audioRecorder = AudioRecorder()
    private val transcriptionService = TranscriptionService()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    @FXML
    fun initialize() {
        // Initialization code if needed
    }

    @FXML
    fun onRecordButtonClick() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        isRecording = true
        (recordButton.graphic as? Text)?.text = "⏹"
        recordButton.style =
            "-fx-background-radius: 50%; -fx-min-width: 100; -fx-min-height: 100; -fx-max-width: 100; -fx-max-height: 100; -fx-background-color: #e74c3c; -fx-cursor: hand;"
        statusLabel.text = "Recording..."
        audioRecorder.startRecording()
    }

    private fun stopRecording() {
        isRecording = false
        (recordButton.graphic as? Text)?.text = "🎙"
        recordButton.style =
            "-fx-background-radius: 50%; -fx-min-width: 100; -fx-min-height: 100; -fx-max-width: 100; -fx-max-height: 100; -fx-background-color: #3498db; -fx-cursor: hand;"
        statusLabel.text = "Processing audio..."

        val outputFile = File("recording_${System.currentTimeMillis()}.wav")
        audioRecorder.stopRecording(outputFile)

        processAudioFile(outputFile)
    }

    private fun processAudioFile(audioFile: File) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    transcriptionService.transcribeAudio(audioFile)
                }

                result.onSuccess { transcript ->
                    Platform.runLater {
                        transcriptArea.text = transcript
                        copyToClipboard(transcript)
                        statusLabel.text = "Transcription complete!"
                    }
                }.onFailure { error ->
                    Platform.runLater {
                        statusLabel.text = "Transcription failed"
                        showError("Transcription Error", "Failed to transcribe audio: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    statusLabel.text = "Error during transcription"
                    showError("Error", "An error occurred: ${e.message}")
                }
            }
        }
    }

    @FXML
    fun onCopyToClipboard() {
        if (transcriptArea.text.isNotBlank()) {
            copyToClipboard(transcriptArea.text)
            showAlert("Success", "Text copied to clipboard!")
        }
    }

    @FXML
    fun onClearText() {
        transcriptArea.clear()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }

    private fun showError(title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    fun onClose() {
        coroutineScope.cancel()
    }
}
