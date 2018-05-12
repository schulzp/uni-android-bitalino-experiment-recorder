package uni.bremen.conditionrecorder

import android.content.Context
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent
import android.view.View

class RecycleViewHelper {

    class RippleOnItemTouchListener : View.OnTouchListener {

        override fun onTouch(child: View, e: MotionEvent): Boolean {
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
                child.background?.setHotspot(e.x, e.y)
            }

            return false
        }

    }

    companion object {

        const val RIPPLE_DURATION = 1000L

        fun verticalList(list: RecyclerView, context: Context): RecyclerView {
            list.layoutManager = LinearLayoutManager(context)
            list.itemAnimator = DefaultItemAnimator()
            list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            list.setHasFixedSize(true)

            return list
        }

    }

}