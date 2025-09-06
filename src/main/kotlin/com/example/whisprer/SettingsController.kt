package com.example.whisprer

import com.example.whisprer.util.Env
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.TextField
import javafx.stage.Stage

/**
 * A controller class responsible for managing the settings view and handling user interactions
 * for saving and retrieving configuration settings, such as the API key.
 *
 * This class is connected to an FXML view and uses the `@FXML` annotation to bind UI components
 * and event-handling methods. It provides functionality for initializing the view with existing
 * settings and saving user-provided settings persistently.
 */
class SettingsController {

    @FXML
    private lateinit var apiKeyField: TextField

    /**
     * Initializes the settings view by attempting to populate the API key field with
     * an existing key retrieved from the environment configuration. If no key is found,
     * logs a message indicating that no existing API key is available.
     *
     * This method is automatically invoked by the JavaFX framework during the controller's
     * initialization process.
     *
     * @throws IllegalStateException if the environment variable `WHISPRER_API_KEY` is not set.
     */
    @FXML
    fun initialize() {
        try {
            val existingKey = Env.getOrThrow("WHISPRER_API_KEY")
            apiKeyField.text = existingKey
        } catch (e: Exception) {
            println("No existing API key found")
        }
    }

    /**
     * Saves the entered API key into the application's environment configuration.
     *
     * This method retrieves the text from the `apiKeyField`, trims whitespace, and verifies
     * that it is not empty. If the API key is valid, it is stored in the environment using
     * the `WHISPRER_API_KEY` key. Upon successful saving, a success alert is displayed,
     * and the window containing the `apiKeyField` is closed.
     *
     * If an exception occurs while saving the API key, an error alert is displayed with
     * the corresponding error message. Additionally, if the API key is empty, the method
     * displays an alert indicating that the key cannot be empty.
     *
     * The method utilizes the utility method `showAlert` to provide feedback to the user
     * and interacts with the `apiKeyField` for input handling.
     */
    @FXML
    fun saveApiKey() {
        val apiKey = apiKeyField.text.trim()
        if (apiKey.isNotEmpty()) {
            try {
                Env.set("WHISPRER_API_KEY", apiKey)

                showAlert("Success", "API Key saved successfully!")

                (apiKeyField.scene.window as? Stage)?.close()
            } catch (e: Exception) {
                showAlert("Error", "Failed to save API Key: ${e.message}", Alert.AlertType.ERROR)
            }
        } else {
            showAlert("Error", "API Key cannot be empty!", Alert.AlertType.ERROR)
        }
    }

    /**
     * Displays an alert dialog with the specified title, message, and alert type.
     *
     * The alert dialog is modal and requires the user to acknowledge it before
     * proceeding. The dialog's header is omitted, focusing solely on the content
     * text and title.
     *
     * @param title The title of the alert dialog.
     * @param message The message to display in the alert content.
     * @param type The type of alert to display, such as INFORMATION, WARNING, or ERROR. Defaults to INFORMATION.
     */
    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.INFORMATION) {
        Alert(type).apply {
            this.title = title
            headerText = null
            contentText = message
        }.showAndWait()
    }
}