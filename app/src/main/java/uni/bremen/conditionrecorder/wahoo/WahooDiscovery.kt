package uni.bremen.conditionrecorder.wahoo

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.wahoofitness.connector.HardwareConnector
import com.wahoofitness.connector.HardwareConnectorTypes
import com.wahoofitness.connector.conn.connections.params.BTLEConnectionParams
import com.wahoofitness.connector.conn.connections.params.ConnectionParams
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener
import io.reactivex.Observable
import uni.bremen.conditionrecorder.BluetoothDeviceDiscovery

class WahooDiscovery(context: Context) : BluetoothDeviceDiscovery() {

    private val callback = object : DefaultCallback() { }

    private val connector = HardwareConnector(context, callback)

    private val listener = object : DiscoveryListener {

        override fun onDiscoveredDeviceRssiChanged(connectionParams: ConnectionParams, rssi: Int) {

        }

        override fun onDiscoveredDeviceLost(connectionParams: ConnectionParams) {

        }

        override fun onDeviceDiscovered(connectionParams: ConnectionParams) {
            if (connectionParams is BTLEConnectionParams) {
                devices?.onNext(connectionParams.bluetoothDevice)
                Log.d(TAG, "discovered wahoo device: $connectionParams")
            }
        }

    }

    override fun start(duration:Long): Observable<BluetoothDevice> {
        val subject = super.start(duration)

        connector.startDiscovery(
                HardwareConnectorTypes.SensorType.HEARTRATE,
                HardwareConnectorTypes.NetworkType.BTLE, listener)

        return subject
    }

    override fun stop() {
        connector.stopDiscovery(HardwareConnectorTypes.NetworkType.BTLE)
        super.stop()
    }

    companion object {

        const val TAG = "WahooDiscovery"

    }

}