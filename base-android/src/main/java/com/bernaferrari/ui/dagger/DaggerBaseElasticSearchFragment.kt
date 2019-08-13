package com.bernaferrari.ui.dagger

import android.content.Context
import com.bernaferrari.ui.search.BaseElasticSearchFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * SearchFragment with a Elastic behavior (user can scroll beyond top/bottom to dismiss it).
 */
abstract class DaggerBaseElasticSearchFragment : BaseElasticSearchFragment(), HasAndroidInjector {

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
