package com.bernaferrari.ui.dagger

import android.content.Context
import com.bernaferrari.ui.search.BaseSearchFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


abstract class DaggerBaseSearchFragment : BaseSearchFragment(), HasAndroidInjector {

    open val shouldInject: Boolean = true

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onAttach(context: Context) {
        if (shouldInject) {
            AndroidSupportInjection.inject(this)
        }
        super.onAttach(context)
    }

    override fun androidInjector(): AndroidInjector<Any>? {
        return androidInjector
    }
}
