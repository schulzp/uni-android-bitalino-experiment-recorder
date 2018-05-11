package uni.bremen.conditionrecorder

import android.net.Uri

enum class Content(val builder: () -> Uri.Builder, val id:Int = -1) {
    RECORDINGS({uri().path("recordings")}, R.id.contentRecordings),
    RECORDER({uri().path("recordings")}),
    DEVICES({uri().path("devices")}, R.id.contentDevices),
    DEVICE({uri().path("devices")});
}