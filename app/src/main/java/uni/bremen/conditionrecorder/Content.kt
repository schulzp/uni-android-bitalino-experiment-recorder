package uni.bremen.conditionrecorder

import android.net.Uri

enum class Content(val builder: () -> Uri.Builder) {
    RECORDINGS({uri().path("recordings")}),
    RECORDER({uri().path("recordings")}),
    DEVICES({uri().path("devices")}),
    DEVICE({uri().path("devices")});
}