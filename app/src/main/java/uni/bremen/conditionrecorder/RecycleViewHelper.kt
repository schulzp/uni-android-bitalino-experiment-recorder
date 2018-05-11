package uni.bremen.conditionrecorder

import android.content.Context
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent

class RecycleViewHelper {

    class RippleOnItemTouchListener : RecyclerView.OnItemTouchListener {

        override fun onInterceptTouchEvent(list: RecyclerView, e: MotionEvent): Boolean = true

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) { }

        override fun onTouchEvent(list: RecyclerView, e: MotionEvent) {
            val child = list.findChildViewUnder(e.x, e.y) ?: return

            val down = e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE
            val up = e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL

            val running = (child.getTag(R.id.ripple_started) ?: 0L) as Long
            val started = System.currentTimeMillis()
            val delta = started - running

            if (delta < RIPPLE_DURATION && up && !down) {
                Log.d("RIPPLE", "end")
                child.isPressed = false
                child.setTag(R.id.ripple_started, 0L)
            } else if (delta > RIPPLE_DURATION && down && !up) {
                Log.d("RIPPLE", "start")
                child.isPressed = true
                child.setTag(R.id.ripple_started, started)
                child.background?.setHotspot(e.x - child.x, e.y - child.y)
            }

        }

    }

    companion object {

        const val RIPPLE_DURATION = 1000L

        fun verticalList(list: RecyclerView, context: Context): RecyclerView {
            list.addOnItemTouchListener(RippleOnItemTouchListener())
            list.layoutManager = LinearLayoutManager(context)
            list.itemAnimator = DefaultItemAnimator()
            list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            list.setHasFixedSize(true)

            return list
        }

    }

}