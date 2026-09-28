package com.mikonoma.drivinglog

import android.app.Application
import android.text.format.DateFormat
import androidx.compose.runtime.mutableIntStateOf
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

    val graph: AppGraph by lazy {
        createAppGraph(
            driver = DatabaseDriverFactory(this).createDriver(),
            // The pictures live in the application's private storage, where no other app and no photo library sees them.
            picturesRoot = Path(filesDir.absolutePath, "pictures"),
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
        )
    }
}
