package com.mikonoma.drivinglog

import android.app.Application
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.mutableIntStateOf
import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.auth.AccountDatabaseResolver
import com.mikonoma.drivinglog.auth.AuthRepository
import com.mikonoma.drivinglog.auth.CurrentActivityHolder
import com.mikonoma.drivinglog.auth.SharedPreferencesClaimedAccountStore
import com.mikonoma.drivinglog.auth.applyAuthLaunchArguments
import com.mikonoma.drivinglog.auth.createAuthRepository
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory
import com.mikonoma.drivinglog.vehicle.ocr.CombinedTextRecognizer
import com.mikonoma.drivinglog.vehicle.ocr.MlKitTextRecognizer
import com.mikonoma.drivinglog.vehicle.ocr.PpOcrTextRecognizer
import com.mikonoma.drivinglog.vehicle.picture.AndroidImageCodec
import kotlinx.io.files.Path

class DrivingLogApplication : Application() {
    /** Bumped when the app returns to the foreground, so screens re-read the system's clock setting. */
    val resumeCount = mutableIntStateOf(0)

    /** Tracks the current resumed `Activity` for `AuthRepository.signIn()` to launch UI from (add-firebase-auth/
     * design.md decision 2) — registered in [onCreate], before any activity exists. */
    private val currentActivityHolder = CurrentActivityHolder()

    /** Constructed once, outside any [AppGraph] — it's what decides which account's graph to build, so it can't
     * itself depend on one. The concrete type (`FirebaseAuthRepository`/`FakeAuthRepository`) is chosen by the
     * product flavor; this class never names either (design.md decision 6). */
    val authRepository: AuthRepository by lazy { createAuthRepository(currentActivityHolder) }

    private val claimedAccountStore by lazy { SharedPreferencesClaimedAccountStore(this) }
    private val accountDatabaseResolver by lazy { AccountDatabaseResolver(claimedAccountStore) }

    private var cachedUid: String? = null
    private var cachedDriver: SqlDriver? = null
    private var cachedGraph: AppGraph? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(currentActivityHolder)
    }

    /** Called once from `MainActivity.onCreate`; a no-op in the `production` flavor (design.md decision 7). */
    fun applyLaunchArguments(intent: Intent) = applyAuthLaunchArguments(authRepository, intent)

    /**
     * The [AppGraph] for [uid]'s own, isolated local database (design.md decision 5) — not built until an account
     * is actually signed in, and rebuilt (the previous account's driver closed, never merged, filtered, or
     * deleted) when a *different* account signs in within the same running process. The pictures root is
     * isolated the same way as the database file: without it, `sweepPictures`/`sweepCaptures` (which delete any
     * picture no *current* vehicle references) would delete a previous account's pictures the moment a different
     * account's graph is built, even though its database file is untouched.
     */
    @Synchronized
    fun graphFor(uid: String): AppGraph {
        cachedGraph?.let { graph -> if (cachedUid == uid) return graph }
        cachedDriver?.close()

        val databaseName = accountDatabaseResolver.databaseNameFor(uid)
        val claimsOriginalData = claimedAccountStore.claimedUid() == uid
        val picturesDirName = if (claimsOriginalData) "pictures" else "pictures-$uid"

        val driver = DatabaseDriverFactory(this).createDriver(databaseName)
        val graph = createAppGraph(
            driver = driver,
            // The pictures live in the application's private storage, where no other app and no photo library sees them.
            picturesRoot = Path(filesDir.absolutePath, picturesDirName),
            imageCodec = AndroidImageCodec(),
            // ML Kit reads printed text; PP-OCR reads seven-segment LCD digits (add-seven-segment-ocr). Both run on every scan.
            textRecognizer = CombinedTextRecognizer(
                MlKitTextRecognizer(),
                PpOcrTextRecognizer(loadModel = { name -> assets.open("ocr/$name").use { it.readBytes() } }),
            ),
            // Read on every call; the resume count makes composables that read it recompose after
            // the user changed the system's 12/24-hour setting and came back.
            deviceLocale = SystemDeviceLocale {
                resumeCount.intValue
                DateFormat.is24HourFormat(this)
            },
            authRepository = authRepository,
        )

        cachedUid = uid
        cachedDriver = driver
        cachedGraph = graph
        return graph
    }
}
