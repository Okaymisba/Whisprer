package com.example.whisprer

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

/**
 *
 * Main application class for the Whisprer application, which provides functionality
 * for voice-to-text transcription. This class sets up the primary stage and initializes
 * the application's UI using an FXML file.
 *
 * The application includes features for recording voice input, transcribing the audio,
 * and interacting with the user through a graphical interface.
 *
 * Inherits from the `Application` class to support the JavaFX application lifecycle.
 */
class Whisprer : Application() {
    override fun start(primaryStage: Stage) {
        // Load the FXML file
        val fxmlLoader = FXMLLoader(javaClass.getResource("/com/example/whisprer/whisprer-view.fxml"))
        val root: Parent = fxmlLoader.load()

        // Set up the stage
        primaryStage.title = "Whisprer - Voice to Text"
        primaryStage.scene = Scene(root, 500.00, 600.00)
        primaryStage.minWidth = 400.0
        primaryStage.minHeight = 600.0

        // Set up close request handler
        primaryStage.setOnCloseRequest {
            val controller = fxmlLoader.getController<WhisprerController>()
            controller.onClose()
        }

        primaryStage.show()
    }

    /**
     * Companion object providing the entry point for the Whisprer application.
     *
     * The `main` method serves as the starting point for launching the JavaFX application.
     * It initializes and invokes the application lifecycle by calling the `launch` method
     * with the `Whisprer` class reference and any supplied runtime arguments.
     */
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Whisprer::class.java, *args)
        }
    }
}