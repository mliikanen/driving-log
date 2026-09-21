package com.mikonoma.drivinglog.landing

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class LandingProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun tile(tiles: List<LandingTile>, id: LandingTileId) = tiles.single { it.id == id }

    // ---- The tiles

    @Test
    fun theGridHasFourTilesInOrder() {
        assertEquals(
            listOf(LandingTileId.VEHICLES, LandingTileId.LOG_EVENT, LandingTileId.TRIP, LandingTileId.PLACEHOLDER),
            landingTiles(hasVehicles = true, isLoading = false).map { it.id },
        )
    }

    @Test
    fun withVehiclesTheFirstTileOpensTheVehicles() {
        val first = tile(landingTiles(hasVehicles = true, isLoading = false), LandingTileId.VEHICLES)

        assertEquals("Vehicles", first.name)
        assertEquals(LandingIcon.VEHICLES, first.icon)
        assertTrue(first.enabled)
    }

    @Test
    fun withoutVehiclesTheFirstTileIsTheSameCarAndOnlyItsNameChanges() {
        val first = tile(landingTiles(hasVehicles = false, isLoading = false), LandingTileId.VEHICLES)
        val withVehicles = tile(landingTiles(hasVehicles = true, isLoading = false), LandingTileId.VEHICLES)

        assertEquals("Add vehicle", first.name)
        assertEquals(LandingIcon.VEHICLES, first.icon)
        assertEquals(withVehicles.icon, first.icon)
        assertTrue(first.enabled)
    }

    @Test
    fun whileLoadingTheFirstTileHasNoNameAndIsNotEnabledSoATapCannotPickTheWrongScreen() {
        for (hasVehicles in listOf(true, false)) {
            val first = tile(landingTiles(hasVehicles, isLoading = true), LandingTileId.VEHICLES)

            assertEquals("", first.name)
            assertEquals(LandingIcon.VEHICLES, first.icon)
            assertFalse(first.enabled)
        }
    }

    @Test
    fun exactlyTheThreePlaceholdersAreNotEnabled() {
        for (hasVehicles in listOf(true, false)) {
            val disabled = landingTiles(hasVehicles, isLoading = false).filterNot { it.enabled }.map { it.id }

            assertEquals(listOf(LandingTileId.LOG_EVENT, LandingTileId.TRIP, LandingTileId.PLACEHOLDER), disabled)
        }
    }

    @Test
    fun thePlaceholdersHaveTheirNamesAndIcons() {
        val tiles = landingTiles(hasVehicles = true, isLoading = false)

        assertEquals("Log event" to LandingIcon.LOG_EVENT, tile(tiles, LandingTileId.LOG_EVENT).let { it.name to it.icon })
        assertEquals("Trip" to LandingIcon.TRIP, tile(tiles, LandingTileId.TRIP).let { it.name to it.icon })
        assertEquals("Placeholder" to LandingIcon.PLACEHOLDER, tile(tiles, LandingTileId.PLACEHOLDER).let { it.name to it.icon })
    }

    // ---- The processor

    @Test
    fun anEmptyRepositoryGivesTheAddVehicleTile() {
        val processor = LandingProcessor(repository)

        assertFalse(processor.state.isLoading)
        assertFalse(processor.state.hasVehicles)
        assertEquals("Add vehicle", tile(processor.state.tiles, LandingTileId.VEHICLES).name)
    }

    @Test
    fun aVehicleGivesTheVehiclesTile() {
        repository.seedVehicle("v1", "Family car")

        val processor = LandingProcessor(repository)

        assertTrue(processor.state.hasVehicles)
        assertEquals("Vehicles", tile(processor.state.tiles, LandingTileId.VEHICLES).name)
    }

    @Test
    fun theTileFollowsAVehicleBeingAdded() = runTest {
        val processor = LandingProcessor(repository)
        assertEquals("Add vehicle", tile(processor.state.tiles, LandingTileId.VEHICLES).name)

        repository.addVehicle("Van", null, VehicleType.VAN, VehicleColors.default, OdometerUnit.KILOMETERS, Distance.ZERO)

        assertEquals("Vehicles", tile(processor.state.tiles, LandingTileId.VEHICLES).name)
    }

    @Test
    fun theVehiclesIntentShowsTheVehicles() = runTest {
        LandingProcessor(repository).test {
            dispatch(LandingIntent.OpenVehicles)
            expectSideEffect(LandingEffect.ShowVehicles)
        }
    }

    @Test
    fun theAddVehicleIntentShowsTheAddScreen() = runTest {
        LandingProcessor(repository).test {
            dispatch(LandingIntent.AddVehicle)
            expectSideEffect(LandingEffect.ShowAddVehicle)
        }
    }
}
