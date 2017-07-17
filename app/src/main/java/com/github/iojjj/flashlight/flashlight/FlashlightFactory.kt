package com.github.iojjj.flashlight.flashlight

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.github.iojjj.flashlight.extensions.isLollipop
import com.github.iojjj.flashlight.extensions.isMarshmallowAndUpper

/**
 * Factory that creates flashlight depending on Android version.
 */
object FlashlightFactory {

    /**
     * Create a new instance of flashlight.
     */
    @SuppressLint("NewApi")
    fun newInstance(context: Context, sdkVersion: Int = Build.VERSION.SDK_INT): Flashlight {
        return when {
            sdkVersion.isLollipop() -> LollipopFlashlight(context)
            sdkVersion.isMarshmallowAndUpper() -> MarshmallowFlashlight(context)
            else -> PreLollipopFlashlight()
        }
    }
}