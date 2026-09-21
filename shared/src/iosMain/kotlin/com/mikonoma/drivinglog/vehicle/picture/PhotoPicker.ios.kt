package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * The system's source sheet: Take Photo, Photo Library and Choose File, each the system's own screen. Camera access is asked for
 * when Take Photo is chosen, not before (the app's Info.plist needs NSCameraUsageDescription for the text of that prompt); the
 * library and the file picker need no permission.
 */
@Composable
actual fun rememberPhotoPicker(onResult: (PhotoResult) -> Unit): PhotoPicker {
    val latest by rememberUpdatedState(onResult)
    // The system's pickers hold their delegates weakly, so the sources live as long as this composition.
    val sources = remember { IosPhotoSources() }
    sources.onResult = { latest(it) }
    return remember(sources) { PhotoPicker { sources.showSourceSheet() } }
}

@OptIn(ExperimentalForeignApi::class)
private class IosPhotoSources {
    var onResult: (PhotoResult) -> Unit = {}

    private val library = LibraryDelegate { onResult(it) }
    private val camera = CameraDelegate { onResult(it) }
    private val documents = DocumentDelegate { onResult(it) }

    fun showSourceSheet() {
        val sheet = UIAlertController.alertControllerWithTitle(title = null, message = null, preferredStyle = UIAlertControllerStyleActionSheet)
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            sheet.addAction(UIAlertAction.actionWithTitle("Take Photo", UIAlertActionStyleDefault) { _ -> takePhoto() })
        }
        sheet.addAction(UIAlertAction.actionWithTitle("Photo Library", UIAlertActionStyleDefault) { _ -> presentLibrary() })
        sheet.addAction(UIAlertAction.actionWithTitle("Choose File", UIAlertActionStyleDefault) { _ -> presentDocuments() })
        sheet.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) { _ -> onResult(PhotoResult.Cancelled) })
        top()?.let { presenter ->
            // On an iPad an action sheet is a popover and needs a place to point at.
            sheet.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(sheet, animated = true, completion = null)
        }
    }

    /** Camera access is asked for here, at the moment the user chose Take Photo. */
    private fun takePhoto() {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> presentCamera()
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) { if (granted) presentCamera() else onResult(PhotoResult.CameraDenied) }
            }
            else -> onResult(PhotoResult.CameraDenied) // refused, or restricted
        }
    }

    private fun presentCamera() {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.delegate = camera
        top()?.presentViewController(picker, animated = true, completion = null)
    }

    private fun presentLibrary() {
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = library
        top()?.presentViewController(picker, animated = true, completion = null)
    }

    private fun presentDocuments() {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeImage))
        picker.delegate = documents
        top()?.presentViewController(picker, animated = true, completion = null)
    }

    private fun top(): UIViewController? {
        var top: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}

private class LibraryDelegate(val done: (PhotoResult) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            done(PhotoResult.Cancelled)
            return
        }
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val result = data?.toByteArray()?.takeIf { it.isNotEmpty() }?.let { PhotoResult.Chosen(it) } ?: PhotoResult.Unreadable
            dispatch_async(dispatch_get_main_queue()) { done(result) }
        }
    }
}

private class CameraDelegate(val done: (PhotoResult) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        // JPEG keeps the photo's orientation in its metadata, which the codec applies.
        val bytes = image?.let { UIImageJPEGRepresentation(it, 0.95) }?.toByteArray()
        done(bytes?.takeIf { it.isNotEmpty() }?.let { PhotoResult.Chosen(it) } ?: PhotoResult.Unreadable)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        done(PhotoResult.Cancelled)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DocumentDelegate(val done: (PhotoResult) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            done(PhotoResult.Cancelled)
            return
        }
        // The file lives outside the app: it can be read only inside the security scope.
        val scoped = url.startAccessingSecurityScopedResource()
        val data: NSData? = NSData.dataWithContentsOfURL(url)
        if (scoped) url.stopAccessingSecurityScopedResource()
        done(data?.toByteArray()?.takeIf { it.isNotEmpty() }?.let { PhotoResult.Chosen(it) } ?: PhotoResult.Unreadable)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = done(PhotoResult.Cancelled)
}
