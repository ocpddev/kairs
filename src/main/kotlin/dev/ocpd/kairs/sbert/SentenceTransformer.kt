package dev.ocpd.kairs.sbert

import dev.ocpd.kairs.NATIVE
import dev.ocpd.kairs.native.Handle
import java.nio.file.Path

/**
 * A loaded SentenceTransformer model.
 *
 * This class *should be* thread-safe, while the safety is not yet fully verified.
 *
 * NOTE: Owner of this class is responsible for calling [close] when the object is no longer needed,
 * failing to do so may result in resource leaks.
 * It is recommended to use this class with the `use` function, or other equivalent techniques,
 * to ensure the resources will be properly released.
 */
class SentenceTransformer private constructor(private val handle: Handle) : AutoCloseable {

    /**
     * Load a pre-trained transformer model from a directory.
     * The directory should contain the following files:
     * - `config.json` (a JSON file containing the model configuration)
     * - `tokenizer.json` (a JSON file containing the tokenizer configuration)
     * - `pytorch_model.bin` (a binary file containing the model weights)
     */
    constructor(modelDir: Path) : this(SentenceTransformerNative.load(modelDir.toString()))

    /**
     * Calculate the cosine similarity between two sentences.
     *
     * @return a value between -1.0 and 1.0, where 1.0 means the sentences are identical.
     */
    fun cosineSimilarity(a: String, b: String): Float {
        val score = SentenceTransformerNative.cosineSimilarity(handle, a, b)
        if (score.isNaN()) throw IllegalStateException("Failed to calculate similarity. See log for details")
        return score
    }

    /**
     * Release the native resources.
     *
     * This method should only be called once. After calling this method,
     * the object should be considered unusable. Any further method calls
     * will result in undefined behavior.
     */
    override fun close() {
        SentenceTransformerNative.drop(handle)
    }
}

/**
 * Unsafe native bindings that should never be exposed.
 */
private object SentenceTransformerNative {

    fun load(path: String): Handle {
        // we only load the native library when we actually get called.
        // compares to initializing in the static block, this prevents
        // superfluous triggering if this class gets accessed from
        // reflection or other means.
        // this is okay because the library handle is thread-safe and
        // this is the only entry point of all other native calls.
        NATIVE.load()
        val handle = load0(path)
        check(handle != 0L) { "Failed to load model. See log for details" }
        return Handle(handle)
    }

    fun cosineSimilarity(handle: Handle, a: String, b: String): Float {
        return handle.borrow { ptr ->
            cosineSimilarity0(ptr, a, b)
        }
    }

    fun drop(handle: Handle) {
        drop0(handle.take())
    }

    private external fun load0(path: String): Long
    private external fun cosineSimilarity0(handle: Long, a: String, b: String): Float
    private external fun drop0(handle: Long)
}
