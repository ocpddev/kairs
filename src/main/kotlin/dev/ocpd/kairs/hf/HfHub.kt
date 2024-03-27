package dev.ocpd.kairs.hf

import dev.ocpd.kairs.cache.CACHE
import dev.ocpd.slf4k.info
import dev.ocpd.slf4k.slf4j
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * Very crude implementation of the Hugging Face Hub API.
 * Not recommended for production use.
 */
object HfHub {

    private const val BASE_URL = "https://huggingface.co"

    private val log by slf4j

    fun repo(modelId: String, ref: String = "main", fn: Repo.() -> Unit): Path {
        val destDir = CACHE.modelDir(modelId)
        val repo = Repo(modelId, ref, destDir)
        repo.fn()
        return destDir
    }

    class Repo(private val repoId: String, private val ref: String, private val path: Path) {

        private val client = HttpClient.newBuilder()
            // respect system proxy settings.
            // this is redundant as it is the default behavior, just to be explicit
            .proxy(ProxySelector.getDefault())
            // follow redirects, as LFS objects are often redirected
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        /**
         * Download a file from the repository. Skip if it is already cached.
         */
        fun download(file: String) {
            val destFile = path / file
            if (destFile.exists()) return
            doDownload(file, destFile)
        }

        private fun doDownload(file: String, destFile: Path) {
            val url = "$BASE_URL/$repoId/resolve/$ref/$file"
            val request = HttpRequest.newBuilder().uri(URI(url)).build()
            log.info { "Downloading $url to $destFile" }
            client.send(request, HttpResponse.BodyHandlers.ofFile(destFile))
            log.info { "Downloaded $url to $destFile" }
        }
    }
}
