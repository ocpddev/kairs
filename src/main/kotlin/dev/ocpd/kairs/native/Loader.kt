package dev.ocpd.kairs.native

import dev.ocpd.kairs.cache.CACHE
import dev.ocpd.slf4k.slf4j
import dev.ocpd.slf4k.warn
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.outputStream

/**
 * A helper to automatically install and load the native library.
 *
 * This helper will extract the native library from the JAR file,
 * copy it to a temporary directory, and then load it into the JVM.
 */
internal class NativeLibrary(private val name: String) {

    @Volatile
    private var loaded = false
    private val filename = System.mapLibraryName(name)

    /**
     * Load the native library.
     *
     * This method is idempotent and thread-safe.
     */
    internal fun load() {
        // skip if already loaded
        if (loaded) {
            return
        }
        synchronized(this) {
            // check again in case it was loaded by another thread
            if (loaded) {
                return
            }
            loadLibrary()
            loaded = true
        }
    }

    private fun loadLibrary() {
        val libPath = libPath()
        try {
            // currently extract every time, maybe we should cache it?
            extractResource(libPath)
        } catch (e: Exception) {
            // we should not panic if the extraction fails.
            // in containerized environments, the library might be cached
            // within the image at build time and set to read-only.
            log.warn(e) { "Failed to extract native library: ${e.message}" }
        }
        System.load(libPath.toAbsolutePath().toString())
    }

    private fun extractResource(dest: Path) {
        val url = resourceUrl()
        url.openStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun resourceUrl(): URL {
        val platform = Platform.current()
        val path = "/native/${platform.identifier()}/$filename"
        return this.javaClass.getResource(path)
            ?: throw UnsupportedOperationException("Native library $filename is not available for $platform")
    }

    private fun libPath(): Path {
        return CACHE.nativeDir() / filename
    }

    companion object {
        private val log by slf4j
    }
}
