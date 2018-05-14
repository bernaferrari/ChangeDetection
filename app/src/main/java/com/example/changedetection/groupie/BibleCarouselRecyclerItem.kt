package com.example.changedetection.groupie

//import com.biblialibras.android.main.SelectCoordenadoresFrag
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.changedetection.BottomSheetDialogExpanded
import com.example.changedetection.R
import com.example.changedetection.data.Diff
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.bible_recycler_item.*
import kotlinx.android.synthetic.main.todos_encontros_activity.*
import com.example.changedetection.diffs.text.DiffRow
import com.example.changedetection.diffs.text.DiffRowGenerator
import java.util.*

/**
 * A horizontally scrolling RecyclerView, for use in a vertically scrolling RecyclerView.
 */
class BibleCarouselRecyclerItem(
    private val bookTitle: String,
    private val subtitle: String,
    private val shouldShowMore: Boolean,
    private val diffList: List<Diff>,
    private val callback: (Diff) -> Unit
) :
    Item() {

    override fun getLayout(): Int {
        return R.layout.bible_recycler_item
    }

    override fun unbind(holder: ViewHolder) {
        holder.morecontainer.visibility = View.VISIBLE
        super.unbind(holder)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = bookTitle
        viewHolder.subtitle.text = subtitle

        if (!shouldShowMore) {
            viewHolder.morecontainer.visibility = View.GONE
        }

        viewHolder.icon.setImageDrawable(
            IconicsDrawable(
                viewHolder.icon.context,
                GoogleMaterial.Icon.gmd_keyboard_arrow_right
            ).color(ContextCompat.getColor(viewHolder.icon.context, R.color.atlasianblue))
        )

        val carouselAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

        viewHolder.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter
        }

        val updating = mutableListOf<Item>()

        Logger.d("DiffList: ${diffList}")

        diffList.forEach {
            Logger.d("Diff: ${it.timestamp}")
            updating += DiffItem(it)
        }

        carouselAdapter.add(Section(updating))

        carouselAdapter.setOnItemClickListener { item, view ->
            if (item is DiffItem){
                callback(item.diff)
            }
        }
    }
}
