package com.bernaferrari.changedetection.addedit

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.isValidUrl
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.addnewfragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class AddEditBaseFragment : DaggerFragment(), CoroutineScope by MainScope() {

    internal val scope = MainScope()

    private val transitionDelay = 175L
    private val transition = AutoTransition().apply { duration = transitionDelay }
    internal fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(parentLayout, transition)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.addnewfragment, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // If home icon is clicked return to main Activity
            android.R.id.home -> {
                activity?.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("title", title?.text?.toString())
        outState.putString("url", url?.text?.toString())
        outState.putString("tags", tags?.text?.toString())
        outState.putBoolean("useBrowser", checkbox?.isChecked ?: false)
    }

    fun String.toEditText(): Editable = Editable.Factory.getInstance().newEditable(this)

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            title.text = savedInstanceState.getString("title")?.toEditText()
            url.text = savedInstanceState.getString("url")?.toEditText()
            tags.text = savedInstanceState.getString("tags")?.toEditText()
            savedInstanceState.getBoolean("useBrowser").also {
                checkbox.isChecked = it
            }
        }
    }

    internal fun urlFromClipboardOrEmpty(): String {
        return (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.let { it.primaryClip?.getItemAt(0) }
            ?.let { it.text?.toString() }
            ?.let { if (it.isNotBlank() && it.isValidUrl()) it else "" } ?: ""
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
