package com.mikonoma.drivinglog.vehicle.picture

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

/*
 * The camera apps in the photo chooser (`ask-camera-for-camera-app`). The app declares the camera permission for the live scanner, and
 * Android refuses `ACTION_IMAGE_CAPTURE` from an app that declares it without holding it. The chooser launches the chosen app as this
 * app, so a camera app chosen there would fail silently. The chooser's refinement step (`EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER`) runs
 * after the user picks an app and before it is launched: [CameraRefinementReceiver] answers it, asking for the permission first
 * ([CameraPermissionActivity]) when the chosen app is a camera and the permission isn't held.
 */

/** "Take a photo into [output]", with the write grant on it carried to whichever camera app opens it. */
internal fun captureIntent(output: Uri): Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    .putExtra(MediaStore.EXTRA_OUTPUT, output)
    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    .apply { clipData = ClipData.newRawUri("", output) }

/**
 * The photos whose camera app the user wasn't allowed to open, by output Uri. The chooser accepts a refinement only for the app the user
 * chose, so a refusal can only cancel the launch, which reaches the picker as a plain cancel; [CameraPermissionActivity] notes the
 * refusal here first, and the picker reads it back ([consumeDenial]) to tell a refusal from leaving the chooser. In memory: the picker
 * and this live in the same process, and a refusal lost to a process restart reads as a cancel.
 */
private val denied = mutableSetOf<Uri>()

/** Whether the camera app for [output] was refused camera access, forgetting it either way. */
internal fun consumeDenial(output: Uri): Boolean = synchronized(denied) { denied.remove(output) }

private fun noteDenial(output: Uri) = synchronized(denied) { denied += output }

/** The chooser's refinement step, answered by [CameraRefinementReceiver]. Mutable: the chooser fills in the chosen intent. */
internal fun cameraRefinement(context: Context): IntentSender = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, CameraRefinementReceiver::class.java),
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
).intentSender

private fun hasCamera(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Intent.chosen(): Intent? = IntentCompat.getParcelableExtra(this, Intent.EXTRA_INTENT, Intent::class.java)

private fun Intent.reply(): ResultReceiver? = IntentCompat.getParcelableExtra(this, Intent.EXTRA_RESULT_RECEIVER, ResultReceiver::class.java)

/** Launch [intent] as chosen (the chooser then starts it as the app, its result going back to the picker). */
private fun ResultReceiver.launch(intent: Intent) = send(Activity.RESULT_OK, Bundle().apply { putParcelable(Intent.EXTRA_INTENT, intent) })

/** Launch nothing: the chooser closes, and the picker gets a cancel. */
private fun ResultReceiver.cancel() = send(Activity.RESULT_CANCELED, null)

/**
 * Answers the chooser's refinement step: anything but a camera app, or a camera app while the permission is held, is launched as chosen,
 * at once and with no UI; a camera app without the permission goes through [CameraPermissionActivity] first. Not exported: only the
 * chooser, through the app's own PendingIntent, sends it.
 */
class CameraRefinementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chosen = intent.chosen() ?: return
        val reply = intent.reply() ?: return
        if (chosen.action != MediaStore.ACTION_IMAGE_CAPTURE || hasCamera(context)) {
            reply.launch(chosen)
        } else {
            context.startActivity(
                Intent(context, CameraPermissionActivity::class.java)
                    .putExtra(Intent.EXTRA_INTENT, chosen)
                    .putExtra(Intent.EXTRA_RESULT_RECEIVER, reply)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/**
 * Asks for the camera permission between choosing a camera app in the chooser and its launch. No UI of its own: the system prompt shows
 * over the chooser. Allowed, the chooser launches the camera app the user chose. Refused (now, before, or the prompt left without an
 * answer), nothing is launched and the refusal is noted for the picker. Not exported.
 */
class CameraPermissionActivity : ComponentActivity() {
    private val request = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> answer(granted) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restored while the prompt is up (a rotation): the prompt's answer still comes to [request], so it is not asked again.
        if (savedInstanceState == null) request.launch(Manifest.permission.CAMERA)
    }

    private fun answer(granted: Boolean) {
        val chosen = intent.chosen()
        val reply = intent.reply()
        if (chosen != null && reply != null) {
            if (granted) {
                reply.launch(chosen)
            } else {
                IntentCompat.getParcelableExtra(chosen, MediaStore.EXTRA_OUTPUT, Uri::class.java)?.let(::noteDenial)
                reply.cancel()
            }
        }
        finish()
    }
}
