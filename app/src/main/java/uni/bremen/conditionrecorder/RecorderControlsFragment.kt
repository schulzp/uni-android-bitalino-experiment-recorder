package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_recorder_controls.*

class RecorderControlsFragment : Fragment() {

    private var recording = false

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorderServiceConnection = RecorderService.bind(activity!!) { _, service ->
            service.bus.recording.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                when(it) {
                    is RecorderBus.RecordingStared -> {
                        recordButton.text = getString(R.string.stop)
                        recording = true
                    }
                    is RecorderBus.RecordingStopped -> {
                        recordButton.text = getString(R.string.record)
                        recording = false
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        recorderServiceConnection?.close(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_controls, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordButton.setOnClickListener {
                    if (recording)
                        recorderServiceConnection
                                .whenConnected { _, service ->
                                    service.bus.post(RecorderBus.StopRecording(), RecorderBus.BitalinoRecordingStopped()) }
                    else
                        recorderServiceConnection
                                .whenConnected { _, service ->
                                    service.bus.post(RecorderBus.StartRecording(), RecorderBus.BitalinoRecordingStarted()) }
        }
    }

    companion object {
        fun newInstance():RecorderControlsFragment = RecorderControlsFragment()
    }

}