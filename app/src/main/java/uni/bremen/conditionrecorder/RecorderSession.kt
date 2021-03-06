package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Environment
import android.util.Log
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import uni.bremen.conditionrecorder.bitalino.BITalinoRecorder
import uni.bremen.conditionrecorder.io.DataAggregator
import uni.bremen.conditionrecorder.io.DataWriter
import uni.bremen.conditionrecorder.service.RecorderService
import uni.bremen.conditionrecorder.wahoo.WahooRecorder
import java.io.File
import java.util.*

class RecorderSession(private val service: RecorderService, private val scheduler: Scheduler) {

    enum class State {
        NOT_READY, READY, RECORDING;

        companion object {

            fun map(state: Recorder.State):State {
                return when(state) {
                    Recorder.State.CONNECTED -> State.READY
                    Recorder.State.RECORDING -> State.RECORDING
                    else -> State.NOT_READY
                }
            }
        }
    }

    private val sessionDisposables = CompositeDisposable()

    private var aggregator = DataAggregator()

    private val recorders = HashMap<BluetoothDevice, Recorder>()

    fun create() {
        sessionDisposables.add(service.bus.commands.observeOn(scheduler).subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.StartRecording -> startRecording()
                        is RecorderBus.StopRecording -> stopRecording()
                    }
                })
        sessionDisposables.add(service.bus.events.observeOn(scheduler).subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.RecorderStateChanged -> updateState()
                        is RecorderBus.SelectedDevice -> createRecorder(it.device)
                        is RecorderBus.PhaseSelected -> aggregator.phase.set(it.phase)
                    }
                })
        Log.d(TAG, "created")
    }

    fun destroy() {
        stopRecording()
        recorders.values.forEach(Recorder::disconnect)
        sessionDisposables.dispose()
        Log.d(TAG, "destroyed")
    }

    private fun startRecording() {
        if (recorders.isEmpty()) throw IllegalStateException("no recorder available")

        Log.d(TAG, "started recording")

        aggregator = DataAggregator()

        recorders.values.forEach(this::startRecorder)
    }

    private fun startRecorder(recorder: Recorder) {
        var disposable: Disposable? = null

        if (recorder is BITalinoRecorder) {
            val file = getDataOutputFile()
            val writer = DataWriter(file)
            Log.d(TAG, "writing to $file")

            disposable = recorder.frames
                    .doOnError { error ->
                        Log.e(TAG, "data.bitalino error", error)
                    }
                    .doFinally {
                        disposable?.dispose()
                        writer.close()
                    }
                    .observeOn(scheduler)
                    .subscribeOn(scheduler)
                    .map(aggregator::map)
                    .subscribe(writer::write)
        } else if (recorder is WahooRecorder) {
            disposable = recorder.data
                    .doOnError {error ->
                        Log.e(TAG, "data.wahoo error", error)
                    }
                    .doFinally {
                        disposable?.dispose()
                    }
                    .observeOn(scheduler)
                    .subscribeOn(scheduler)
                    .subscribe { data ->
                        Log.v(TAG, "heart rate data: $data")
                        aggregator.value.set(data.heartrate.asEventsPerSecond())
                    }
        }

        recorder.start()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")

        recorders.values.forEach(Recorder::stop)
    }

    private fun createRecorder(device: BluetoothDevice) {
        val recorder = buildRecorder(device)
        recorders[device] = recorder
        recorder.connect()
    }

    private fun buildRecorder(device: BluetoothDevice): Recorder {
        if (device.address.endsWith("37")) {
            return BITalinoRecorder(device, service, scheduler)
        }
        if (device.address.endsWith("58")) {
            return WahooRecorder(device, service)
        }
        throw IllegalArgumentException("unkown device type: $device")
    }

    private fun getDataOutputFile(): File {
        val externalFilesDir = service.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val time = System.currentTimeMillis()
        return File(externalFilesDir, "data-$time.csv")
    }

    private fun updateState() {
        val worstRecorderState = Recorder.State.lowest(recorders.values.map(Recorder::state))

        val sessionState = State.map(worstRecorderState)

        service.bus.events.onNext(RecorderBus.RecorderSessionStateChanged(sessionState))
    }

    companion object {

        const val TAG = "RecorderSession"

    }

}