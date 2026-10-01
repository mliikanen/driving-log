package com.mikonoma.drivinglog.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Tracks the current resumed [Activity] for `AuthRepository.signIn()` implementations that need one to launch UI
 * from (Credential Manager's real picker, or the `fake` flavor's [TestAccountPickerActivity]) — `AuthRepository` is
 * constructed once at `Application` scope, before any `Activity` exists, and an `Activity` is recreated on
 * rotation, so a reference captured at construction time would go stale (add-firebase-auth/design.md decision 2).
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {
    @Volatile
    var current: Activity? = null
        private set

    override fun onActivityResumed(activity: Activity) {
        current = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (current === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
