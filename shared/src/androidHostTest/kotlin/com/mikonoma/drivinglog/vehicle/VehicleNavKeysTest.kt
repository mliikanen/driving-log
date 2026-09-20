package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.vehicle.data.createTestDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.fuusio.kide.navigation.ScreenNavKeyRegistry

class VehicleNavKeysTest {

    private val serialKeys = listOf("vehicle-list", "vehicle-add", "vehicle-details", "vehicle-edit", "vehicle-log")

    @Test
    fun everyScreenIsRegistered() {
        registerVehicleNavKeys(createAppGraph(createTestDriver()))

        for (key in serialKeys) assertNotNull(ScreenNavKeyRegistry.find(key), key)
    }

    /** Composition runs again when the activity is recreated, for example on rotation: that must not crash. */
    @Test
    fun registeringTwiceWithTheSameGraphIsHarmless() {
        val graph = createAppGraph(createTestDriver())

        registerVehicleNavKeys(graph)
        registerVehicleNavKeys(graph)
        registerVehicleNavKeys(graph)

        for (key in serialKeys) assertNotNull(ScreenNavKeyRegistry.find(key), key)
    }

    @Test
    fun aNewGraphReplacesThePreviousRegistrations() {
        registerVehicleNavKeys(createAppGraph(createTestDriver()))
        val second = createAppGraph(createTestDriver())

        registerVehicleNavKeys(second)

        assertTrue(serialKeys.all { ScreenNavKeyRegistry.find(it) != null })
    }

    @Test
    fun aDetailsKeyIsRestoredWithItsVehicleId() {
        registerVehicleNavKeys(createAppGraph(createTestDriver()))

        val restored = ScreenNavKeyRegistry.get("vehicle-details").restoreArgs("v42") as VehicleDetailsNavKey

        assertEquals("v42", restored.vehicleId)
        assertEquals("v42", restored.saveArgs())
    }

    // Kide keeps a screen's processor in a store keyed by the nav key. After a rotation the back stack is restored into
    // new key instances, so keys must be equal by value or every rotation builds a new processor and loses the state.

    @Test
    fun keysWithoutArgumentsAreEqualAcrossInstances() {
        val first = createAppGraph(createTestDriver())
        val second = createAppGraph(createTestDriver())

        assertEquals(VehicleListNavKey(first), VehicleListNavKey(second))
        assertEquals(AddVehicleNavKey(first), AddVehicleNavKey(second))
        assertEquals(VehicleListNavKey(first).hashCode(), VehicleListNavKey(second).hashCode())
        assertNotEquals<Any>(VehicleListNavKey(first), AddVehicleNavKey(first))
    }

    @Test
    fun keysWithAVehicleIdAreEqualByIdAndKind() {
        val graph = createAppGraph(createTestDriver())

        assertEquals(VehicleDetailsNavKey(graph, "v1"), VehicleDetailsNavKey(graph, "v1"))
        assertEquals(VehicleDetailsNavKey(graph, "v1").hashCode(), VehicleDetailsNavKey(graph, "v1").hashCode())
        assertNotEquals(VehicleDetailsNavKey(graph, "v1"), VehicleDetailsNavKey(graph, "v2"))
        assertNotEquals<Any>(VehicleDetailsNavKey(graph, "v1"), EditVehicleNavKey(graph, "v1"))
        assertNotEquals<Any>(EditVehicleNavKey(graph, "v1"), VehicleLogNavKey(graph, "v1"))
    }

    @Test
    fun aRestoredKeyEqualsTheKeyItWasSavedFrom() {
        val graph = createAppGraph(createTestDriver())
        registerVehicleNavKeys(graph)

        val keys = listOf(VehicleDetailsNavKey(graph, "v7"), EditVehicleNavKey(graph, "v7"), VehicleLogNavKey(graph, "v7"))
        for (key in keys) {
            val restored = ScreenNavKeyRegistry.get(key.serialKey).restoreArgs(checkNotNull(key.saveArgs()))
            assertEquals<Any>(key, restored, key.serialKey)
        }
    }
}
