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
}
