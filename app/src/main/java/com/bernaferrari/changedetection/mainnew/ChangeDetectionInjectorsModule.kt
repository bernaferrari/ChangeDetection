package com.bernaferrari.changedetection.mainnew

import com.bernaferrari.changedetection.addedit.AddEditFragment
import com.bernaferrari.changedetection.logs.LogsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ChangeDetectionInjectorsModule {

    @ContributesAndroidInjector
    abstract fun mainFragment(): MainFragment

    @ContributesAndroidInjector
    abstract fun logsFragment(): LogsFragment

    @ContributesAndroidInjector
    abstract fun addNewFragment(): AddEditFragment

}