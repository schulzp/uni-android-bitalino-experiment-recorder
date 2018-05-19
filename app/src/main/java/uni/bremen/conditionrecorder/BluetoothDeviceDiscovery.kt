package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.subjects.PublishSubject


abstract class BluetoothDeviceDiscovery() {

    private val devices: PublishSubject<BluetoothDevice> = PublishSubject.create<BluetoothDevice>()

    private var started = false

    val discovery = devices.doOnSubscribe { _ ->
        if (!started) {
            started = true
            onStart()
        }
    }.doFinally(::onComplete)

    fun isScanning(): Boolean = started && !devices.hasComplete()

    fun stop() { if (!started) Log.w("BTDD", "not started yet"); devices.onComplete() }

    protected fun onDevice(device: BluetoothDevice) {
        devices.onNext(device)
    }

    protected abstract fun onStart()

    protected abstract fun onComplete()

    companion object {

        const val SCAN_DURATION = 20000L

    }

}