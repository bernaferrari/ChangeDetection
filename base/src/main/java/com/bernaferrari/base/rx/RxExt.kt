package com.bernaferrari.base.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * <code>
 *   val disposables = CompositeDisposable()
 *   disposables += someObject.observable().subscribe { }
 *   disposables.clear()
 * </code>
 */
operator fun CompositeDisposable?.plusAssign(disposable: Disposable) {
    this?.add(disposable)
}
