package uni.bremen.conditionrecorder

import android.os.Bundle
import android.os.Handler
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import android.opengl.ETC1.getWidth



class RecorderFragment : Fragment() {

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorderServiceConnection = RecorderService.bind(activity!!) { _, service ->
            service.bus.recordingStopped.subscribeOn(AndroidSchedulers.mainThread()).subscribe { event ->
                showToast(event.videoRecordingStopped.path)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        recorderServiceConnection?.close(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder, container, false)

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

        with(activity as MainActivity) {
            title = resources.getString(R.string.recorder)
            setFullScreen(true)
            setDrawerEnabled(false)
            updateDrawerMenu(-1)

            with(getFab()) {
                hide()
            }

            val display = windowManager.defaultDisplay
            val width = display.width

            view?.layoutParams?.width = width
            view?.requestLayout()
        }

    }

    private fun showToast(message : String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    companion object {
        fun newInstance():RecorderFragment = RecorderFragment()
    }

}