package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import info.plux.pluxapi.Constants
import java.util.*


class DeviceListAdapter(val context:Context, val devices:MutableList<StatefulBluetoothDevice<*>>) : BaseAdapter() {

    class DeviceViewHolder(val name: TextView, val address: TextView, val state: TextView)

    fun find(device: BluetoothDevice) : StatefulBluetoothDevice<*>? {
        return devices.find { statefulBluetoothDevice -> statefulBluetoothDevice.device == device }
    }

    fun add(device: StatefulBluetoothDevice<*>) {
        if (!devices.contains(device)) {
            devices.add(device)
        }
    }

    fun update(viewHolder: DeviceViewHolder, statefulBluetoothDevice: StatefulBluetoothDevice<*>) {
        with(viewHolder) {
            name.text = statefulBluetoothDevice.device.name ?: "BITalino"
            address.text = statefulBluetoothDevice.device.address

            statefulBluetoothDevice.update(this, context)
        }
    }

    fun clear() {
        devices.clear()
    }

    override fun getCount(): Int = devices.size

    override fun getItem(i: Int): StatefulBluetoothDevice<*> = devices[i]

    override fun getItemId(i: Int): Long = i.toLong()

    override fun getView(position: Int, existingView: View?, parent: ViewGroup?): View {
        val view = existingView ?: LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        view.setPadding(0, 0, 0, 0)

        val statefulBluetoothDevice = getItem(position)
        statefulBluetoothDevice.position = position

        update(getDeviceViewHolder(view), statefulBluetoothDevice)

        return view
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    private fun getDeviceViewHolder(view:View): DeviceViewHolder {
        var deviceViewHolder = view.getTag(R.id.list_item_view_handler)
        if (deviceViewHolder == null) {
            deviceViewHolder = DeviceViewHolder(
                    view.findViewById(android.R.id.text1) as TextView,
                    view.findViewById(android.R.id.text2) as TextView,
                    view.findViewById(R.id.deviceState) as TextView)

            view.setTag(R.id.list_item_view_handler, deviceViewHolder)
        }
        return deviceViewHolder as DeviceViewHolder
    }



    interface StatefulBluetoothDevice<S> {
        val device: BluetoothDevice
        val type:Int
        var position:Int
        var state:S?

        fun update(deviceViewHolder: DeviceViewHolder, context: Context)
    }

    class BITalinoBluetoothDevice(override val device: BluetoothDevice, override var state:Constants.States? = null) : StatefulBluetoothDevice<Constants.States> {

        override val type = 0

        override var position = -1

        override fun hashCode(): Int {
            return Objects.hashCode(device)
        }

        override fun equals(other: Any?): Boolean {
            return super.equals(other) || other is BITalinoBluetoothDevice && Objects.equals(device, other.device)
        }

        override fun update(deviceViewHolder: DeviceViewHolder, context: Context) {
            with(deviceViewHolder) {
                val color = context.getColor(when(this@BITalinoBluetoothDevice.state) {
                    Constants.States.CONNECTED -> R.color.deviceStatusConnected
                    Constants.States.DISCONNECTED -> R.color.deviceStatusDisconnected
                    else -> R.color.accent
                })

                state.compoundDrawables[0]?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                state.text = this@BITalinoBluetoothDevice.state?.name
            }
        }

    }

}