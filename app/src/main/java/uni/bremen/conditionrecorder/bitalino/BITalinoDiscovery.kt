package uni.bremen.conditionrecorder.bitalino

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import info.plux.pluxapi.BTHDeviceScan
import info.plux.pluxapi.Constants
import io.reactivex.Observable
import uni.bremen.conditionrecorder.BluetoothDeviceDiscovery

class BITalinoDiscovery(context: Context) : BluetoothDeviceDiscovery() {

    private var bthDeviceScan = BTHDeviceScan(context)

    private val scanDevicesUpdateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == Constants.ACTION_MESSAGE_SCAN) {
                val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_DEVICE_SCAN)

                if (bluetoothDevice != null) {
                    devices?.onNext(bluetoothDevice)

                    Log.d(TAG, "found bitalino device: $bluetoothDevice")
                }
            }
        }

    }

    init {
        context.registerReceiver(scanDevicesUpdateReceiver, IntentFilter(Constants.ACTION_MESSAGE_SCAN))
    }

    private var onDestroy = { context.unregisterReceiver(scanDevicesUpdateReceiver); bthDeviceScan.closeScanReceiver() }

    override fun start(duration:Long): Observable<BluetoothDevice> {
        val subject = super.start(duration)

        bthDeviceScan.doDiscovery()

        return subject
    }

    override fun stop() {
        bthDeviceScan.stopScan()
    }

    override fun destroy() {
        super.destroy()

        onDestroy()
    }

    companion object {

        const val TAG = "BITalinoDiscovery"

    }

}