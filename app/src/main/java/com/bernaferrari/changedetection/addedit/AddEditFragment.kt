package com.bernaferrari.changedetection.addedit

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.bernaferrari.base.misc.onTextChanged
import com.bernaferrari.changedetection.Injector
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.core.simpleController
import com.bernaferrari.changedetection.extensions.fixUrlIfNecessary
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.extensions.itemAnimatorWithoutChangeAnimations
import com.bernaferrari.changedetection.extensions.shakeIt
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.util.GradientColors
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.addnewfragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddEditFragment : AddEditBaseFragment() {

    private lateinit var model: SharedViewModel
    @Inject
    lateinit var sitesDao: SitesDao

    private val colorsList = GradientColors.gradients

    private var selectedColors = colorsList.shuffled().first()

    private var currentSite: Site? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentSite = arguments?.getParcelable("site")

        model = activity?.run {
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (currentSite == null) {
            configureAdd()
        } else {
            configureEdit()
        }

        configureColorPickerRecycler()

        // Checkbox methods
        constraintCheck.setOnClickListener { checkbox.toggle() }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            beginDelayedTransition()
            browserScrollView.isVisible = isChecked
        }

        // Url box methods

        model.selected.observe(this, Observer { item ->
            if (item != null) {
                url.text = item.toEditText()
            }
        })

        urlInputLayout.editText?.onTextChanged {
            if (urlInputLayout.error != null) {
                urlInputLayout.error = null
            }
        }

        urlInputLayout.editText?.text = "www.inf.ufpr.br".toEditText()

        // Click listeners
        saveButton.setOnClickListener {
            onSavePressed(currentSite, selectedColors)
        }

        openBrowser.setOnClickListener {
            if (!isUrlWrong(urlInputLayout)) {
                val bundle = bundleOf("url" to url.text.toString())
                it.findNavController().navigate(R.id.action_addNew_to_webviewFragment, bundle)
            }
        }

        testBoth.setOnClickListener {
            if (!isUrlWrong(urlInputLayout)) {
                CompareDialog.show(requireActivity(), url.text.toString())
            }
        }
    }

    private fun configureAdd() {
        toolbar.title = getString(R.string.addtitle)

        // try to get url from clipboard
        url.text = urlFromClipboardOrEmpty().toEditText()
    }

    private fun configureEdit() {
        toolbar.title = getString(R.string.edittitle)

        currentSite?.also {
            title.text = it.title?.toEditText()
            url.text = it.url.toEditText()
            tags.text = it.notes.toEditText()
//            checkbox.isChecked = it.isChecked // TODO
            selectedColors = it.colors
        }
    }

    private fun configureColorPickerRecycler() {
        colorSelector.setController(
            simpleController {

                // Create each color picker item, checking for the first (because it needs extra margin)
                // and checking for the one which is selected (so it becomes selected)
                colorsList.forEachIndexed { index, it ->

                    ColorPickerItemEpoxy_()
                        .id("picker $index")
                        .allowDeselection(false)
                        .switchIsOn(selectedColors == it)
                        .gradientColor(it)
                        .onClick { v ->
                            selectedColors = v
                            colorSelector.requestModelBuild()
                        }
                        .addTo(this)
                }
            }
        )

        colorSelector.apply {
            this.itemAnimator = itemAnimatorWithoutChangeAnimations()
            this.overScrollMode = View.OVER_SCROLL_NEVER
            this.requestModelBuild()
        }
    }

    private fun onSavePressed(item: Site?, selectedColors: ColorGroup) {

        if (isUrlWrong(urlInputLayout)) return

        if (item != null) {

            val currentUrl = url.text.toString()
            val previousUrl = item.url

            val updatedSite = item.copy(
                title = title.text.toString(),
                url = currentUrl,
                colors = selectedColors
            )

            scope.launch(Dispatchers.IO) {
                sitesDao.updateSite(updatedSite)

                // Only reload if the url has changed.
                if (currentUrl != previousUrl) {
                    WorkerHelper.reloadSite(updatedSite)
                }
            }
        } else {
            val site = Site(
                title = title.text.toString(),
                url = url.text.toString(),
                timestamp = System.currentTimeMillis(),
                colors = selectedColors
            )

            scope.launch(Dispatchers.IO) {
                sitesDao.insertSite(site)
                WorkerHelper.reloadSite(site)
            }
        }

        model.select(null)
        activity?.onBackPressed()

        val sharedPrefs = Injector.get().sharedPrefs()
        // when list size is 1 or 2, warn the user that background sync is off
        /*  if (!isInEditingMode && sitesList.size < 3 && !sharedPrefs.getBoolean(
                  "backgroundSync",
                  false
              )
          ) {
              MaterialDialog(requireContext())
                  .title(R.string.turn_on_background_sync_title)
                  .message(R.string.turn_on_background_sync_content)
                  .negativeButton(R.string.no)
                  .onSavePressed(R.string.yes) {
                      sharedPrefs.edit { putBoolean("backgroundSync", true) }
                      WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
                  }
                  .show()
          }*/
    }

    private fun isUrlWrong(urlInputLayout: TextInputLayout): Boolean {

        val fixedUrl = urlInputLayout.editText?.text.toString().fixUrlIfNecessary()
        url.text = fixedUrl.toEditText()

        if (!fixedUrl.isValidUrl()) {
            urlInputLayout.shakeIt()
            urlInputLayout.error = "Invalid url"
            Toast.makeText(
                this.context,
                com.bernaferrari.changedetection.R.string.incorrect_url,
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
        return false
    }
}
