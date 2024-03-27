package dev.ocpd.kairs.cache

import dev.ocpd.kairs.LIB_NAME
import dev.ocpd.slf4k.info
import dev.ocpd.slf4k.slf4j
import dev.ocpd.slf4k.warn
import java.nio.file.Path
import kotlin.io.path.*

internal val CACHE by lazy { Cache.auto() }

/**
 * A cache for storing downloaded models and extracted native libraries.
 */
internal class Cache(private val path: Path) {

    internal fun nativeDir() = (path / "native").createDirectories()

    internal fun modelDir(modelId: String) = (path / "models" / modelId.replace("/", "--")).createDirectories()

    internal companion object {

        private val log by slf4j

        /**
         * Automatically determine the cache directory.
         *
         * This will first check the environment variable, then the home directory, and finally the temporary directory.
         */
        internal fun auto(): Cache {
            /**
             * Use the path specified in the environment variable as cache.
             */
            fun env(): Path? =
                // well, automatically creating the directories might not be a good idea
                System.getenv("${LIB_NAME.uppercase()}_HOME")?.let(::Path)?.createDirectories()

            /**
             * Use the home directory as cache.
             */
            fun home(): Path {
                val homeDir = System.getProperty("user.home")?.let(::Path)
                    // we won't create the home directory if it doesn't exist
                    ?.takeIf(Path::exists) ?: error("Home directory does not exist")
                return (homeDir / ".cache" / LIB_NAME).createDirectories()
            }

            /**
             * Use temporary directory as cache.
             * This is almost always available, we give up if this fails
             */
            fun tmp(): Path = createTempDirectory(LIB_NAME)

            val path = env() ?: try {
                home()
            } catch (e: Exception) {
                log.warn(e) { "Failed to create home cache, falling back to temporary cache" }
                tmp()
            }

            log.info { "Using $path as cache" }

            return Cache(path)
        }
    }
}
