package com.github.iojjj.flashlight

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CheckBox
import com.github.iojjj.flashlight.flashlight.Flashlight
import com.github.iojjj.flashlight.flashlight.FlashlightFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var _onOffBtn: View
    private lateinit var _stroboscope: CheckBox
    private lateinit var _flashlight: Flashlight

    private var _isFlashlightSupported = true
    private var _isFlashlightEnabled = false
    private var _isStroboscopeEnabled = false
    private var _onPermissionsGranted = BehaviorSubject.create<Unit>()
    private var _onStart = PublishSubject.create<Unit>()
    private var _onStop = PublishSubject.create<Unit>()
    private var _permissionDialog: AlertDialog? = null
    private var _stroboscopeDisposable: Disposable? = null

    private companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _onOffBtn = findViewById(R.id.btn_on_off)
        _stroboscope = findViewById(R.id.cb_stroboscope) as CheckBox

        _onOffBtn.setOnClickListener { onFlashlightOnOffClicked() }
        _onOffBtn.isEnabled = _isFlashlightSupported
        _stroboscope.setOnCheckedChangeListener { _, isChecked -> onStroboscopeEnableChanged(isChecked) }

        _flashlight = FlashlightFactory.newInstance(this, Build.VERSION_CODES.KITKAT)
        _flashlight.isSupported()
                .subscribeOn(Schedulers.io())
                .filter { !it }
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onFlashlightNotSupported() }
    }

    private fun checkCameraPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            _onPermissionsGranted.onNext(Unit)
            return
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            val listener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> requestCameraPermissions()
                }
                dialog.dismiss()
            }
            if (_permissionDialog == null) {
                _permissionDialog = AlertDialog.Builder(this)
                        .setTitle("Camera Permissions")
                        .setMessage("Please grant the permission to your camera so that application can access flashlight of your device.")
                        .setPositiveButton("Continue", listener)
                        .setNegativeButton("Cancel", listener)
                        .setOnDismissListener { _permissionDialog = null }
                        .show()
            }
            return
        }
        requestCameraPermissions()
    }

    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
    }

    override fun onStart() {
        super.onStart()
        // call flashlight's onStart only after permissions granted and onStart called
        Observable
                .zip(_onPermissionsGranted, _onStart, BiFunction<Unit, Unit, Unit> { _, _ -> })
                .takeUntil(_onStop)
                .subscribe {
                    _flashlight.onStart()
                }
        Observable
                .zip(_flashlight.onInitialized, _onPermissionsGranted, BiFunction<Unit, Unit, Unit> { _, _ -> })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _onOffBtn.isEnabled = _isFlashlightSupported
                })
        _onStart.onNext(Unit)
        checkCameraPermissions()
    }

    override fun onStop() {
        super.onStop()
        _onStop.onNext(Unit)
        _flashlight.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _onPermissionsGranted.onNext(Unit)
                }
            }
        }
    }

    private fun onFlashlightOnOffClicked() {
        // disable flashlight if enabled
        if (_isFlashlightEnabled) {
            stopStroboscopeTask()
            toggleFlashlight(false)
            return
        }
        // start stroboscope task
        if (_isStroboscopeEnabled) {
            startStroboscopeTask()
            return
        }
        // just enable flashlight
        toggleFlashlight(true)
    }

    private fun startStroboscopeTask() {
        _stroboscopeDisposable = Observable
                .interval(0, 100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe { toggleFlashlight() }
    }

    private fun stopStroboscopeTask() {
        _stroboscopeDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        _stroboscopeDisposable = null
    }

    private fun onStroboscopeEnableChanged(enabled: Boolean) {
        _isStroboscopeEnabled = enabled
        _onOffBtn.isEnabled = _isFlashlightSupported && !enabled
        if (_isStroboscopeEnabled) {
            startStroboscopeTask()
        } else {
            stopStroboscopeTask()
            toggleFlashlight(false)
        }
    }

    private fun toggleFlashlight(enabled: Boolean = !_isFlashlightEnabled) {
        _isFlashlightEnabled = enabled
        _flashlight.enable(_isFlashlightEnabled)
    }

    private fun onFlashlightNotSupported() {
        _isFlashlightSupported = false
        _onOffBtn.isEnabled = false
    }
}
