package com.bernaferrari.ui.extras

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bernaferrari.ui.R
import com.bernaferrari.ui.base.TiviMvRxFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

abstract class BaseRecyclerFragment : TiviMvRxFragment(), CoroutineScope {

    override fun layoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.recyclerview, container, false).apply {
        recyclerView = findViewById(R.id.recycler)
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }
}
