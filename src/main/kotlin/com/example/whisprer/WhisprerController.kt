package com.example.whisprer

import com.example.whisprer.service.TranscriptionService
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * Controls the main interface for the Whisprer application, enabling functionality such as audio recording,
 * transcription, clipboard copying, and accessing settings.
 *
 * This class manages the UI interactions and integrates with the underlying components like audio recording
 * and transcription services. It is designed to provide a seamless user experience for audio-to-text transcription.
 *
 * Key Responsibilities:
 * 1. Manage UI components and their states (e.g., buttons, labels, text areas).
 * 2. Handle user-triggered actions like starting/stopping recording and copying transcriptions.
 * 3. Integrate with audio recording and transcription services.
 * 4. Display status updates and error messages to the user.
 * 5. Offer functionality to open additional views, such as settings.
 */
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

    /**
     * Handles the action when the record button is clicked.
     *
     * Toggles the recording state of the application. If recording is not active, initiates
     * the recording process by calling `startRecording()`. If recording is already active,
     * stops the recording process by calling `stopRecording()` and triggers processing of
     * the recorded audio.
     *
     * This method is bound to the record button in the application's UI via the `@FXML` annotation.
     */
    @FXML
    fun onRecordButtonClick() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    /**
     * Initiates the recording process by setting the application state to "recording".
     *
     * This method updates the UI components to reflect the active recording state. It changes the appearance
     * of the record button to indicate that recording is in progress and updates the status label to display
     * a "Recording..." message. The audio recording process itself is started by delegating the task to
     * the associated `audioRecorder`.
     *
     * Ensures the application transitions into a recording state with appropriate visual and functional behavior.
     */
    private fun startRecording() {
        isRecording = true
        (recordButton.graphic as? Text)?.text = "⏹"
        recordButton.style =
            "-fx-background-radius: 50%; -fx-min-width: 100; -fx-min-height: 100; -fx-max-width: 100; -fx-max-height: 100; -fx-background-color: #e74c3c; -fx-cursor: hand;"
        statusLabel.text = "Recording..."
        audioRecorder.startRecording()
    }

    /**
     * Stops the ongoing audio recording process and transitions the application out of the recording state.
     *
     * This method performs the following actions:
     * - Sets the recording state to false, disabling further audio capture.
     * - Updates the user interface to reflect that recording has stopped, including changes
     *   to the record button's appearance and status label's text.
     * - Saves the recorded audio data to a file in WAV format, using a timestamp-based name for the file.
     * - Delegates further processing of the audio file to the `processAudioFile` method.
     *
     * Ensures that the application stops recording gracefully and handles the audio file appropriately
     * for further operations, such as transcription.
     */
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

    /**
     * Processes the given audio file by transcribing its content and updating the user interface accordingly.
     *
     * The method utilizes a coroutine for background processing. It transcribes the given audio file in an
     * I/O-bound coroutine context. The transcription result is either displayed on the UI and copied to the
     * clipboard upon success, or an error is shown to the user upon failure.
     *
     * @param audioFile The audio file to be transcribed. Must be a valid, existing file.
     */
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

    /**
     * Copies the text from the transcript area to the system clipboard.
     *
     * If the text in the `transcriptArea` is not blank, this method copies the content
     * to the system clipboard using the `copyToClipboard` helper method. Upon successfully
     * copying the text, it displays a confirmation alert to the user via the `showAlert`
     * method.
     *
     * This method is intended to be triggered by an event, such as a button click in the UI,
     * and is associated with the `@FXML` annotation for JavaFX applications.
     */
    @FXML
    fun onCopyToClipboard() {
        if (transcriptArea.text.isNotBlank()) {
            copyToClipboard(transcriptArea.text)
            showAlert("Success", "Text copied to clipboard!")
        }
    }

    /**
     * Clears the text content within the transcript area.
     *
     * This method is typically triggered by a user action, such as clicking a "Clear" button
     * in the application's UI. It resets the `transcriptArea` by removing all text, leaving
     * it blank. The primary purpose is to provide a quick way for users to clear any
     * transcribed or manually entered text.
     */
    @FXML
    fun onClearText() {
        transcriptArea.clear()
    }

    /**
     * Copies the provided text to the system clipboard.
     *
     * This method takes a string as input and places it on the system clipboard,
     * allowing the text to be accessible for further pasting operations in other
     * applications. It utilizes the `Toolkit` system clipboard and a `StringSelection`
     * object to transfer the text.
     *
     * @param text The text to be copied to the clipboard. Must be a non-null string value.
     */
    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }

    /**
     * Displays an error message to the user in the form of an alert dialog.
     *
     * This method creates and shows an alert dialog with the specified error title
     * and message. The alert dialog is of type `ERROR` and does not include a header.
     * The method pauses execution until the user closes the alert dialog.
     *
     * @param title The title of the error message to be displayed.
     * @param message The content of the error message to be displayed.
     */
    private fun showError(title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    /**
     * Displays an informational alert dialog with the specified title and message.
     *
     * This method creates an alert dialog of type `INFORMATION` with no header text.
     * The dialog pauses execution until the user closes it.
     *
     * @param title The title text for the alert dialog.
     * @param message The content text to be displayed within the dialog.
     */
    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    /**
     * Handles the cleanup process when the application is closing.
     *
     * This method is invoked as part of the application's shutdown routine. It cancels any
     * ongoing coroutines by calling `cancel()` on the `coroutineScope` associated with the
     * `WhisprerController` class, ensuring proper resource management and preventing potential
     * memory leaks or unfinished tasks.
     *
     * Designed to be executed when the application window receives a close request, ensuring
     * that all background operations are terminated gracefully.
     */
    fun onClose() {
        coroutineScope.cancel()
    }

    /**
     * Opens the settings window for the application.
     *
     * This method is responsible for displaying a modal settings window,
     * allowing users to configure application preferences. It loads the
     * settings view from the corresponding FXML file, initializes a new
     * JavaFX stage and sets it up as a modal dialog with a specified
     * title, size and scene.
     *
     * The window remains focused until the user closes it.
     *
     * Any exceptions occurring during the loading or display of the settings
     * window are caught and printed to the console as debug information.
     */
    @FXML
    fun openSettingsWindow() {
        try {
            // Load the settings view FXML file
            val fxmlLoader = FXMLLoader(javaClass.getResource("/com/example/whisprer/settings-view.fxml"))
            val root: Parent = fxmlLoader.load()

            // Set up a new stage for the settings window
            val stage = Stage()
            stage.title = "Settings"
            stage.scene = Scene(root, 300.0, 200.0)
            stage.initModality(Modality.APPLICATION_MODAL) // Makes the window modal
            stage.showAndWait()
        } catch (e: Exception) {
            e.printStackTrace() // Debugging error handling
        }
    }

}
