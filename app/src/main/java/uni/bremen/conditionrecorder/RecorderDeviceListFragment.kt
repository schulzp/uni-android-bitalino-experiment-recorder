package uni.bremen.conditionrecorder

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_recorder_device_list.*
import java.util.*


class RecorderDeviceListFragment : ListFragment(), AdapterView.OnItemClickListener {

    private lateinit var adapter:DeviceListAdapter

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onResume() {
        super.onResume()

        recorderServiceConnection = RecorderService.bind(context!!) { _, service ->
            val scheduler = AndroidSchedulers.mainThread()

            listOf(
                    service.bus.deviceSelected.subscribeOn(scheduler)
                            .subscribe { selected -> adapter.add(selected.device) },
                    service.bus.bitalinoStateChange.subscribeOn(scheduler)
                            .subscribe { change -> adapter.notifyDataSetChanged() })
        }

        empty.setOnClickListener {
            startActivityForResult(MainActivity.createPickDeviceIntent(context!!), INTENT_REQUEST_PICK_DEVICE)
        }
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

    override fun onPause() {
        super.onPause()

        recorderServiceConnection?.close(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_device_list, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = DeviceListAdapter(activity!!, LinkedList())
        listView.adapter = adapter
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

    }

    companion object {

        fun newInstance():RecorderDeviceListFragment = RecorderDeviceListFragment()

    }

}