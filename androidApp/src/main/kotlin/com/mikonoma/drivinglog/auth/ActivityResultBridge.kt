package com.mikonoma.drivinglog.auth

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred

/**
 * A classic `startActivityForResult`/`onActivityResult` bridge usable from a suspend function, for launching a
 * screen and awaiting its result. `androidx.activity.result.ActivityResultLauncher` needs to be registered before
 * the activity reaches `STARTED`, too early for an `AuthRepository` call made long after that (it's constructed
 * once at `Application` scope) — this is the fallback that works from any point in the activity's lifetime.
 * `MainActivity.onActivityResult` forwards here; at most one request is pending at a time, which is all this
 * project's single-screen test picker ([TestAccountPickerActivity]) needs.
 */
object ActivityResultBridge {
    private var nextRequestCode = 1
    private var pending: CompletableDeferred<Pair<Int, Intent?>>? = null

    suspend fun launchForResult(activity: Activity, intent: Intent): Pair<Int, Intent?> {
        val deferred = CompletableDeferred<Pair<Int, Intent?>>()
        pending = deferred
        activity.startActivityForResult(intent, nextRequestCode++)
        return deferred.await()
    }

    fun onActivityResult(resultCode: Int, data: Intent?) {
        pending?.complete(resultCode to data)
        pending = null
    }
}
