package com.bernaferrari.changedetection

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.afollestad.rxkprefs.Pref
import com.afollestad.rxkprefs.RxkPrefs
import com.afollestad.rxkprefs.rxkPrefs
import com.bernaferrari.changedetection.detailsText.TextBottomDataSource
import com.bernaferrari.changedetection.mainnew.AppModule
import com.bernaferrari.changedetection.mainnew.ChangeDetectionInjectorsModule
import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.*
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModuleAR {

    @Provides
    fun provideContext(application: App): Context = application.applicationContext

    @Provides
    fun sharedPrefs(application: App): SharedPreferences {
        return application.getSharedPreferences("workerPreferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun rxPrefs(application: App): RxkPrefs {
        return rxkPrefs(sharedPrefs(application))
    }
}

@Module
class SitesRepositoryModule {

    @Provides
    @Singleton
    internal fun provideSitesLocalDataSource(
        dao: SitesDao,
        executors: AppExecutors
    ): SitesDataSource = SitesLocalDataSource(executors, dao)

    @Singleton
    @Provides
    internal fun provideSitesDao(db: ChangeDatabase): SitesDao = db.siteDao()
}

@Module
class RxPrefsModule {

    @Provides
    @Named("lightMode")
    fun isLightTheme(rxPrefs: RxkPrefs): Pref<Boolean> {
        return rxPrefs.boolean("lightMode", true)
    }

    @Provides
    @Named("colorBySdk")
    fun isColorBySdk(rxPrefs: RxkPrefs): Pref<Boolean> {
        return rxPrefs.boolean("colorBySdk", true)
    }

    @Provides
    @Named("showSystemApps")
    fun showSystemApps(rxPrefs: RxkPrefs): Pref<Boolean> {
        return rxPrefs.boolean("showSystemApps", false)
    }

    @Provides
    @Named("backgroundSync")
    fun backgroundSync(rxPrefs: RxkPrefs): Pref<Boolean> {
        return rxPrefs.boolean("backgroundSync", false)
    }

    @Provides
    @Named("syncInterval")
    fun syncInterval(rxPrefs: RxkPrefs): Pref<String> {
        return rxPrefs.string("syncInterval", "301")
    }

    @Provides
    @Named("orderBySdk")
    fun orderBySdk(rxPrefs: RxkPrefs): Pref<Boolean> {
        return rxPrefs.boolean("orderBySdk", false)
    }
}


@Module
class SnapsRepositoryModule {

    @Provides
    @Singleton
    internal fun provideSnapsLocalDataSource(
        dao: SnapsDao,
        executors: AppExecutors,
        context: Context
    ): SnapsDataSource = SnapsLocalDataSource(executors, dao, context)

    @Singleton
    @Provides
    internal fun provideSnapsDao(db: ChangeDatabase): SnapsDao = db.snapsDao()

    @Provides
    @Singleton
    internal fun provideTextBottomDataSource(dao: SnapsDao) = TextBottomDataSource(dao)
}

@Module
class RepositoriesMutualDependenciesModule {

    @Singleton
    @Provides
    internal fun provideDb(context: Context): ChangeDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChangeDatabase::class.java,
            "Changes.db"
        )
            .build()
    }

    @Singleton
    @Provides
    internal fun provideAppExecutors(): AppExecutors = AppExecutors()
}

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ChangeDetectionInjectorsModule::class,
        AppModule::class,
        AppModuleAR::class,
        RxPrefsModule::class,
        SitesRepositoryModule::class,
        SnapsRepositoryModule::class,
        RepositoriesMutualDependenciesModule::class]
)
@Singleton
interface SingletonComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: App): Builder

        fun build(): SingletonComponent
    }

    fun inject(app: App)

    fun appContext(): Context
    fun sharedPrefs(): SharedPreferences
    fun sitesRepository(): SitesRepository
    fun snapsRepository(): SnapsRepository

    @Named("lightMode")
    fun isLightTheme(): Pref<Boolean>

    @Named("colorBySdk")
    fun isColorBySdk(): Pref<Boolean>

    @Named("showSystemApps")
    fun showSystemApps(): Pref<Boolean>

    @Named("backgroundSync")
    fun backgroundSync(): Pref<Boolean>

    @Named("syncInterval")
    fun syncInterval(): Pref<String>

    @Named("orderBySdk")
    fun orderBySdk(): Pref<Boolean>
}

class Injector private constructor() {
    companion object {
        fun get(): SingletonComponent = App.get().component
    }
}
