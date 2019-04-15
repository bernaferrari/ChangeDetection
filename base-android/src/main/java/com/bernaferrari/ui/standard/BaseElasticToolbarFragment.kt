package com.bernaferrari.ui.standard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bernaferrari.ui.R
import com.bernaferrari.ui.widgets.ElasticDragDismissFrameLayout
import kotlinx.android.synthetic.main.frag_elastic_search.*

/**
 * BaseToolbarFragment with a Elastic behavior (user can scroll beyond top/bottom to dismiss it).
 */
abstract class BaseElasticToolbarFragment : BaseToolbarFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.frag_elastic_standard, container, false).apply {
        recyclerView = findViewById(R.id.recycler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chromeFader =
            ElasticDragDismissFrameLayout.SystemChromeFader(activity as AppCompatActivity)
        elastic_container.addListener(chromeFader)
    }
}
