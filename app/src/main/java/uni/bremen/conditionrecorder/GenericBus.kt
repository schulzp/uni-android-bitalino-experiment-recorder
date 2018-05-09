package uni.bremen.conditionrecorder

import android.util.Log
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


abstract class GenericBus<T: GenericBus.Instance, ET: T, CT: T> {

    protected val subject = PublishSubject.create<T>().also {
        it.doOnError { error -> Log.e("BUS", "an error occurred on the bus", error) }
    }

    abstract val commandSubject: Observable<CT>

    abstract val eventSubject: Observable<ET>

    fun post(vararg instance: T) {
        instance.forEach { subject.onNext(it) }
    }

    interface Instance

}