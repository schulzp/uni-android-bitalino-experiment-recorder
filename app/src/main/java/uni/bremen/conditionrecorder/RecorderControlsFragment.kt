package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_recorder_controls.*

class RecorderControlsFragment : Fragment(), RecorderBus.Aware {

    override lateinit var recorderBus: RecorderBus

    private var disposable:Disposable? = null

    private var recording = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_controls, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordButton.setOnClickListener {
            recorderBus.post(
                    if (recording)
                        RecorderBus.StopRecordingVideo()
                    else
                        RecorderBus.StartRecordingVideo())
        }
    }

    override fun onResume() {
        super.onResume()

        disposable = recorderBus.eventSubject.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
            when(it) {
                is RecorderBus.VideoRecordingStarted -> {
                    recordButton.text = getString(R.string.stop)
                    recording = true
                }
                is RecorderBus.VideoRecordingStopped -> {
                    recordButton.text = getString(R.string.record)
                    recording = false
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    companion object {
        fun newInstance(bus: RecorderBus):RecorderControlsFragment = RecorderControlsFragment().also { it.recorderBus = bus }
    }

}