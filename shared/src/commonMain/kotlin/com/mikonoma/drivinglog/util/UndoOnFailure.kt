package com.mikonoma.drivinglog.util

/**
 * Runs [block] and returns its result. If it throws, [undo] runs first and then the same throwable is rethrown, unchanged.
 * For work that must be rolled back whatever stopped it: an error, or the coroutine being cancelled. The
 * `CancellationException` is rethrown like anything else, so cancellation still propagates.
 */
@Suppress("TooGenericExceptionCaught") // Every throwable is rethrown as is after [undo]; nothing is swallowed or narrowed.
inline fun <T> undoOnFailure( undo: () -> Unit, block: () -> T ): T {
    try {
        return block()
    } catch (throwable: Throwable) {
        undo()
        throw throwable
    }
}
