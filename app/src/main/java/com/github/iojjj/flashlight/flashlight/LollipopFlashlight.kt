package com.github.iojjj.flashlight.flashlight

import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import io.reactivex.Observable

/**
 * Implementation of flashlight for devices with Android Lollipop versions installed. Uses android.hardware.Camera2 API's.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopFlashlight(context: Context) : AbstractLollipopFlashlight(context), Flashlight {

    private lateinit var _backgroundThread: HandlerThread
    private lateinit var _backgroundHandler: Handler
    private lateinit var _imageReader: ImageReader

    private var _cameraDevice: CameraDevice? = null
    private var _captureBuilder: CaptureRequest.Builder? = null
    private var _captureSession: CameraCaptureSession? = null

    private val _sessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession?) {
            _cameraDevice?.let {
                _captureSession = session
                _captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                _captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                _captureSession?.setRepeatingRequest(_captureBuilder?.build(), null, null)
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession?) {
            // do nothing
        }
    }

    private val _stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(device: CameraDevice?) {
            _cameraDevice = device
            _cameraDevice?.let {
                val imageReaderInitialized = initImageReader(it.id)
                if (!imageReaderInitialized) {
                    return@let
                }
                val builder = it.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                _captureBuilder = builder
                builder.addTarget(_imageReader.surface)
                val surfaces = listOf(_imageReader.surface)
                it.createCaptureSession(surfaces, _sessionCallback, _backgroundHandler)
                _onInitialized.onNext(Unit)
            }
        }

        override fun onDisconnected(device: CameraDevice?) {
            nullAll()
        }

        override fun onError(device: CameraDevice?, errorCode: Int) {
            nullAll()
        }

    }

    override fun onStart() {
        val id = getFirstCameraIdWithFlash() ?: return
        startBackgroundThread()
        cameraManager.openCamera(id, _stateCallback, _backgroundHandler)
    }

    override fun onStop() {
        _cameraDevice?.close()
        stopBackgroundThread()
        nullAll()
    }

    override fun enable(enable: Boolean) {
        if (enable) {
            _captureBuilder?.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
        } else {
            _captureBuilder?.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }
        _captureSession?.setRepeatingRequest(_captureBuilder?.build(), null, null)
    }

    override fun isSupported(): Observable<Boolean> = Observable.fromCallable { getFirstCameraIdWithFlash() != null }

    /**
     * Set values to null.
     */
    private fun nullAll() {
        _cameraDevice = null
        _captureSession = null
        _captureBuilder = null
    }

    /**
     * Create and start background thread.
     */
    private fun startBackgroundThread() {
        _backgroundThread = HandlerThread("CameraHandler")
        _backgroundThread.start()
        _backgroundHandler = Handler(_backgroundThread.looper)
    }

    /**
     * Stop background thread.
     */
    private fun stopBackgroundThread() {
        _backgroundThread.quitSafely()
    }

    /**
     * Initialize instance of ImageReader.
     */
    private fun initImageReader(cameraId: String): Boolean {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        val outputSizes = map.getOutputSizes(ImageFormat.JPEG)
        if (outputSizes.isEmpty()) {
            return false
        }
        val minSize = outputSizes.sortedBy { size -> size.width * size.height }[0]
        _imageReader = ImageReader.newInstance(minSize.width, minSize.height, ImageFormat.JPEG, 2)
        _imageReader.setOnImageAvailableListener({ it.acquireLatestImage().close() }, _backgroundHandler)
        return true
    }
}