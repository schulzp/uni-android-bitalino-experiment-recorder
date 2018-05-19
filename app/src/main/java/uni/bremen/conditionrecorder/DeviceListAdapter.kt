package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.widget.TextView
import androidx.recyclerview.selection.StorageStrategy
import info.plux.pluxapi.Constants
import java.util.*


class DeviceListAdapter(context:Context, devices:MutableList<StatefulBluetoothDevice> = LinkedList()) : GenericRecycleViewAdapter<DeviceListAdapter.StatefulBluetoothDevice, String, DeviceListAdapter.DeviceViewHolder>(context, devices) {

    init {
        setHasStableIds(true)
    }

    override val itemKeyProvider = DeviceKeyProvider()

    override val itemKeyStorageStrategy = StorageStrategy.createStringStorage()

    override fun getItemViewResourceId(viewType: Int): Int = R.layout.item_device

    override fun onBindViewHolder(holder: DeviceViewHolder, item:StatefulBluetoothDevice, position: Int) {
        with(holder) {
            name.text = item.device.name ?: "BITalino"
            address.text = item.device.address

            item.update(this, context)
        }
    }

    fun indexOf(device: StatefulBluetoothDevice):Int {
        return items.indexOf(device)
    }

    fun find(device: BluetoothDevice) : DeviceListAdapter.StatefulBluetoothDevice? {
        return items.find { statefulBluetoothDevice -> statefulBluetoothDevice.device == device }
    }

    override fun add(item: StatefulBluetoothDevice) {
        super.add(item)

        item.position = items.size - 1
    }

    override fun createViewHolder(view:View):DeviceViewHolder {
        view.setPadding(0, 0, 0, 0)

        return DeviceViewHolder(view,
                view.findViewById(android.R.id.text1) as TextView,
                view.findViewById(android.R.id.text2) as TextView,
                view.findViewById(R.id.text3) as TextView)
    }

    override fun getItemViewType(position: Int): Int = items[position].type

    inner class DeviceKeyProvider : GenericItemKeyProvider<String>(0) {

        override fun getKey(position: Int): String = items[position].device.address

        override fun getPosition(key: String): Int {
            return items
                    .find { item -> item.device.address == key }
                    ?.let { item -> items.indexOf(item) } ?: -1
        }

    }

    class DeviceViewHolder(view:View, val name: TextView, val address: TextView, val state: TextView) : GenericRecycleViewAdapter.GenericViewHolder<String>(view, GenericRecycleViewAdapter.GenericItemDetails("", -1))

    interface StatefulBluetoothDevice {
        val device: BluetoothDevice
        val type:Int
        var position:Int
        var state:Recorder.State
        var batteryLevel:Recorder.BatteryLevel

        fun update(deviceViewHolder: DeviceViewHolder, context: Context)
    }

    class RecorderBluetoothDevice(
            override val device: BluetoothDevice,
            override var state:Recorder.State = Recorder.State.DISCONNECTED,
            override var batteryLevel: Recorder.BatteryLevel = Recorder.BatteryLevel.UNKNOWN)
        : StatefulBluetoothDevice {

        override val type = 0

        override var position = -1

        override fun hashCode(): Int {
            return Objects.hashCode(device)
        }

        override fun equals(other: Any?): Boolean {
            return super.equals(other) || other is RecorderBluetoothDevice && Objects.equals(device, other.device)
        }

        override fun update(deviceViewHolder: DeviceViewHolder, context: Context) {
            with(deviceViewHolder) {
                val connectionColor = context.getColor(when(this@RecorderBluetoothDevice.state) {
                    Recorder.State.CONNECTED -> R.color.deviceStatusConnected
                    Constants.States.DISCONNECTED -> R.color.deviceStatusDisconnected
                    else -> R.color.accent
                })

                state.compoundDrawables[0]?.setColorFilter(connectionColor, PorterDuff.Mode.MULTIPLY)
                state.text = this@RecorderBluetoothDevice.state?.name

                val batteryColor = context.getColor(when(this@RecorderBluetoothDevice.batteryLevel) {
                    Recorder.BatteryLevel.UNKNOWN -> R.color.batteryUnkown
                    Recorder.BatteryLevel.CRITICAL -> R.color.batteryCritical
                    Recorder.BatteryLevel.LOW -> R.color.batteryLow
                    Recorder.BatteryLevel.GOOD -> R.color.batteryGood
                })

                name.compoundDrawables[2]?.setColorFilter(batteryColor, PorterDuff.Mode.MULTIPLY)
            }
        }
    }

}