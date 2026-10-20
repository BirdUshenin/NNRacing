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
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

actual val swipeBackEnabled: Boolean = true
