package com.bernaferrari.ui.dagger

import android.content.Context
import androidx.fragment.app.Fragment
import com.bernaferrari.ui.search.BaseElasticSearchFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

/**
 * SearchFragment with a Elastic behavior (user can scroll beyond top/bottom to dismiss it).
 */
abstract class DaggerBaseElasticSearchFragment : BaseElasticSearchFragment(),
    HasSupportFragmentInjector {

    open val shouldInject: Boolean = true

    @Inject
    lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun onAttach(context: Context) {
        if (shouldInject) {
            AndroidSupportInjection.inject(this)
        }
        super.onAttach(context)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return childFragmentInjector
    }

}
