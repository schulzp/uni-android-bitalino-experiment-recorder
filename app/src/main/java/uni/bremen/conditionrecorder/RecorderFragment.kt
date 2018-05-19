package uni.bremen.conditionrecorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import uni.bremen.conditionrecorder.service.BindableServiceConnection
import uni.bremen.conditionrecorder.service.RecorderService


class RecorderFragment : ContentFragment(Content.RECORDER, R.string.recorder), Fullscreen {

    private lateinit var recorderServiceConnection: BindableServiceConnection<RecorderService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorderServiceConnection = RecorderService.bind(activity!!)
        recorderServiceConnection.service.subscribe { service ->
            service.bus.commands.onNext(RecorderBus.CreateSession())

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

        recorderServiceConnection.service.subscribe { service ->
            service.bus.commands.onNext(RecorderBus.DestroySession())
        }

        recorderServiceConnection.close()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder, container, false)
            .also { it.fitsSystemWindows = false }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.beginTransaction()
                .replace(R.id.videoPlaceholder, RecorderVideoFragment())
                .replace(R.id.devicesPlaceholder, RecorderDeviceListFragment())
                .replace(R.id.controlsPlaceholder, RecorderControlsFragment())
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

}