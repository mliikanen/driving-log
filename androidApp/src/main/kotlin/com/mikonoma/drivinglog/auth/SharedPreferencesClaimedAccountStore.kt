package com.mikonoma.drivinglog.auth

import android.content.Context
import androidx.core.content.edit

/** Plain, un-scoped `SharedPreferences` — not SQLDelight's `app_state`, which lives inside an account-scoped
 * database file and so can't be read before one is chosen (design.md decision 5). */
class SharedPreferencesClaimedAccountStore(context: Context) : ClaimedAccountStore {
    private val prefs = context.getSharedPreferences("auth_claim", Context.MODE_PRIVATE)

    override fun claimedUid(): String? = prefs.getString(KEY_CLAIMED_UID, null)

    override fun setClaimedUid(uid: String) {
        prefs.edit { putString(KEY_CLAIMED_UID, uid) }
    }

    private companion object {
        const val KEY_CLAIMED_UID = "claimed_uid"
    }
}
