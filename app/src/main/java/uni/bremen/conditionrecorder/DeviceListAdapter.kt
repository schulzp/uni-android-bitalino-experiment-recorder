package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


class DeviceListAdapter(val context:Context, val devices:MutableList<BluetoothDevice>) : BaseAdapter() {

    class DeviceViewHolder(val name: TextView, val address: TextView, val state: TextView)

    fun add(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            devices.add(device)
            notifyDataSetChanged()
        }
    }

    fun clear() {
        devices.clear()
    }

    override fun getCount(): Int {
        return devices.size
    }

    override fun getItem(i: Int): BluetoothDevice {
        return devices[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(position: Int, existingView: View?, parent: ViewGroup?): View {
        val view = existingView ?: LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)

        with(getDeviceViewHolder(view)) {
            val device = getItem(position)

            name.text = device.name ?: "BITalino"
            address.text = device.address

            var color = context.getColor(if (device.bondState == BluetoothDevice.BOND_BONDED) R.color.connected else R.color.disconnected)
            state.compoundDrawables[0].setColorFilter(color, PorterDuff.Mode.MULTIPLY)
            state.text = if (device.bondState == BluetoothDevice.BOND_BONDED) "C" else "D"
        }

        return view
    }

    private fun getDeviceViewHolder(view:View): DeviceViewHolder {
        var deviceViewHolder = view.getTag(R.id.list_item_view_handler)
        if (deviceViewHolder == null) {
            deviceViewHolder = DeviceViewHolder(
                    view.findViewById(android.R.id.text1) as TextView,
                    view.findViewById(android.R.id.text2) as TextView,
                    view.findViewById(R.id.device_state) as TextView)

            view.setTag(R.id.list_item_view_handler, deviceViewHolder)
        }
        return deviceViewHolder as DeviceViewHolder
    }

}