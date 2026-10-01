package com.mikonoma.drivinglog.auth

import com.mikonoma.drivinglog.vehicle.data.DATABASE_NAME

/**
 * A device-level flag outside any account's database, recording which account has claimed the device's original,
 * un-scoped local data (design.md decision 5) — plain key-value storage, never SQLDelight's `app_state`, since that
 * lives inside an account-scoped database file and so can't be read before one is chosen.
 */
interface ClaimedAccountStore {
    fun claimedUid(): String?

    fun setClaimedUid(uid: String)
}

/**
 * Resolves which SQLite file a signed-in account's data lives in (design.md decision 5): the first account ever
 * signed in on the device claims the original, un-scoped file (so an existing local-only install keeps its data on
 * upgrade); every other account gets its own file named from its UID, isolated from every other account's — never
 * merged, filtered, or deleted, so switching back to a previous account restores exactly what it had.
 */
class AccountDatabaseResolver(private val claimedAccountStore: ClaimedAccountStore) {
    fun databaseNameFor(uid: String): String {
        val claimed = claimedAccountStore.claimedUid()
        if (claimed == null) {
            claimedAccountStore.setClaimedUid(uid)
            return DATABASE_NAME
        }
        return if (claimed == uid) DATABASE_NAME else "driving-log-$uid.db"
    }
}
