package uni.bremen.conditionrecorder.wahoo

import com.wahoofitness.connector.HardwareConnector
import uni.bremen.conditionrecorder.RecorderService
import uni.bremen.conditionrecorder.bitalino.Recorder

class WahooRecorder(private val service: RecorderService) : Recorder {

    private val callback = object : DefaultCallback() { }

    private var connector = HardwareConnector(service, callback)

    override fun connect() {

    }

    override fun disconnect() {
        connector.shutdown()
    }


    override fun getState(): Recorder.State {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}