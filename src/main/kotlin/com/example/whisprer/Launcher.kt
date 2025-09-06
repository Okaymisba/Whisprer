package com.example.whisprer

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import javafx.application.Application
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    // Configure JNativeHook logging
    val logger = Logger.getLogger(GlobalScreen::class.java.`package`.name)
    logger.level = Level.WARNING

    // Remove default console handler to prevent unwanted output
    logger.useParentHandlers = false

    // Start the JavaFX application in a separate thread
    Thread {
        Application.launch(Whisprer::class.java)
    }.start()

    // Setup the global shortcut listener in the main thread
    setupGlobalShortcut()
}

// Function to set up global key listener for recording shortcut
fun setupGlobalShortcut() {
    try {
        // Set up the global screen
        GlobalScreen.registerNativeHook()

        // Create the key listener
        val keyListener = object : NativeKeyListener {
            private var isRecording = false

            override fun nativeKeyPressed(event: NativeKeyEvent) {
                // Check for Ctrl + Alt + Shift + P
                if ((event.modifiers and (NativeKeyEvent.CTRL_MASK or NativeKeyEvent.ALT_MASK or NativeKeyEvent.SHIFT_MASK) != 0) && event.keyCode == NativeKeyEvent.VC_P) {
                    if (!isRecording) {
                        startRecording()
                    } else {
                        stopRecordingAndProcess()
                    }
                    isRecording = !isRecording
                }
            }

            override fun nativeKeyReleased(event: NativeKeyEvent) {}
            override fun nativeKeyTyped(event: NativeKeyEvent) {}
        }

        // Add the key listener
        GlobalScreen.addNativeKeyListener(keyListener)

        // Add shutdown hook
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

// Function to start recording
private fun startRecording() {
    println("\n🎤 Recording started... (Press Ctrl+Alt+Shift+P to stop)")
    // TODO: Add your actual recording logic here
}

// Function to stop recording, process the recording, and copy the result to the clipboard
private fun stopRecordingAndProcess() {
    println("\n⏹️ Recording stopped. Processing...")

    try {
        // Simulate processing
        Thread.sleep(1000)

        val result = "Sample transcription result. Replace with actual implementation."

        // Copy to clipboard
        val selection = StringSelection(result)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, null)

        println("✅ Processing complete! Result copied to clipboard.")
    } catch (e: Exception) {
        System.err.println("❌ Error processing recording: ${e.message}")
        e.printStackTrace()
    }
}
