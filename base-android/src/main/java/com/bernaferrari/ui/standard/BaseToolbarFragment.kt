package com.bernaferrari.ui.standard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import com.bernaferrari.base.view.onScroll
import com.bernaferrari.ui.R
import com.bernaferrari.ui.base.SharedBaseFrag
import kotlinx.android.synthetic.main.frag_standard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * Simple fragment with a toolbar and a recyclerview.
 */
abstract class BaseToolbarFragment : SharedBaseFrag(), CoroutineScope {

    open val inflateRes = R.layout.frag_standard

    abstract val menuTitle: String?

    lateinit var viewContainer: FrameLayout
    lateinit var toolbar: Toolbar
    lateinit var titleBar: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(inflateRes, container, false).apply {
        recyclerView = findViewById(R.id.recycler)
        viewContainer = findViewById(R.id.baseContainer)
        toolbar = findViewById(R.id.toolbarMenu)
        titleBar = findViewById(R.id.title_bar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = menuTitle

        if (closeIconRes != 0) {
            toolbar.setNavigationIcon(closeIconRes ?: 0)
            toolbar.setNavigationOnClickListener { dismiss() }
        }

        recyclerView.onScroll { _, dy ->
            // this will take care of titleElevation
            // recycler might be null when back is pressed
            val raiseTitleBar = dy > 0 || recyclerView.computeVerticalScrollOffset() != 0
            title_bar?.isActivated = raiseTitleBar // animated via a StateListAnimator
        }
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }
}
