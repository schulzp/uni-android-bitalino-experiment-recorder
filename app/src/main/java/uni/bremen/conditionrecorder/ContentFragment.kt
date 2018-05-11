package uni.bremen.conditionrecorder

import android.support.v4.app.Fragment

abstract class ContentFragment(val content:Content, val title:Int) : Fragment() {

    fun withObserver():ContentFragmentLifecycle? {
        return if (activity is ContentFragmentLifecycle) activity as? ContentFragmentLifecycle else null
    }

}