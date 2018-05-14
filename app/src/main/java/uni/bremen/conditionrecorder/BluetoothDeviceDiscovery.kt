package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Handler
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


abstract class BluetoothDeviceDiscovery {

    private val handler = Handler()

    protected var devices: PublishSubject<BluetoothDevice>? = null
        private set(value) { field = value }

    fun isScanning(): Boolean = devices?.hasComplete() ?: false

    open fun start(duration:Long = SCAN_DURATION): Observable<BluetoothDevice> {
        val devices = createSubject()

        if (duration > 0) {
            handler.postDelayed(this::stop, duration)
        }

        this.devices = devices

        return devices
    }

    open fun stop() {
        devices?.onComplete()
    }

    open fun destroy() {

    }

    private fun createSubject() = PublishSubject.create<BluetoothDevice>()

    companion object {

        const val SCAN_DURATION = 100000L

    }

}