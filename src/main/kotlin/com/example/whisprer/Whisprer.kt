package com.example.whisprer

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Whisprer::class.java, *args)
        }
    }
}