package com.mikonoma.drivinglog.vehicle.picture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CropStateTest {

    private fun landscape() = CropState.initial(4000, 3000)
    private fun portrait() = CropState.initial(3000, 4000)
    private fun square() = CropState.initial(2000, 2000)

    private fun assertInside(state: CropState) {
        val r = state.rect()
        assertTrue(r.x >= 0 && r.y >= 0, "$r starts inside")
        assertTrue(r.x + r.side <= state.imageWidth && r.y + r.side <= state.imageHeight, "$r ends inside")
        assertTrue(state.zoom >= 1f && state.zoom <= state.maxZoom, "zoom ${state.zoom} within limits")
    }

    // ---- The start

    @Test
    fun aLandscapePhotoStartsWithTheLargestCentredSquare() {
        assertEquals(CropRect(500, 0, 3000), landscape().rect())
    }

    @Test
    fun aPortraitPhotoStartsWithTheLargestCentredSquare() {
        assertEquals(CropRect(0, 500, 3000), portrait().rect())
    }

    @Test
    fun aSquarePhotoStartsWithTheWholePhoto() {
        assertEquals(CropRect(0, 0, 2000), square().rect())
    }

    @Test
    fun theStartIsAtTheSmallestZoom() {
        assertEquals(1f, landscape().zoom)
        assertEquals(3000f, landscape().side)
    }

    @Test
    fun anImageWithNoSizeIsRefused() {
        assertFailsWith<IllegalArgumentException> { CropState.initial(0, 100) }
        assertFailsWith<IllegalArgumentException> { CropState.initial(100, -1) }
    }

    // ---- Panning stops at the edges

    @Test
    fun panningMovesTheFrameOverTheLandscapePhoto() {
        assertEquals(CropRect(700, 0, 3000), landscape().panBy(200f, 0f).rect())
        assertEquals(CropRect(300, 0, 3000), landscape().panBy(-200f, 0f).rect())
    }

    @Test
    fun panningFarToTheRightStopsAtTheRightEdge() {
        assertEquals(CropRect(1000, 0, 3000), landscape().panBy(1_000_000f, 0f).rect())
    }

    @Test
    fun panningFarToTheLeftStopsAtTheLeftEdge() {
        assertEquals(CropRect(0, 0, 3000), landscape().panBy(-1_000_000f, 0f).rect())
    }

    @Test
    fun aLandscapePhotoAtSmallestZoomCannotMoveUpOrDown() {
        assertEquals(landscape().rect(), landscape().panBy(0f, 5000f).rect())
        assertEquals(landscape().rect(), landscape().panBy(0f, -5000f).rect())
    }

    @Test
    fun aPortraitPhotoMovesUpAndDownAndStopsAtBothEdges() {
        assertEquals(CropRect(0, 1000, 3000), portrait().panBy(0f, 1_000_000f).rect())
        assertEquals(CropRect(0, 0, 3000), portrait().panBy(0f, -1_000_000f).rect())
        assertEquals(portrait().rect(), portrait().panBy(5000f, 0f).rect())
    }

    @Test
    fun aSquarePhotoAtSmallestZoomCannotMoveAtAll() {
        assertEquals(square().rect(), square().panBy(700f, -300f).rect())
    }

    @Test
    fun panningZoomedInStopsAtEveryEdge() {
        val zoomed = landscape().zoomBy(3f) // a frame of 1000 px
        assertEquals(1000f, zoomed.side)
        assertEquals(CropRect(3000, 2000, 1000), zoomed.panBy(1e6f, 1e6f).rect())
        assertEquals(CropRect(0, 0, 1000), zoomed.panBy(-1e6f, -1e6f).rect())
    }

    // ---- Zooming

    @Test
    fun zoomingInMakesTheFrameSmallerAroundTheCentre() {
        assertEquals(CropRect(1000, 500, 2000), landscape().zoomBy(1.5f).rect())
    }

    @Test
    fun zoomingOutPastTheStartIsRefusedAtTheSmallestZoom() {
        val zoomedOut = landscape().zoomBy(0.1f)

        assertEquals(1f, zoomedOut.zoom)
        assertEquals(landscape().rect(), zoomedOut.rect())
    }

    @Test
    fun zoomingInAndOutAgainReturnsToTheStart() {
        assertEquals(landscape().rect(), landscape().zoomBy(2f).zoomBy(0.5f).rect())
    }

    @Test
    fun zoomingOutFromAnOffCentreFrameKeepsItInsidePhoto() {
        val state = landscape().zoomBy(3f).panBy(1e6f, 1e6f).zoomBy(0.4f)

        assertInside(state)
    }

    @Test
    fun theZoomStopsWhenTheFrameWouldBeSmallerThanTheLimit() {
        val state = landscape().zoomBy(1000f)

        assertEquals(CropState.MIN_SIDE.toFloat(), state.side)
        assertEquals(state.maxZoom, state.zoom)
        assertEquals(CropState.MIN_SIDE, state.rect().side)
    }

    @Test
    fun aPhotoSmallerThanTheLimitCannotBeZoomedAtAll() {
        val tiny = CropState.initial(100, 80)

        assertEquals(1f, tiny.maxZoom)
        assertEquals(tiny.rect(), tiny.zoomBy(5f).rect())
        assertEquals(CropRect(10, 0, 80), tiny.rect())
    }

    @Test
    fun zoomingAroundAFocusKeepsThatPointStillInTheFrame() {
        // The point at the frame's left-quarter stays at the frame's left-quarter.
        val start = landscape().panBy(-1e6f, 0f) // the frame covers x in 0..3000
        val focusX = 750f // a quarter of the way across the frame
        val zoomed = start.zoomBy(2f, focusX = focusX, focusY = 1500f)

        val r = zoomed.rect()
        assertEquals(1500, r.side)
        assertEquals(750f, r.x + r.side * 0.25f) // 375 + 375: still a quarter across
    }

    @Test
    fun zoomingAroundAPointAtTheEdgeStaysInsideThePhoto() {
        val zoomed = landscape().panBy(1e6f, 1e6f).zoomBy(4f, focusX = 4000f, focusY = 3000f)

        assertInside(zoomed)
        assertEquals(4000, zoomed.rect().x + zoomed.rect().side)
    }

    // ---- The rectangle

    @Test
    fun aNarrowPhotoUsesItsWholeWidth() {
        val narrow = CropState.initial(1000, 6000)

        assertEquals(CropRect(0, 2500, 1000), narrow.rect())
    }

    @Test
    fun theRectangleIsAlwaysInsideThePhotoWhateverIsDone() {
        var state = landscape()
        val steps = listOf(
            { s: CropState -> s.zoomBy(2.3f, 100f, 100f) },
            { s: CropState -> s.panBy(-7777f, 4321f) },
            { s: CropState -> s.zoomBy(0.7f, 3900f, 2900f) },
            { s: CropState -> s.panBy(99999f, -99999f) },
            { s: CropState -> s.zoomBy(9f, 0f, 0f) },
            { s: CropState -> s.zoomBy(0.01f, 4000f, 3000f) },
        )
        for (step in steps) {
            state = step(state)
            assertInside(state)
        }
    }

    @Test
    fun theSameMovesGiveTheSameFrame() {
        val a = landscape().zoomBy(2f, 900f, 700f).panBy(50f, -20f)
        val b = landscape().zoomBy(2f, 900f, 700f).panBy(50f, -20f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ---- The buttons: steps, moves, turning and saving

    private fun aZoomedFrame() = landscape().zoomBy(2f, 2000f, 1500f).panBy(300f, 100f)

    private fun assertNear(expected: Int, actual: Int) = assertTrue(kotlin.math.abs(expected - actual) <= 1, "$actual is $expected within a pixel")

    private fun assertRectsWithinAPixel(expected: CropRect, actual: CropRect) {
        assertTrue(kotlin.math.abs(expected.x - actual.x) <= 1 && kotlin.math.abs(expected.y - actual.y) <= 1 && kotlin.math.abs(expected.side - actual.side) <= 1, "$actual is $expected within a pixel")
    }

    @Test
    fun aZoomInStepMakesTheFrameAFifthSmaller() {
        assertEquals(3000f / CropState.ZOOM_STEP, landscape().zoomInStep().side, 0.01f)
        assertEquals(CropRect(500 + 300, 0 + 300, 2400), landscape().zoomInStep().rect())
    }

    @Test
    fun aZoomStepAndItsInverseReturnToTheSameFrameOnEveryShapeOfPhoto() {
        for (start in listOf(landscape(), portrait(), square(), aZoomedFrame())) {
            val zoomedIn = start.zoomInStep()
            assertRectsWithinAPixel(start.rect(), zoomedIn.zoomOutStep().rect())
        }
    }

    @Test
    fun theZoomButtonsStopAtTheZoomLimits() {
        var state = landscape()
        repeat(60) { state = state.zoomInStep() }
        assertEquals(state.maxZoom, state.zoom)
        assertEquals(state, state.zoomInStep())
        repeat(60) { state = state.zoomOutStep() }
        assertEquals(1f, state.zoom)
        assertEquals(state, state.zoomOutStep())
        assertInside(state)
    }

    @Test
    fun aPhotoSmallerThanTheLimitHasNoZoomButtonsEffect() {
        val tiny = CropState.initial(100, 100)
        assertEquals(tiny, tiny.zoomInStep())
        assertEquals(tiny, tiny.zoomOutStep())
    }

    @Test
    fun aMoveButtonMovesThePhotoAStepUnderTheFrame() {
        val start = aZoomedFrame()
        val step = start.side * CropState.MOVE_STEP
        // The photo moves left, so the frame is further right over it.
        assertNear(start.rect().x + step.toInt(), start.movePhoto(CropMove.Left).rect().x)
        assertNear(start.rect().x - step.toInt(), start.movePhoto(CropMove.Right).rect().x)
        assertNear(start.rect().y + step.toInt(), start.movePhoto(CropMove.Up).rect().y)
        assertNear(start.rect().y - step.toInt(), start.movePhoto(CropMove.Down).rect().y)
    }

    @Test
    fun theMoveButtonsStopAtEveryEdgeOnEveryShapeOfPhoto() {
        for (start in listOf(landscape().zoomInStep(), portrait().zoomInStep(), square().zoomInStep(), landscape(), CropState.initial(100, 100))) {
            for (direction in CropMove.values()) {
                var state = start
                repeat(200) { state = state.movePhoto(direction); assertInside(state) }
            }
        }
    }

    @Test
    fun aLandscapePhotoAtTheSmallestZoomCannotBeMovedUpOrDownByTheButtons() {
        assertEquals(landscape(), landscape().movePhoto(CropMove.Up))
        assertEquals(landscape(), landscape().movePhoto(CropMove.Down))
    }

    @Test
    fun aQuarterTurnSwapsTheSizeAndKeepsTheFrameOverTheSameContent() {
        for (start in listOf(landscape(), portrait(), square(), aZoomedFrame(), portrait().zoomBy(3f, 500f, 3500f))) {
            val before = start.rect()
            val turned = start.rotatedClockwise()
            assertEquals(start.imageHeight, turned.imageWidth)
            assertEquals(start.imageWidth, turned.imageHeight)
            // A point (x, y) of the photo is at (height - y, x) once it is turned: the square that began at x, y ends at height - y - side, x.
            assertRectsWithinAPixel(CropRect(start.imageHeight - before.y - before.side, before.x, before.side), turned.rect())
            assertInside(turned)
        }
    }

    @Test
    fun fourQuarterTurnsReturnTheFrame() {
        for (start in listOf(landscape(), aZoomedFrame(), portrait().zoomBy(3f, 500f, 3500f))) {
            val back = start.rotatedClockwise().rotatedClockwise().rotatedClockwise().rotatedClockwise()
            assertEquals(start.imageWidth, back.imageWidth)
            assertEquals(start.imageHeight, back.imageHeight)
            assertRectsWithinAPixel(start.rect(), back.rect())
        }
    }

    @Test
    fun aTurnedFrameCanBeMovedAndZoomedAndStaysInside() {
        var state = aZoomedFrame().rotatedClockwise()
        for (direction in CropMove.values()) { state = state.movePhoto(direction).zoomInStep(); assertInside(state) }
    }

    @Test
    fun aSavedFrameRestoresToAnEqualOne() {
        for (start in listOf(landscape(), aZoomedFrame(), portrait().zoomBy(3f, 500f, 3500f).rotatedClockwise())) {
            assertEquals(start, CropState.restore(start.toSaved(), start.imageWidth, start.imageHeight))
        }
    }

    @Test
    fun aSavedFrameIsRefusedForAPhotoOfAnotherSize() {
        val saved = aZoomedFrame().toSaved()
        assertEquals(null, CropState.restore(saved, 3000, 4000))
        assertEquals(null, CropState.restore(saved, 4000, 3001))
    }

    @Test
    fun nothingSavedOrAnUnreadableSavedValueGivesNoFrame() {
        assertEquals(null, CropState.restore(null, 4000, 3000))
        assertEquals(null, CropState.restore(listOf(1f, 2f), 4000, 3000))
    }

    @Test
    fun aSavedFrameWithValuesOutsideTheLimitsIsBroughtInsideThem() {
        val restored = CropState.restore(listOf(4000f, 3000f, 99999f, -50f, 99999f), 4000, 3000)!!
        assertInside(restored)
    }

    @Test
    fun resetIsTheStartAgain() {
        assertEquals(landscape(), CropState.initial(4000, 3000))
        assertTrue(aZoomedFrame() != landscape())
    }

    // ---- Gestures: what a drag and a pinch do (the screen only passes what the fingers did)

    /** A frame of 800 pixels at (140, 500), like a phone's crop screen. */
    private val frame = CropFrame(left = 140f, top = 500f, side = 800f)

    /** The point of the photo under the screen point ([x], [y]), for [state] drawn in [frame]. */
    private fun photoPointAt(state: CropState, x: Float, y: Float): Pair<Float, Float> {
        val scale = frame.side / state.side
        return (state.centerX - state.side / 2 + (x - frame.left) / scale) to (state.centerY - state.side / 2 + (y - frame.top) / scale)
    }

    @Test
    fun aDragMovesThePhotoWithTheFingers() {
        val start = aZoomedFrame()
        val scale = frame.side / start.side

        val dragged = start.transformedBy(frame, 500f, 900f, panX = 100f, panY = -60f, zoom = 1f)

        // The photo goes right and up with the fingers, so the frame goes left and down over it, by the drag divided by the scale.
        assertEquals(start.centerX - 100f / scale, dragged.centerX, 0.01f)
        assertEquals(start.centerY + 60f / scale, dragged.centerY, 0.01f)
        assertEquals(start.zoom, dragged.zoom)
    }

    @Test
    fun aDragFarBeyondThePhotoStopsAtItsEdges() {
        for (start in listOf(aZoomedFrame(), portrait().zoomInStep(), landscape().zoomInStep())) {
            for ((dx, dy) in listOf(5000f to 0f, -5000f to 0f, 0f to 5000f, 0f to -5000f, 5000f to 5000f)) {
                assertInside(start.transformedBy(frame, 500f, 900f, dx, dy, 1f))
            }
        }
    }

    @Test
    fun aPinchKeepsThePointUnderTheFingersUnderThem() {
        for (start in listOf(landscape(), aZoomedFrame(), portrait().zoomBy(2f, 1500f, 2000f))) {
            for (zoom in listOf(1.5f, 0.8f)) {
                val x = 300f; val y = 800f
                val before = photoPointAt(start, x, y)
                val pinched = start.transformedBy(frame, x, y, 0f, 0f, zoom)
                // Unless a limit stopped the zoom (or the photo's edge stopped the frame), the same point of the photo is still there.
                if (pinched.zoom == start.zoom * zoom) {
                    val after = photoPointAt(pinched, x, y)
                    assertEquals(before.first, after.first, 1f)
                    assertEquals(before.second, after.second, 1f)
                }
                assertInside(pinched)
            }
        }
    }

    @Test
    fun aPinchInMakesTheFrameSmallerAndAPinchOutLarger() {
        val start = aZoomedFrame()
        assertTrue(start.transformedBy(frame, 500f, 900f, 0f, 0f, 1.5f).side < start.side)
        assertTrue(start.transformedBy(frame, 500f, 900f, 0f, 0f, 0.8f).side > start.side)
    }

    @Test
    fun aPinchStopsAtTheZoomLimits() {
        var state = landscape()
        repeat(40) { state = state.transformedBy(frame, 500f, 900f, 0f, 0f, 2f) }
        assertEquals(state.maxZoom, state.zoom)
        repeat(40) { state = state.transformedBy(frame, 500f, 900f, 0f, 0f, 0.5f) }
        assertEquals(1f, state.zoom)
        assertInside(state)
    }

    @Test
    fun aGestureIsAppliedToTheStateItIsGivenSoAButtonBeforeItIsNotLost() {
        val start = landscape()
        val afterButton = start.zoomInStep()

        val dragged = afterButton.transformedBy(frame, 500f, 900f, panX = 50f, panY = 0f, zoom = 1f)

        // The drag starts from the zoomed frame the button made: it is still zoomed in, and it moved from where the button left it.
        assertEquals(afterButton.zoom, dragged.zoom)
        assertTrue(dragged.zoom > start.zoom)
        assertTrue(dragged.centerX != afterButton.centerX)
    }

    @Test
    fun aGestureThatDoesNothingChangesNothing() {
        val start = aZoomedFrame()
        assertEquals(start, start.transformedBy(frame, 500f, 900f, 0f, 0f, 1f))
    }

    @Test
    fun theFrameIsTheLargestSquareInTheMiddleOfTheFreeSpaceKeepingTheMarginClear() {
        // A free space of 1080 by 1500 pixels from (0, 200): the frame is limited by the width.
        val portraitFrame = cropFrame(0f, 200f, 1080f, 1700f, margin = 60f)
        assertEquals(960f, portraitFrame.side)
        assertEquals(60f, portraitFrame.left)
        assertEquals(200f + (1500f - 960f) / 2, portraitFrame.top)
        // A wide, short free space (landscape): limited by the height, in the middle of the width.
        val landscapeFrame = cropFrame(0f, 100f, 2000f, 700f, margin = 60f)
        assertEquals(480f, landscapeFrame.side)
        assertEquals(1000f - 240f, landscapeFrame.left)
        assertEquals(100f + 60f, landscapeFrame.top)
    }

    @Test
    fun theFrameIsNeverSmallerThanAPixelWhateverTheFreeSpace() {
        assertEquals(1f, cropFrame(0f, 0f, 100f, 100f, margin = 60f).side)
        assertEquals(1f, cropFrame(0f, 0f, 0f, 0f, margin = 60f).side)
    }
}
