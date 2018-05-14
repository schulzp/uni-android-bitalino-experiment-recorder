package uni.bremen.conditionrecorder.bitalino

interface Recorder {

    enum class State {

        DISCONNECTED, CONNECTING, CONNECTED, RECORDING_STARTED, RECORDING, RECORDING_STOPPED;

        companion object {

            fun lowest(states:Iterable<State>): State {
                return states.map(State::ordinal).sorted().first().let { State.values()[it] }
            }

        }

    }

    fun getState():State

}