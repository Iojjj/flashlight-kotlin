package com.github.iojjj.flashlight.extensions

import android.os.Build

/**
 * Check if the integer is in Lollipop version codes range.
 */
fun Int.isLollipop() = this >= Build.VERSION_CODES.LOLLIPOP && this < Build.VERSION_CODES.M

/**
 * Check if the integer is equals or greater than Marshmallow version code.
 */
fun Int.isMarshmallowAndUpper() = this >= Build.VERSION_CODES.M