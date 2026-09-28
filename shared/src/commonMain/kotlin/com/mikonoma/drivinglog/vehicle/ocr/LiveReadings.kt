package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** One analyzed camera frame of the live scanner (`add-live-scanner`): the upright image and every number detected in it. */
class LiveFrame(val image: RgbImage, val detections: List<Detection>)

/**
 * A reading shown in the live scanner: [id] stays the same while it is tracked from frame to frame; [detection] is how it was read
 * in [frame], the frame it was last seen in, which is what a tap keeps.
 */
data class LiveReading(val id: Long, val detection: Detection, val frame: LiveFrame, val lastSeen: Instant)

/**
 * The readings the live scanner shows, merged frame by frame (design.md, "Readings are tracked across frames"). A candidate found
 * at the place of a shown reading (overlapping boxes, or the same value close by on the same row) is that reading, taking the
 * newest value and box; a new one appears; one not found again stays for [grace] after it was last seen, so a reading a frame
 * misses does not flicker off and on.
 */
class LiveReadings(private val clock: Clock, private val grace: Duration = DEFAULT_GRACE) {
    private var nextId = 1L

    var shown: List<LiveReading> = emptyList()
        private set

    /** Merges [frame]'s candidates into [shown] and returns it. */
    fun update(frame: LiveFrame): List<LiveReading> {
        val now = clock.now()
        val unmatched = shown.toMutableList()
        val next = mutableListOf<LiveReading>()
        for (d in frame.detections.candidates()) {
            val i = unmatched.indexOfFirst { samePlace(it.detection.box, d.box) || (it.detection.value == d.value && sameRowNearby(it.detection.box, d.box)) }
            val id = if (i >= 0) unmatched.removeAt(i).id else nextId++
            next += LiveReading(id, d, frame, now)
        }
        next += unmatched.filter { now - it.lastSeen <= grace }
        shown = next
        return shown
    }

    /** Readings left over from frames too old to show: those past [grace], without a new frame to replace them. */
    fun expire(): List<LiveReading> {
        val now = clock.now()
        shown = shown.filter { now - it.lastSeen <= grace }
        return shown
    }

    companion object {
        val DEFAULT_GRACE: Duration = 1.5.seconds
    }
}
