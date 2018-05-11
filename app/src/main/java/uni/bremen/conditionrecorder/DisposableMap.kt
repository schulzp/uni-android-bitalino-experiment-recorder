package uni.bremen.conditionrecorder

import io.reactivex.disposables.Disposable

class DisposableMap : HashMap<String, Disposable>() {

    fun dispose() {
        values.forEach { it.dispose() }
        values.clear()
    }

}