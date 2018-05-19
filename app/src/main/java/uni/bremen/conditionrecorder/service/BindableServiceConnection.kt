package uni.bremen.conditionrecorder.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.reactivex.Maybe
import io.reactivex.subjects.MaybeSubject

class BindableServiceConnection<S : BindableService>(private val context:Context) : ServiceConnection {

    val service = MaybeSubject.create<S>()

    override fun onServiceConnected(source: ComponentName, binder: IBinder) {
        Log.d("SC", "connected $binder from $source")
        if (binder is BindableService.Binder) {
            service.onSuccess(binder.getService())
        }
    }

    override fun onServiceDisconnected(source: ComponentName) {
        Log.d("SC", "disconnected $source")
    }

    fun close() = context.unbindService(this)

}