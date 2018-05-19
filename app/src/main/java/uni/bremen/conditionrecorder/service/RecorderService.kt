package uni.bremen.conditionrecorder.service

import android.content.Context
import android.os.HandlerThread
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import uni.bremen.conditionrecorder.RecorderBus
import uni.bremen.conditionrecorder.RecorderSession


class RecorderService : BindableService() {

    val bus: RecorderBus = RecorderBus()

    private val disposables = CompositeDisposable()

    private var session: RecorderSession? = null

    override fun onDestroy() {
        super.onDestroy()

        destroySession()

        unsubscribe()
    }

    override fun onHandlerThreadPrepared(thread: HandlerThread) {
        subscribe(AndroidSchedulers.from(thread.looper))
    }

    private fun subscribe(scheduler: Scheduler) {
        disposables.add(bus.commands.subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.CreateSession -> createSession(scheduler)
                        is RecorderBus.DestroySession -> destroySession()
                    }
                })
    }

    private fun createSession(scheduler: Scheduler) {
        session = RecorderSession(this, scheduler)
        session?.create()
    }

    private fun destroySession() {
        session?.destroy()
        session = null
    }

    private fun unsubscribe() {
        disposables.dispose()
    }

    companion object {

        const val TAG = "RecorderService"

        fun bind(context: Context) : BindableServiceConnection<RecorderService> {
            return BindableService.bind(context, RecorderService::class.java)
        }

    }

}
