package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_recorder_controls.*

class RecorderControlsFragment : Fragment() {

    private var recording = false

    private val phaseButtons = ArrayList<ToggleButton>()

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorderServiceConnection = RecorderService.bind(activity!!) { _, service ->
            service.bus.recorderSession.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                when(it.state) {
                    RecorderSession.State.NOT_READY -> {
                        if (!recording) {
                            disable()
                        }
                    }
                    RecorderSession.State.RECORDING -> {
                        phaseButtons.first().isEnabled = true
                        phaseButtons.forEach { button -> button.isChecked = false }
                        recordButton.text = getString(R.string.stop)
                        recording = true
                    }
                    RecorderSession.State.READY -> {
                        recordButton.isEnabled = true
                        phaseButtons.forEach { button -> button.isEnabled = false }
                        recordButton.text = getString(R.string.record)
                        recording = false
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        recorderServiceConnection.close(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_controls, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preparePhaseButtons()

        recordButton.setOnClickListener {
                    if (recording)
                        recorderServiceConnection
                                .whenConnected { _, service ->
                                    service.bus.commands.onNext(RecorderBus.StopRecording()) }
                    else
                        recorderServiceConnection
                                .whenConnected { _, service ->
                                    service.bus.commands.onNext(RecorderBus.StartRecording()) }
        }
    }

    private fun preparePhaseButtons() {
        preparePhaseButton(phaseButton1, 1)
        preparePhaseButton(phaseButton2, 2)
        preparePhaseButton(phaseButton3, 3)
    }

    private fun preparePhaseButton(button: ToggleButton, phase: Int) {
        phaseButtons.add(button)

        val next = phaseButtons.size

        button.isEnabled = false

        button.setOnClickListener {
            it.isEnabled = false

            if (next < phaseButtons.size) phaseButtons[next].isEnabled = true

            recorderServiceConnection.whenConnected { _, service ->
                service.bus.events.onNext(RecorderBus.PhaseSelected(phase))
            }
        }
    }

    private fun disable() {
        phaseButtons.forEach { button -> button.isEnabled = false }
        recordButton.isEnabled = false
    }

}