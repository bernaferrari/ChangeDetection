package com.bernaferrari.ui.standard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bernaferrari.ui.R
import com.bernaferrari.ui.widgets.ElasticDragDismissFrameLayout
import kotlinx.android.synthetic.main.frag_elastic_search.*

/**
 * BaseToolbarFragment with a Elastic behavior (user can scroll beyond top/bottom to dismiss it).
 */
abstract class BaseElasticToolbarFragment : BaseToolbarFragment() {

    override val inflateRes: Int = R.layout.frag_elastic_standard

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chromeFader =
            ElasticDragDismissFrameLayout.SystemChromeFader(activity as AppCompatActivity)
        elastic_container.addListener(chromeFader)
    }
}
