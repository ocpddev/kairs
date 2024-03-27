package dev.ocpd.kairs.sbert

import dev.ocpd.kairs.hf.HfHub
import org.junit.jupiter.api.Test

class SentenceTransformerTest {

    private fun withTransformer(fn: SentenceTransformer.() -> Unit) {
        val modelPath = HfHub.repo("sentence-transformers/all-MiniLM-L6-v2") {
            download("config.json")
            download("pytorch_model.bin")
            download("tokenizer.json")
        }
        SentenceTransformer(modelPath).use(fn)
    }

    @Test
    fun test() {
        withTransformer {
            testCase(
                "I am a sentence for which I would like to get its embedding.",
                "I am a sentence for which I would like to get its embedding.",
                1.0f
            )
            testCase(
                "This is a completely different text that is not similar to the other documents.",
                "I am a sentence for which I would like to get its embedding.",
                0.22324173f
            )
        }
    }

    private fun SentenceTransformer.testCase(a: String, b: String, score: Float) {
        val actual = cosineSimilarity(a, b)
        assert(actual in (score - 1e-6)..(score + 1e-6)) {
            """
                Expected similarity score between:
                > $a
                > $b
                is near $score, but got $actual
            """.trimIndent()
        }
    }
}
