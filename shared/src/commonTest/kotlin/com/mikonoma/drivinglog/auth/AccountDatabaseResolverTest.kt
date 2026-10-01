package com.mikonoma.drivinglog.auth

import com.mikonoma.drivinglog.vehicle.data.DATABASE_NAME
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** A fresh in-memory fake per test case — never a real/shared store, so results can't depend on test order. */
private class FakeClaimedAccountStore(initial: String? = null) : ClaimedAccountStore {
    private var claimed: String? = initial

    override fun claimedUid(): String? = claimed

    override fun setClaimedUid(uid: String) {
        claimed = uid
    }
}

class AccountDatabaseResolverTest {
    @Test
    fun theFirstAccountToSignInClaimsTheOriginalFile() {
        val store = FakeClaimedAccountStore()
        val resolver = AccountDatabaseResolver(store)

        val name = resolver.databaseNameFor("uid-a")

        assertEquals(DATABASE_NAME, name)
        assertEquals("uid-a", store.claimedUid())
    }

    @Test
    fun theSameAccountSigningInAgainReusesTheOriginalFile() {
        val store = FakeClaimedAccountStore(initial = "uid-a")
        val resolver = AccountDatabaseResolver(store)

        assertEquals(DATABASE_NAME, resolver.databaseNameFor("uid-a"))
    }

    @Test
    fun aDifferentAccountGetsItsOwnFreshFile() {
        val store = FakeClaimedAccountStore(initial = "uid-a")
        val resolver = AccountDatabaseResolver(store)

        val name = resolver.databaseNameFor("uid-b")

        assertNotEquals(DATABASE_NAME, name)
        assertEquals("driving-log-uid-b.db", name)
        // The claim is untouched — switching back to uid-a must still resolve to the original file.
        assertEquals("uid-a", store.claimedUid())
    }

    @Test
    fun aThirdAccountGetsItsOwnFileIndependentOfTheSecond() {
        val store = FakeClaimedAccountStore(initial = "uid-a")
        val resolver = AccountDatabaseResolver(store)

        assertEquals("driving-log-uid-b.db", resolver.databaseNameFor("uid-b"))
        assertEquals("driving-log-uid-c.db", resolver.databaseNameFor("uid-c"))
    }
}
