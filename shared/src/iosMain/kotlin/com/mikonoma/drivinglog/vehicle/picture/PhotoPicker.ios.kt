package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): PhotoPicker {
    val latest by rememberUpdatedState(onResult)
    // The picker only holds its delegate weakly, so the delegate lives as long as this composition.
    val delegate = remember { PickerDelegate() }
    delegate.onResult = { latest(it) }
    return remember(delegate) { PhotoPicker { present(delegate) } }
}

private class PickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {
    var onResult: (ByteArray?) -> Unit = {}

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val provider: NSItemProvider? = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onResult(null) // left without choosing
            return
        }
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            // An empty array means the photo could not be read.
            val bytes = data?.toByteArray() ?: ByteArray(0)
            dispatch_async(dispatch_get_main_queue()) { onResult(bytes) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun present(delegate: PickerDelegate) {
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 1
        filter = PHPickerFilter.imagesFilter
    }
    val picker = PHPickerViewController(configuration = configuration)
    picker.delegate = delegate
    var top: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    top?.presentViewController(picker, animated = true, completion = null)
}
