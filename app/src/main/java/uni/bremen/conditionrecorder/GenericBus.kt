package uni.bremen.conditionrecorder

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


abstract class GenericBus<T: GenericBus.Instance, ET: T, CT: T> {

    protected val subject = PublishSubject.create<T>()

    abstract val commandSubject: Observable<CT>

    abstract val eventSubject: Observable<ET>

    fun post(instance: T) {
        subject.onNext(instance)
    }

    interface Instance

}