package com.example.whisprer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.*

/**
 * Handles audio recording and saving functionality.
 *
 * AudioRecorder provides the ability to start recording audio using an available microphone,
 * stop the recording, and save the captured audio data to a file in WAVE format.
 *
 * The class internally manages the recording process through a background thread
 * and uses a target data line for capturing audio data from the input device.
 */
class AudioRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var isRecording = false
    private val audioBuffer = ByteArrayOutputStream()

    /**
     * Represents the configuration for the audio format used by the AudioRecorder.
     *
     * This configuration uses PCM_SIGNED encoding with the following properties:
     * - Sample rate: 44100 Hz
     * - Sample size: 16 bits
     * - Number of channels: 2 (stereo)
     * - Frame size: 4 bytes
     * - Frame rate: 44100 Hz
     * - Big-endian byte order: false (little-endian)
     *
     * These settings are used to initialize the audio format for recording and encoding audio data.
     */
    private val audioFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false
    )

    /**
     * Starts recording audio from the first available and compatible audio input device.
     *
     * The method checks if the recording is already in progress and exits early if so.
     * It identifies the suitable audio input device, opens a `TargetDataLine` on the device,
     * and begins recording audio data. The audio data is captured asynchronously in a separate thread
     * and written to an internal audio buffer.
     *
     * If recording fails or an exception is encountered, the method stops recording, closes any open
     * resources, and sets the recording state to false.
     *
     * Exceptional cases, such as the absence of a compatible audio device, are logged and handled to prevent
     * application crashes.
     */
    fun startRecording() {
        try {
            if (isRecording) {
                return
            }

            val mixerInfo = AudioSystem.getMixerInfo().firstOrNull { mixerInfo ->
                val mixer = AudioSystem.getMixer(mixerInfo)
                val lineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
                mixer.isLineSupported(lineInfo)
            } ?: throw IllegalStateException("No suitable audio input device found")

            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            targetDataLine = AudioSystem.getMixer(mixerInfo).getLine(info) as TargetDataLine

            targetDataLine?.apply {
                open(audioFormat)
                start()
            }

            isRecording = true
            audioBuffer.reset()

            Thread(Runnable {
                val buffer = ByteArray(4096)
                while (isRecording) {
                    val count = targetDataLine?.read(buffer, 0, buffer.size) ?: 0
                    if (count > 0) {
                        audioBuffer.write(buffer, 0, count)
                    }
                }
            }).start()

        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            targetDataLine?.close()
            targetDataLine = null
        }
    }

    /**
     * Stops the ongoing audio recording and writes the recorded audio data to the specified output file in WAVE format.
     *
     * The method ensures that the recording process is stopped, cleans up any resources
     * associated with the audio capture, and writes the captured audio data to the given file.
     * If no audio data has been recorded or an error occurs during the file writing process,
     * appropriate handling is performed.
     *
     * @param outputFile the file where the recorded audio data will be saved
     */
    fun stopRecording(outputFile: File) {
        try {
            if (!isRecording) {
                return
            }

            isRecording = false

            Thread.sleep(100)

            targetDataLine?.apply {
                stop()
                close()
            }

            val audioData = audioBuffer.toByteArray()

            if (audioData.isEmpty()) {
                return
            }

            val audioInputStream = AudioInputStream(
                ByteArrayInputStream(audioData), audioFormat, audioData.size.toLong() / audioFormat.frameSize
            )

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile)
            audioInputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioBuffer.reset()
            targetDataLine = null
        }
    }

//    /**
//     * Plays an audio file using the system's audio playback capabilities.
//     * The method synchronously plays the specified audio file and blocks the current thread
//     * until the audio playback is completed.
//     *
//     * @param audioFile the audio file to be played
//     */
//    private fun playAudio(audioFile: File) {
//        try {
//            val audioIn = AudioSystem.getAudioInputStream(audioFile)
//            val clip = AudioSystem.getClip()
//            clip.open(audioIn)
//            clip.start()
//
//            // Keep the application running while the clip is playing
//            Thread.sleep(clip.microsecondLength / 1000)
//            clip.close()
//            audioIn.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
}