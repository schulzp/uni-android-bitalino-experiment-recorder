package uni.bremen.conditionrecorder

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.plux.pluxapi.Constants
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_recorder_device_list.*
import java.util.*


class RecorderDeviceListFragment : Fragment() {

    private lateinit var adapter:DeviceListAdapter

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_device_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DeviceListAdapter(activity!!, LinkedList())
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(activity!!)
        list.setHasFixedSize(true)
    }

    override fun onResume() {
        super.onResume()

        recorderServiceConnection = RecorderService.bind(context!!) { _, service ->
            val scheduler = AndroidSchedulers.mainThread()

            listOf(
                    service.bus.deviceSelected.subscribeOn(scheduler)
                            .subscribe { selected -> addDevice(selected.device) },
                    service.bus.deviceStateChange.subscribeOn(scheduler)
                            .subscribe { change -> updateDevice(change.device, change.state) })
        }

        empty.setOnClickListener {
            startActivityForResult(MainActivity.createPickDeviceIntent(context!!), INTENT_REQUEST_PICK_DEVICE)
        }
    }

    override fun onPause() {
        super.onPause()

        recorderServiceConnection.close(context!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INTENT_REQUEST_PICK_DEVICE && resultCode == Activity.RESULT_OK) {
            var device = data?.extras?.getParcelable<BluetoothDevice>(EXTRA_DEVICE)
            if (device != null) {
                recorderServiceConnection.whenConnected { _, service ->
                    service.bus.post(RecorderBus.SelectedDevice(device))
                }
            }
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        adapter.add(DeviceListAdapter.BITalinoBluetoothDevice(device))
        adapter.notifyDataSetChanged()
    }

    private fun updateDevice(device: BluetoothDevice, state:Any?) {
        val statefulBluetoothDevice = adapter.find(device)
        if (statefulBluetoothDevice != null) {
            when (statefulBluetoothDevice) {
                is DeviceListAdapter.BITalinoBluetoothDevice -> {
                    statefulBluetoothDevice.state = state as? Constants.States
                }
            }

            var viewHolder = list.findViewHolderForItemId(statefulBluetoothDevice.position.toLong()) as DeviceListAdapter.DeviceViewHolder
            adapter.onBindViewHolder(viewHolder, statefulBluetoothDevice, statefulBluetoothDevice.position)
        }
    }

    companion object {

        fun newInstance():RecorderDeviceListFragment = RecorderDeviceListFragment()

    }

}