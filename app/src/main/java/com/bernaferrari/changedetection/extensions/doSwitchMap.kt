package com.bernaferrari.changedetection.extensions

import io.reactivex.Observable

/**
 * Composes an [rx.Observable] from multiple creation functions chained by [rx.Observable.switchMap].
 *
 * @return composed Observable
 */
fun <A, B, R> doSwitchMap(
    zero: () -> Observable<A>,
    one: (A) -> Observable<B>,
    two: (A, B) -> Observable<R>
): Observable<R> =
    zero.invoke()
        .switchMap { a ->
            one.invoke(a)
                .switchMap { b ->
                    two.invoke(a, b)
                }
        }

/**
 * Composes an [rx.Observable] from multiple creation functions chained by [rx.Observable.switchMap].
 *
 * @return composed Observable
 */
fun <A, B, C, R> doSwitchMap(
    zero: () -> Observable<A>,
    one: (A) -> Observable<B>,
    two: (A, B) -> Observable<C>,
    three: (A, B, C) -> Observable<R>
): Observable<R> =
    zero.invoke()
        .switchMap { a ->
            one.invoke(a)
                .switchMap { b ->
                    two.invoke(a, b)
                        .switchMap { c ->
                            three.invoke(a, b, c)
                        }
                }
        }