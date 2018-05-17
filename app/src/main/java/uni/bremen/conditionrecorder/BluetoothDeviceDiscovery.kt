package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.util.Log
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


abstract class BluetoothDeviceDiscovery {

    private val handler = Handler()

    protected val devices: PublishSubject<BluetoothDevice> = PublishSubject.create<BluetoothDevice>()

    private var started = false

    fun isScanning(): Boolean = started && !devices.hasComplete()

    open fun start(duration:Long = SCAN_DURATION): Observable<BluetoothDevice> {
        if (started) throw IllegalStateException("already started")

        started = true

        devices.doFinally { Log.d("BTD", "discovery ended") }

        if (duration > 0) {
            handler.postDelayed(this::stop, duration)
        }

        return devices
    }

    open fun stop() {
        devices.onComplete()
    }

    open fun destroy() {

    }

    companion object {

        const val SCAN_DURATION = 20000L

    }

}