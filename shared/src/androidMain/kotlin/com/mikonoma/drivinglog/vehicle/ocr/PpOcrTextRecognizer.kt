package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.OnnxPpOcrModels
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.PpOcr
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * PaddleOCR's PP-OCR models on ONNX Runtime (`add-seven-segment-ocr`): the recognizer that reads seven-segment LCD digits, which ML Kit
 * cannot. [loadModel] gives a model file's bytes by name (the app's assets); the models are loaded on the first scan, not at start-up.
 */
class PpOcrTextRecognizer(
    private val loadModel: (String) -> ByteArray,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) : TextRecognizer {

    private val models by lazy {
        OnnxPpOcrModels(
            loadModel(DETECTION_MODEL),
            loadModel(RECOGNITION_MODEL),
            OnnxPpOcrModels.characters(loadModel(CHARACTERS).decodeToString()),
            // ONNX Runtime's worker threads spin-wait between operators by default, which on a phone competes with ML Kit, the UI and
            // the garbage collector for the same cores: measured on the emulator, a scan took 3.6 s spinning and 0.66 s not (design.md).
            threads = Runtime.getRuntime().availableProcessors().coerceAtMost(4),
            spinning = false,
        )
    }

    override val isAvailable: Boolean = true

    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? = withContext(dispatcher) {
        val bitmap = decodeForRecognition(bytes) ?: return@withContext null
        PpOcr.recognize(bitmap.toRgbImage(), models)
    }

    override suspend fun recognize(frame: RgbImage): RecognizedPhoto? = withContext(dispatcher) { PpOcr.recognize(frame, models) }

    companion object {
        const val DETECTION_MODEL = "PP-OCRv6_det_tiny.onnx"
        const val RECOGNITION_MODEL = "en_PP-OCRv5_rec_mobile.onnx"
        const val CHARACTERS = "en_PP-OCRv5_rec_mobile.characters.txt"
    }
}
