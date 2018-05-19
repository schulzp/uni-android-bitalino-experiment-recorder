package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit


abstract class BluetoothDeviceDiscovery(
        private val duration:Long = SCAN_DURATION,
        private val durationUnit:TimeUnit = TimeUnit.SECONDS) {

    private val subject = PublishSubject.create<BluetoothDevice>()

    private var started:Disposable? = null

    val devices = subject.doOnError{ t -> Log.e(TAG, "error: ${t.message}", t) }.doFinally(::onComplete).publish()

    fun isScanning(): Boolean = !subject.hasComplete()

    fun start() {
        if (started == null) {
            started = devices.connect()

            onStart()

            Completable.complete().delay(duration, durationUnit).subscribe(this::stop)
        }
    }

    fun stop() {
        if (started == null) {
            Log.w(TAG, "not started yet")
            return
        }

        subject.onComplete()

        started?.dispose()
    }

    protected fun onError(error:Throwable) {
        subject.onError(error)
    }

    protected fun onDevice(device: BluetoothDevice) {
        subject.onNext(device)
    }

    protected abstract fun onStart()

    protected abstract fun onComplete()

    companion object {

        const val TAG = "BTDD"

        const val SCAN_DURATION = 5L

    }

}