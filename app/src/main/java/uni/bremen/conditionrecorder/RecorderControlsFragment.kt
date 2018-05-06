package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_recorder_controls.*

class RecorderControlsFragment : Fragment() {

    private var recording = false

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorderServiceConnection = RecorderService.bind(activity!!).also { connection ->
            connection.connected {
                connection.dispose(it.recording.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
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
                })
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
                        recorderServiceConnection?.service?.recorderBus?.post(RecorderBus.StopRecording(), RecorderBus.SignalRecordingStopped())
                    else
                        recorderServiceConnection?.service?.recorderBus?.post(RecorderBus.StartRecording(), RecorderBus.SignalRecordingStarted())
        }
    }

    companion object {
        fun newInstance():RecorderControlsFragment = RecorderControlsFragment()
    }

}