package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject


class RecorderBus {

    private lateinit var data:PublishSubject<Any>

    val events = PublishSubject.create<Event>().also {
        it.doOnError { error -> Log.e("COMMAND BUS", "an error occurred on the bus", error) }
    }

    val commands = PublishSubject.create<Command>().also {
        it.doOnError { error -> Log.e("COMMAND BUS", "an error occurred on the bus", error) }
    }

    val bitalinoRecordingStarted = events.filter { it is RecorderBus.BitalinoRecordingStarted }.map { it as RecorderBus.BitalinoRecordingStarted }
    val bitalinoRecordingStopped = events.filter { it is RecorderBus.BitalinoRecordingStopped }.map { it as RecorderBus.BitalinoRecordingStopped }

    val videoRecordingStarted = events.filter { it is RecorderBus.VideoRecordingStarted }.map { it as RecorderBus.VideoRecordingStarted }
    val videoRecordingStopped = events.filter { it is RecorderBus.VideoRecordingStopped }.map { it as RecorderBus.VideoRecordingStopped }

    val recordingStarted = videoRecordingStarted.zipWith(bitalinoRecordingStarted, { v, s -> RecorderBus.RecordingStared(s, v) })
    val recordingStopped = videoRecordingStopped.zipWith(bitalinoRecordingStopped, { v, s -> RecorderBus.RecordingStopped(s, v) })

    val deviceSelected =  events
            .filter { event -> event is RecorderBus.SelectedDevice }
            .map { event -> event as RecorderBus.SelectedDevice }

    val deviceStateChange =  events
            .filter { event -> event is RecorderBus.RecorderStateChanged }
            .map { event -> event as RecorderBus.RecorderStateChanged }

    val recording = Observable.merge(recordingStarted, recordingStopped)

    val recorderSession = events.filter { it is RecorderSessionStateChanged }.map { it as RecorderSessionStateChanged }

    interface Event
    interface Command

    class RecordingStared(val bitalinoRecordingStarted: BitalinoRecordingStarted, val videoRecordingStarted: VideoRecordingStarted) : Event
    class RecordingStopped(val bitalinoRecordingStopped: BitalinoRecordingStopped, val videoRecordingStopped: VideoRecordingStopped) : Event

    class BitalinoRecordingStarted : Event
    class BitalinoRecordingStopped : Event

    class VideoRecordingStarted : Event
    class VideoRecordingStopped(val path:String) : Event

    class CreateSession : Command
    class DestroySession : Command

    class StartRecording : Command
    class StopRecording : Command

    class SelectedDevice(val device: BluetoothDevice) : Event

    class PhaseSelected(val phase:Int) : Event

    class RecorderStateChanged(val device: BluetoothDevice, val state: Recorder.State, val batteryLevel: Recorder.BatteryLevel = Recorder.BatteryLevel.UNKNOWN, val message: String) : Event

    class RecorderSessionStateChanged(val state:RecorderSession.State) : Event

}