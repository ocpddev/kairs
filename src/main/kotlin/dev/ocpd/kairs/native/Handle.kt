package dev.ocpd.kairs.native

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * An internal thread-safe handle to a native resource.
 *
 * This privately holds an "immutable" pointer to a native resource, it can only be created or
 * destroyed, but not modified. This is to ensure the memory safety of the native resources.
 *
 * @constructor Create a new handle with the given pointer.
 * The handle will take ownership of the pointer, and the caller
 * should immediately discard the passed pointer and never use it again.
 */
internal class Handle internal constructor(@Volatile private var ptr: Long) {

    private val lock = ReentrantReadWriteLock()

    init {
        require(ptr != 0L) { "Handle must not be created with a null pointer" }
    }

    internal fun <T> borrow(fn: (Long) -> T): T {
        lock.readLock().withLock {
            val copy = ptr
            check(copy != 0L) { "Trying to operate on a null pointer" }
            return fn(copy)
        }
    }

    internal fun take(): Long {
        lock.writeLock().withLock {
            val copy = ptr
            check(copy != 0L) { "Trying to operate on a null pointer" }
            ptr = 0
            return copy
        }
    }
}
