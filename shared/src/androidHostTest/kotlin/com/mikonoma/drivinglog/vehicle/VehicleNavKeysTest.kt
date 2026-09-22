package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.FakeDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.createTestDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encoding.CompositeDecoder
import com.mikonoma.drivinglog.vehicle.distance.LogDistanceState
import org.fuusio.kide.navigation.ScreenNavKeyRegistry

class VehicleNavKeysTest {

    /** A graph over an in-memory database; the pictures directory is never used by these tests. */
    private fun testGraph() = createAppGraph(
        createTestDriver(),
        FakeDeviceLocale(),
        kotlinx.io.files.Path(kotlinx.io.files.SystemTemporaryDirectory, "driving-log-nav-key-test-pictures"),
        com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec(),
    )

    private val serialKeys = listOf("landing", "vehicle-list", "vehicle-add", "vehicle-details", "vehicle-edit", "vehicle-log", "vehicle-log-distance")

    @Test
    fun everyScreenIsRegistered() {
        registerVehicleNavKeys(testGraph())

        for (key in serialKeys) assertNotNull(ScreenNavKeyRegistry.find(key), key)
    }

    /** Composition runs again when the activity is recreated, for example on rotation: that must not crash. */
    @Test
    fun registeringTwiceWithTheSameGraphIsHarmless() {
        val graph = testGraph()

        registerVehicleNavKeys(graph)
        registerVehicleNavKeys(graph)
        registerVehicleNavKeys(graph)

        for (key in serialKeys) assertNotNull(ScreenNavKeyRegistry.find(key), key)
    }

    @Test
    fun aNewGraphReplacesThePreviousRegistrations() {
        registerVehicleNavKeys(testGraph())
        val second = testGraph()

        registerVehicleNavKeys(second)

        assertTrue(serialKeys.all { ScreenNavKeyRegistry.find(it) != null })
    }

    @Test
    fun aDetailsKeyIsRestoredWithItsVehicleId() {
        registerVehicleNavKeys(testGraph())

        val restored = ScreenNavKeyRegistry.get("vehicle-details").restoreArgs("v42") as VehicleDetailsNavKey

        assertEquals("v42", restored.vehicleId)
        assertEquals("v42", restored.saveArgs())
    }

    // Kide keeps a screen's processor in a store keyed by the nav key. After a rotation the back stack is restored into
    // new key instances, so keys must be equal by value or every rotation builds a new processor and loses the state.

    @Test
    fun keysWithoutArgumentsAreEqualAcrossInstances() {
        val first = testGraph()
        val second = testGraph()

        assertEquals(com.mikonoma.drivinglog.landing.LandingNavKey(first), com.mikonoma.drivinglog.landing.LandingNavKey(second))
        assertEquals(com.mikonoma.drivinglog.landing.LandingNavKey(first).hashCode(), com.mikonoma.drivinglog.landing.LandingNavKey(second).hashCode())
        assertNotEquals<Any>(com.mikonoma.drivinglog.landing.LandingNavKey(first), VehicleListNavKey(first))
        assertEquals(VehicleListNavKey(first), VehicleListNavKey(second))
        assertEquals(AddVehicleNavKey(first), AddVehicleNavKey(second))
        assertEquals(VehicleListNavKey(first).hashCode(), VehicleListNavKey(second).hashCode())
        assertNotEquals<Any>(VehicleListNavKey(first), AddVehicleNavKey(first))
    }

    @Test
    fun keysWithAVehicleIdAreEqualByIdAndKind() {
        val graph = testGraph()

        assertEquals(VehicleDetailsNavKey(graph, "v1"), VehicleDetailsNavKey(graph, "v1"))
        assertEquals(VehicleDetailsNavKey(graph, "v1").hashCode(), VehicleDetailsNavKey(graph, "v1").hashCode())
        assertNotEquals(VehicleDetailsNavKey(graph, "v1"), VehicleDetailsNavKey(graph, "v2"))
        assertNotEquals<Any>(VehicleDetailsNavKey(graph, "v1"), EditVehicleNavKey(graph, "v1"))
        assertNotEquals<Any>(EditVehicleNavKey(graph, "v1"), VehicleLogNavKey(graph, "v1"))
    }

    @Test
    fun aRestoredKeyEqualsTheKeyItWasSavedFrom() {
        val graph = testGraph()
        registerVehicleNavKeys(graph)

        val keys = listOf(VehicleDetailsNavKey(graph, "v7"), EditVehicleNavKey(graph, "v7"), VehicleLogNavKey(graph, "v7"))
        for (key in keys) {
            val restored = ScreenNavKeyRegistry.get(key.serialKey).restoreArgs(checkNotNull(key.saveArgs()))
            assertEquals<Any>(key, restored, key.serialKey)
        }
    }

    @Test
    fun logDistanceKeysAreEqualByVehicleAndRestoredKeysEqualTheOriginal() {
        val graph = testGraph()
        registerVehicleNavKeys(graph)

        assertEquals(LogDistanceNavKey(graph, "v1"), LogDistanceNavKey(graph, "v1"))
        assertEquals(LogDistanceNavKey(graph, "v1").hashCode(), LogDistanceNavKey(graph, "v1").hashCode())
        assertNotEquals(LogDistanceNavKey(graph, "v1"), LogDistanceNavKey(graph, "v2"))
        assertNotEquals<Any>(LogDistanceNavKey(graph, "v1"), VehicleLogNavKey(graph, "v1"))
        val key = LogDistanceNavKey(graph, "v7")
        val restored = ScreenNavKeyRegistry.get(key.serialKey).restoreArgs(checkNotNull(key.saveArgs()))
        assertEquals<Any>(key, restored)
        assertEquals("v7", (restored as LogDistanceNavKey).vehicleId)
    }

    @Test
    fun theLogDistanceStateSavesWhatTheUserTypedAndNotTheRepositoryData() {
        val descriptor = LogDistanceState.serializer().descriptor
        for (saved in listOf("way", "tripDistance", "newOdometer", "localDateTime", "zoneId", "selectedVehicleId", "unitFor")) {
            assertNotEquals(CompositeDecoder.UNKNOWN_NAME, descriptor.getElementIndex(saved), saved)
        }
        // Transient: rebuilt from the repository or only a message.
        for (transient in listOf("log", "error", "isLoading", "notFound", "vehicleUnit", "isSaving", "vehicles")) {
            assertEquals(CompositeDecoder.UNKNOWN_NAME, descriptor.getElementIndex(transient), transient)
        }
    }
}
