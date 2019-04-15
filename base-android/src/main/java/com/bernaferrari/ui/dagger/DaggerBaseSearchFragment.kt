package com.bernaferrari.ui.dagger

import android.content.Context
import androidx.fragment.app.Fragment
import com.bernaferrari.ui.search.BaseSearchFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject


abstract class DaggerBaseSearchFragment : BaseSearchFragment(), HasSupportFragmentInjector {

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
