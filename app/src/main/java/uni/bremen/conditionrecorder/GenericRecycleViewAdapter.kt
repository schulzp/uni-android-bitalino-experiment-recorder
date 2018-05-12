package uni.bremen.conditionrecorder

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.OnItemActivatedListener
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy


abstract class GenericRecycleViewAdapter<I, K, V: GenericRecycleViewAdapter.GenericViewHolder<K>>(val context: Context, internal val items: MutableList<I>) : RecyclerView.Adapter<V>() {

    var onItemClickListener:OnItemClickListener<I>? = null

    abstract val itemKeyStorageStrategy:StorageStrategy<K>

    abstract val itemKeyProvider:GenericItemKeyProvider<K>

    open fun add(item: I) {
        if (!items.contains(item)) {
            items.add(item)
        }
    }

    open fun clear() {
        items.clear()
    }

    fun createSelectionTracker(list:RecyclerView, selectionId:String):SelectionTracker.Builder<K> {
        return SelectionTracker.Builder<K>(
                selectionId,
                list,
                itemKeyProvider,
                createItemDetailsLookup(list),
                itemKeyStorageStrategy)

    }

    fun createActivationListener(delegate:OnItemClickListener<I>)
            : GenericOnItemActivatedListener = GenericOnItemActivatedListener(delegate)

    fun createItemDetailsLookup(view:RecyclerView) : GenericItemDetailsLookup = GenericItemDetailsLookup(view)

    abstract fun createViewHolder(view:View):V

    abstract fun onBindViewHolder(holder: V, item:I, position: Int)

    override fun getItemId(i: Int): Long = i.toLong()

    override fun getItemCount(): Int = items.size

    abstract fun getItemViewResourceId(viewType: Int): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): V {
        val view = createView(viewType, parent).also { view -> prepareView(view) }
        return createViewHolder(view)
    }

    override fun onBindViewHolder(holder: V, position: Int) {
        holder.itemView.setTag(R.id.item_value, items[position])
        onBindViewHolder(holder, items[position], position)
    }

    fun prepareView(view: View) {
        view.setOnTouchListener(RecycleViewHelper.RippleOnItemTouchListener())
        view.setOnClickListener { view ->
            this.onItemClickListener?.onItemSelected(view.getTag(R.id.item_value) as I) ?: false
        }
    }

    fun createView(viewType: Int, parent: ViewGroup): View {
        return LayoutInflater
                .from(context)
                .inflate(getItemViewResourceId(viewType), parent, false)
    }

    abstract class GenericViewHolder<K>(view:View, val itemDetails: GenericItemDetails<K>) : RecyclerView.ViewHolder(view)

    abstract class GenericItemKeyProvider<K>(scope: Int) : androidx.recyclerview.selection.ItemKeyProvider<K>(scope)

    class GenericItemDetails<K>(internal var selectionKey: K, internal var position: Int = RecyclerView.NO_POSITION) : androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails<K>() {

        override fun getSelectionKey(): K = selectionKey

        override fun getPosition(): Int = position

    }

    inner class GenericItemDetailsLookup(private val recyclerView: RecyclerView) : androidx.recyclerview.selection.ItemDetailsLookup<K>() {

        override fun getItemDetails(e: MotionEvent): GenericItemDetails<K>? {
            val view = recyclerView.findChildViewUnder(e.x, e.y)

            if (view != null) {
                val viewHolder = recyclerView.getChildViewHolder(view)
                if (viewHolder is GenericViewHolder<*>) {
                    return viewHolder.itemDetails as? GenericItemDetails<K>
                }
            }

            return null
        }

    }

    interface OnItemClickListener<I> {

        fun onItemSelected(item: I):Boolean

    }

    inner class GenericOnItemActivatedListener(private val delegate: OnItemClickListener<I>) : OnItemActivatedListener<K> {

        override fun onItemActivated(details: ItemDetailsLookup.ItemDetails<K>, e: MotionEvent): Boolean {
            return delegate.onItemSelected(items[details.position])
        }

    }

}