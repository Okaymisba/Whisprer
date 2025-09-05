package com.example.whisprer

import com.example.whisprer.util.Env
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.TextField
import javafx.stage.Stage

class SettingsController {

    @FXML
    private lateinit var apiKeyField: TextField

    @FXML
    fun initialize() {
        // Load existing API key if it exists
        try {
            val existingKey = Env.getOrThrow("WHISPRER_API_KEY")
            apiKeyField.text = existingKey
        } catch (e: Exception) {
            // Key doesn't exist yet, which is fine
            println("No existing API key found")
        }
    }

    @FXML
    fun saveApiKey() {
        val apiKey = apiKeyField.text.trim()
        if (apiKey.isNotEmpty()) {
            try {
                // Save the API key to the environment
                Env.set("WHISPRER_API_KEY", apiKey)

                // Show success message
                showAlert("Success", "API Key saved successfully!")

                // Close the settings window
                (apiKeyField.scene.window as? Stage)?.close()
            } catch (e: Exception) {
                showAlert("Error", "Failed to save API Key: ${e.message}", Alert.AlertType.ERROR)
            }
        } else {
            showAlert("Error", "API Key cannot be empty!", Alert.AlertType.ERROR)
        }
    }

    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.INFORMATION) {
        Alert(type).apply {
            this.title = title
            headerText = null
            contentText = message
        }.showAndWait()
    }
}