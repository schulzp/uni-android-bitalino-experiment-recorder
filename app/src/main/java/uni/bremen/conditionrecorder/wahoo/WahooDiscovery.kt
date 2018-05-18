package uni.bremen.conditionrecorder.wahoo

import android.content.Context
import android.os.Handler
import android.util.Log
import com.wahoofitness.connector.HardwareConnector
import com.wahoofitness.connector.HardwareConnectorTypes
import com.wahoofitness.connector.conn.connections.params.BTLEConnectionParams
import com.wahoofitness.connector.conn.connections.params.ConnectionParams
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener
import io.reactivex.Scheduler
import uni.bremen.conditionrecorder.BluetoothDeviceDiscovery

class WahooDiscovery(context: Context) : BluetoothDeviceDiscovery() {

    private val callback = object : DefaultCallback() { }

    private val connector = HardwareConnector(context, callback)

    private val listener = object : DiscoveryListener {

        override fun onDiscoveredDeviceRssiChanged(connectionParams: ConnectionParams, rssi: Int) { }

        override fun onDiscoveredDeviceLost(connectionParams: ConnectionParams) { }

        override fun onDeviceDiscovered(connectionParams: ConnectionParams) {
            if (connectionParams is BTLEConnectionParams) {
                Log.d(TAG, "discovered wahoo device: $connectionParams")
                onDevice(connectionParams.bluetoothDevice)
            }
        }

    }

    override fun onStart() {
        connector.startDiscovery(
                HardwareConnectorTypes.SensorType.HEARTRATE,
                HardwareConnectorTypes.NetworkType.BTLE, listener)
    }

    override fun onComplete() {
        connector.stopDiscovery(HardwareConnectorTypes.NetworkType.BTLE)
        connector.shutdown()
    }

    companion object {

        const val TAG = "WahooDiscovery"

    }

}