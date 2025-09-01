package com.example.whisprer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.*

class AudioRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var isRecording = false
    private val audioBuffer = ByteArrayOutputStream()

    // Audio format configuration - using CD quality
    private val audioFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, 44100f,      // Sample rate (CD quality)
        16,          // Sample size in bits (16-bit for CD quality)
        2,           // Channels (2 for stereo)
        4,           // Frame size (2 bytes/sample * 2 channels)
        44100f,      // Frame rate (same as sample rate for PCM)
        false        // Little endian (false for little, true for big)
    )

    fun startRecording() {
        try {
            if (isRecording) {
                return
            }

            // Get the default mixer that supports the target line
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

            // Start a new thread for recording
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

    fun stopRecording(outputFile: File) {
        try {
            if (!isRecording) {
                return
            }

            isRecording = false

            // Give some time for the recording thread to finish
            Thread.sleep(100)

            targetDataLine?.apply {
                stop()
                close()
            }

            val audioData = audioBuffer.toByteArray()

            if (audioData.isEmpty()) {
                return
            }

            // Save as WAV file
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