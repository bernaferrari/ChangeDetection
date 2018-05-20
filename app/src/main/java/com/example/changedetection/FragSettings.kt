package com.example.changedetection

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Site
import com.example.changedetection.forms.*
import com.example.changedetection.groupie.BottomSheetCardItem
import com.example.changedetection.groupie.DialogItem
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.android.synthetic.main.todos_encontros_activity.view.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FragSettings : Fragment() {
    private lateinit var mViewModel: TasksViewModel
    private val groupAdapter = GroupAdapter<ViewHolder>()
    private var theList = mutableListOf<BottomSheetCardItem>()
    private var updatingCultos = Section(theList)

    val color: Int by lazy { ContextCompat.getColor(requireActivity(), R.color.FontStrong) }

    fun dp(value: Int, resources: Resources): Int {
        return (resources.displayMetrics.density * value).toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.todos_encontros_activity, container, false)
        mViewModel = MainActivity.obtainViewModel(requireActivity())

        view.defaultRecycler.apply {

            setEmptyView(view.stateLayout.apply {
                this.empty_text.text = "Nothing found"
                showEmptyState()
            })

            itemAnimator = null
            adapter = groupAdapter.apply {
                spanCount = calculateNoOfColumns(requireActivity())
                if (this.itemCount == 0) {
                    this.add(MarqueeItem("Change Detection"))
//                    this.add(updatingCultos)
                }
            }

            layoutManager = GridLayoutManager(context, groupAdapter.spanCount).apply {
                spanSizeLookup = groupAdapter.spanSizeLookup
            }
        }

        return view
    }

    companion object {
        // Useful for landscape mode, tablets and extra extra large displays
        fun calculateNoOfColumns(context: Context): Int {

            val point = Point()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(
                point
            )

            val metrics = context.resources.displayMetrics
            val ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
            val displayMetrics = context.resources.displayMetrics
            val dpWidth = displayMetrics.widthPixels / displayMetrics.density

            Logger.d("WIDHT $dpWidth")
            Logger.d("ratio $ratio")
            if (ratio < 1) {
                return (dpWidth / 240).toInt()
            }
            return (dpWidth / 240).toInt()
        }
    }
}

