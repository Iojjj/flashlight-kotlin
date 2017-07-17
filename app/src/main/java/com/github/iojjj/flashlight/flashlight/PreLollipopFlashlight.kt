package com.github.iojjj.flashlight.flashlight

import android.hardware.Camera
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Implementation of flashlight for devices with Android KitKat and lower versions installed. Uses android.hardware.Camera API's.
 */
@Suppress("DEPRECATION")
class PreLollipopFlashlight : AbstractFlashlight() {

    private var _camera: Camera? = null
    private var _flashlightDisposable: Disposable? = null

    /**
     * Cached observable that returns first available camera id with flashlight.
     */
    private val flashlightObservable = Observable.create<Int?> {
        val result = (0..Camera.getNumberOfCameras() - 1)
                .map {
                    try {
                        val camera = Camera.open(it)
                        val res = camera.parameters?.supportedFlashModes?.isNotEmpty() ?: false
                        camera.release()
                        if (res) it else null
                    } catch (e: RuntimeException) {
                        e.printStackTrace()
                        null
                    }
                }
                .filterNotNull()
                .firstOrNull()
        if (result != null) {
            it.onNext(result)
        }
        it.onComplete()
    }.cache()

    override fun onStart() {
        _flashlightDisposable = flashlightObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    _camera = Camera.open(it)
                    _camera?.let {
                        with(it) {
                            startPreview()
                            _onInitialized.onNext(Unit)
                        }
                    }
                }
    }

    override fun onStop() {
        _flashlightDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        _camera?.let {
            with(it) {
                stopPreview()
                release()
            }
        }
    }

    override fun enable(enable: Boolean) {
        val params = _camera?.parameters
        try {
            if (enable) {
                params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            } else {
                params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
            }
            _camera?.parameters = params
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    override fun isSupported(): Observable<Boolean> = flashlightObservable
            .map { true }
            .defaultIfEmpty(false)
}