package uni.bremen.conditionrecorder


class RecorderBus : GenericBus<GenericBus.Instance, RecorderBus.Event, RecorderBus.Command>() {

    override val commandSubject = subject
            .filter { it is Command }
            .map { it as Command }

    override val eventSubject = subject
            .filter { it is Event }
            .map { it as Event }

    interface Aware {
        var recorderBus: RecorderBus
    }

    interface Event : GenericBus.Instance
    interface Command : GenericBus.Instance

    class VideoRecordingStarted() : Event
    class VideoRecordingStopped(val path:String) : Event

    class StartRecordingVideo() : Command
    class StopRecordingVideo() : Command

}