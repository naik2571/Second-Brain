package com.ai.secondbrain

import java.util.Locale
import kotlin.math.sqrt

object VectorEmbeddingEngine {
    const val DIMENSIONS = 128

    // Semantic concept clusters for projection
    private val CONCEPT_CLUSTERS = mapOf(
        "tech" to listOf("code", "programming", "software", "developer", "algorithm", "database", "api", "system", "react", "kotlin", "python", "bug", "frontend", "backend", "git"),
        "ai" to listOf("ai", "machine learning", "neural", "model", "llm", "intelligence", "gpt", "whisper", "vector", "embedding", "agent", "deep learning", "inference"),
        "finance" to listOf("money", "expense", "budget", "salary", "investment", "stocks", "bank", "crypto", "profit", "revenue", "cash", "saving", "cost", "wealth"),
        "health" to listOf("nutrition", "diet", "gym", "workout", "exercise", "sleep", "food", "calorie", "protein", "muscle", "wellness", "running", "fasting", "avocado"),
        "productivity" to listOf("deep work", "focus", "time", "routine", "habit", "task", "discipline", "schedule", "goal", "journal", "efficiency", "calendar", "action"),
        "business" to listOf("startup", "customer", "product", "market", "sales", "growth", "founder", "interview", "validation", "user", "saas", "pricing", "pitch"),
        "psychology" to listOf("mindset", "stoicism", "mental", "emotion", "meditation", "calm", "anxiety", "thinking", "clarity", "psychology", "philosophy", "wisdom")
    )

    fun embed(text: String): FloatArray {
        val vector = FloatArray(DIMENSIONS)
        val normalized = text.lowercase(Locale.ROOT)
        val words = normalized.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }

        if (words.isEmpty()) return vector

        // 1. Direct character n-gram hashing into vector dimensions
        for (word in words) {
            val hash = word.hashCode()
            val dim1 = Math.abs(hash) % DIMENSIONS
            val dim2 = Math.abs(hash / 31) % DIMENSIONS
            vector[dim1] += 1.0f
            vector[dim2] += 0.5f

            // 2. Semantic Cluster Boost
            CONCEPT_CLUSTERS.entries.forEachIndexed { clusterIdx, entry ->
                for (kw in entry.value) {
                    if (word == kw || word.contains(kw) || kw.contains(word)) {
                        val baseDim = (clusterIdx * 16) % DIMENSIONS
                        for (i in 0 until 12) {
                            vector[(baseDim + i) % DIMENSIONS] += 2.0f
                        }
                    }
                }
            }
        }

        // 3. Unit L2 Normalization (makes cosine similarity = dot product)
        var sumSquares = 0.0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares.toDouble()).toFloat()

        if (norm > 0.0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }

    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0.0f
        var dot = 0.0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(0.0f, 1.0f)
    }

    fun serialize(v: FloatArray): String = v.joinToString(",") { String.format(Locale.US, "%.4f", it) }

    fun deserialize(s: String): FloatArray {
        val parts = s.split(",")
        val arr = FloatArray(parts.size)
        for (i in parts.indices) {
            arr[i] = parts[i].toFloatOrNull() ?: 0.0f
        }
        return arr
    }
}
