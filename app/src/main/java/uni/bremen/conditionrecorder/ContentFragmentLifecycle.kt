package uni.bremen.conditionrecorder

import android.support.v4.app.Fragment

interface ContentFragmentLifecycle {

    fun onContentCreated(fragment: Fragment)

    fun onContentResumed(fragment: Fragment)

}