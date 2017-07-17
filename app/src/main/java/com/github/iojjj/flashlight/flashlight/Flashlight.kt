package com.github.iojjj.flashlight.flashlight

import io.reactivex.Observable

/**
 * Flashlight interface.
 */
interface Flashlight {

    /**
     * Call this method in activity's or fragment's onStart() method.
     */
    fun onStart()

    /**
     * Call this method in activity's or fragment's onStop() method.
     */
    fun onStop()

    /**
     * Enable or disable flashlight.
     *
     * @param enable true to enable, false to disable
     */
    fun enable(enable: Boolean)

    /**
     * Check if flashlight is supported on current device. Emits true if supported, false otherwise
     */
    fun isSupported(): Observable<Boolean>

    /**
     * Event called when flashlight is initialized and ready to receive inputs.
     */
    val onInitialized: Observable<Unit>
}