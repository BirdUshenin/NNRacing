package ru.ilyaushenin.nnracing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject

/** Делегат системного выбора фото: из выбранной картинки делает квадрат 256×256 в JPEG. */
private class PickerDelegate(var onPicked: (ByteArray) -> Unit) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
        squareJpeg(image)?.let(onPicked)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun squareJpeg(image: UIImage): ByteArray? {
    val (w, h) = image.size.useContents { width to height }
    val side = minOf(w, h)
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(256.0, 256.0), true, 1.0)
    // рисуем с отрицательным смещением: в кадр попадает центральный квадрат
    val scale = 256.0 / side
    image.drawInRect(CGRectMake(-(w - side) / 2 * scale, -(h - side) / 2 * scale, w * scale, h * scale))
    val result = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    val data = UIImageJPEGRepresentation(result ?: return null, 0.85) ?: return null
    return data.bytes?.readBytes(data.length.toInt())
}

/** Делегат выбора фото для чата. */
private class PhotoDelegate(var onPicked: (PickedPhoto) -> Unit) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
        chatPhoto(image)?.let(onPicked)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

/** Длинная сторона до 1600 px и JPEG; при большом размере качество снижается до 3 МБ. */
@OptIn(ExperimentalForeignApi::class)
private fun chatPhoto(image: UIImage): PickedPhoto? {
    val (w, h) = image.size.useContents { width to height }
    val k = minOf(1.0, 1600.0 / maxOf(w, h))
    val tw = w * k
    val th = h * k
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(tw, th), true, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, tw, th))
    val result = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    var bytes = ByteArray(0)
    for (quality in doubleArrayOf(0.85, 0.72, 0.6, 0.45)) {
        val data = UIImageJPEGRepresentation(result ?: return null, quality) ?: return null
        bytes = data.bytes?.readBytes(data.length.toInt()) ?: return null
        if (bytes.size <= 3 * 1024 * 1024) break
    }
    return PickedPhoto(bytes, tw.toInt(), th.toInt())
}

private fun topViewController(): UIViewController? {
    var vc = (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    val delegate = remember { PickerDelegate { callback.value(it) } }
    return {
        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            this.delegate = delegate
        }
        topViewController()?.presentViewController(picker, true, null)
    }
}

@Composable
actual fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    val delegate = remember { PhotoDelegate { callback.value(it) } }
    return {
        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            this.delegate = delegate
        }
        topViewController()?.presentViewController(picker, true, null)
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

actual val swipeBackEnabled: Boolean = true

actual val pageTopExtraDp: Int = 0

actual val onboardingEnabled: Boolean = false

@Composable
actual fun currentAppVersion(): Int = Int.MAX_VALUE   // на iPhone обновления идут через Xcode/TestFlight: окно не показываем

@Composable
actual fun rememberUrlOpener(): (String) -> Unit = { url ->
    platform.Foundation.NSURL.URLWithString(url)?.let { platform.UIKit.UIApplication.sharedApplication.openURL(it) }
}
