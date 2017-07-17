package com.github.iojjj.flashlight.flashlight

import android.annotation.TargetApi
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/**
 * Abstract implementation of flashlight for Lollipop and upper versions.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
abstract class AbstractLollipopFlashlight(context: Context): AbstractFlashlight() {

    protected val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    protected fun getFirstCameraIdWithFlash(): String? =
            cameraManager.cameraIdList
                    .filter {
                        val char = cameraManager.getCameraCharacteristics(it)
                        char[CameraCharacteristics.FLASH_INFO_AVAILABLE] ?: false
                    }
                    .firstOrNull()
}