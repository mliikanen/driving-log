package com.mikonoma.drivinglog.vehicle.ocr.ppocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * The two PP-OCR models on ONNX Runtime (`add-seven-segment-ocr`), from their bytes, and the recognizer's [characters] (its character
 * list file, one per line). The list is shipped as a file rather than read from the model's metadata, which ONNX Runtime's Java API
 * hands over with characters outside the Basic Multilingual Plane garbled. [threads] is the number of threads an operator runs on (0
 * leaves it to ONNX Runtime) and [spinning] whether they spin-wait between operators. The same `ai.onnxruntime` API is ONNX Runtime's
 * Android library in the app and its JVM library in the host tests, so this class runs in both.
 */
class OnnxPpOcrModels(
    detectionModel: ByteArray,
    recognitionModel: ByteArray,
    override val characters: List<String>,
    private val threads: Int = 0,
    private val spinning: Boolean = true,
) : PpOcrModels, AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private fun options() = OrtSession.SessionOptions().apply {
        if (threads > 0) setIntraOpNumThreads(threads)
        if (!spinning) addConfigEntry("session.intra_op.allow_spinning", "0")
    }
    private val detection = env.createSession(detectionModel, options())
    private val recognition = env.createSession(recognitionModel, options())

    companion object {
        /** A character list file's characters: one per line, blank lines ignored. */
        fun characters(file: String): List<String> = file.split('\n').map { it.removeSuffix("\r") }.filter { it.isNotEmpty() }
    }

    override fun detect(input: FloatArray, width: Int, height: Int): FloatArray =
        run(detection, input, longArrayOf(1, 3, height.toLong(), width.toLong())) { it.floatBuffer.toArray() }

    override fun recognize(input: FloatArray, count: Int, width: Int): RecognitionScores =
        run(recognition, input, longArrayOf(count.toLong(), 3, PpOcr.REC_HEIGHT.toLong(), width.toLong())) { out ->
            val shape = out.info.shape
            RecognitionScores(out.floatBuffer.toArray(), shape[0].toInt(), shape[1].toInt(), shape[2].toInt())
        }

    private fun <T> run(session: OrtSession, input: FloatArray, shape: LongArray, read: (OnnxTensor) -> T): T =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { result -> read(result.get(0) as OnnxTensor) }
        }

    private fun FloatBuffer.toArray(): FloatArray = FloatArray(remaining()).also { get(it) }

    override fun close() {
        detection.close()
        recognition.close()
    }
}
