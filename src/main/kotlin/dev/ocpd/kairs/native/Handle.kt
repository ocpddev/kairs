package dev.ocpd.kairs.native

import java.util.concurrent.atomic.AtomicLong

/**
 * An internal thread-safe handle to a native resource.
 *
 * This privately holds an "immutable" pointer to a native resource, it can only be created and
 * destroyed, but not modified. This is to ensure the memory safety of the native resources.
 */
@JvmInline
internal value class Handle private constructor(private val holder: AtomicLong) {

    internal constructor(handle: Long) : this(AtomicLong(handle)) {
        require(handle != 0L) { "Handle must not be created with a null pointer" }
    }

    internal fun get(): Long {
        val handle = holder.get()
        check(handle != 0L) { "Trying to retrieve a null pointer" }
        return handle
    }

    internal fun take(): Long {
        val handle = holder.getAndSet(0L)
        check(handle != 0L) { "Trying to free a null pointer" }
        return handle
    }
}
