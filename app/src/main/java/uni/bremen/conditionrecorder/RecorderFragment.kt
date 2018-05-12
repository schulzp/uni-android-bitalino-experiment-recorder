package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers


class RecorderFragment : ContentFragment(Content.RECORDER, R.string.recorder), Fullscreen {

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorderServiceConnection = RecorderService.bind(activity!!) { _, service ->
            service.bus.recordingStopped.subscribeOn(AndroidSchedulers.mainThread()).subscribe { event ->
                showToast(event.videoRecordingStopped.path)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        withObserver()?.onContentResumed(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        recorderServiceConnection.close(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder, container, false)
            .also { it.fitsSystemWindows = false }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.beginTransaction()
                .replace(R.id.videoPlaceholder, RecorderVideoFragment.newInstance())
                .replace(R.id.devicesPlaceholder, RecorderDeviceListFragment.newInstance())
                .replace(R.id.controlsPlaceholder, RecorderControlsFragment.newInstance())
                .commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        withObserver()?.onContentCreated(this)

        with(activity as MainActivity) {
            getFab().hide()
        }
    }

    private fun showToast(message : String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    companion object {
        fun newInstance():RecorderFragment = RecorderFragment()
    }

}