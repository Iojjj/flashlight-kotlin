package com.github.iojjj.flashlight.flashlight

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Implementation of flashlight for devices with Android Marshmallow and greater versions installed. Uses Marshmallow API's.
 */
@TargetApi(Build.VERSION_CODES.M)
class MarshmallowFlashlight(context: Context): AbstractLollipopFlashlight(context) {

    override fun newOnInitialized(): Subject<Unit> = BehaviorSubject.createDefault(Unit)

    override fun onStart() {
        // do nothing
    }

    override fun onStop() {
        // do nothing
    }

    override fun enable(enable: Boolean) {
        val id = getFirstCameraIdWithFlash() ?: return
        cameraManager.setTorchMode(id, enable)
    }

    override fun isSupported(): Observable<Boolean> = Observable.fromCallable { getFirstCameraIdWithFlash() != null }

}