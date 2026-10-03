package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlin.time.Clock

/**
 * One opening of the live scanner (`add-live-scanner`): each camera frame is recognized, classified by [classify] (the form's own
 * rule: `detectReadings` against the known odometer on the log event form, `detectInitialOdometer` when adding a vehicle) and merged
 * into the readings shown. A new instance each time the scanner opens starts with nothing shown.
 */
class LiveScanner(private val recognizer: TextRecognizer, clock: Clock, private val classify: (RecognizedPhoto) -> List<Detection>) {
    private val readings = LiveReadings(clock)

    /** The readings to show after [frame] (upright); a frame that cannot be read changes only what has expired. */
    suspend fun analyze(frame: RgbImage): List<LiveReading> {
        val photo = recognizer.recognize(frame) ?: return readings.expire()
        return readings.update(LiveFrame(frame, classify(photo)))
    }
}
