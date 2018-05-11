package uni.bremen.conditionrecorder

import android.content.Context
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

class RecycleViewHelper(private val context:Context) {

    companion object {

        fun verticalList(list: RecyclerView, context: Context): RecyclerView {
            list.layoutManager = LinearLayoutManager(context)
            list.itemAnimator = DefaultItemAnimator()
            list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            list.setHasFixedSize(true)

            return list
        }

    }

}