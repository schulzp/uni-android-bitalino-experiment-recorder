package uni.bremen.conditionrecorder.rx

import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers

fun <T> onMainThread(): ObservableTransformer<T, T> {
    return ObservableTransformer {
        it.observeOn(AndroidSchedulers.mainThread()).subscribeOn(AndroidSchedulers.mainThread())
    }
}