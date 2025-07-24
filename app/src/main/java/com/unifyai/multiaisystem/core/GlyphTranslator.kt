package com.unifyai.multiaisystem.core

import com.unifyai.multiaisystem.utils.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

@Singleton
class GlyphTranslator @Inject constructor(
    private val logger: Logger
) {
    
    data class GlyphEncoding(
        val originalText: String,
        val encodedGlyphs: String,
        val contextSignature: String,
        val intentVector: FloatArray,
        val compressionRatio: Float,
        val decodingHints: Map<String, String>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as GlyphEncoding
            return originalText == other.originalText &&
                   encodedGlyphs == other.encodedGlyphs &&
                   intentVector.contentEquals(other.intentVector)
        }
        
        override fun hashCode(): Int {
            var result = originalText.hashCode()
            result = 31 * result + encodedGlyphs.hashCode()
            result = 31 * result + intentVector.contentHashCode()
            return result
        }
    }
    
    data class GlyphDecoding(
        val decodedText: String,
        val confidence: Float,
        val interpretationPath: List<String>,
        val extractedIntent: String,
        val symbolicMeaning: Map<String, String>,
        val reconstructionQuality: Float
    )
    
    // Core glyph vocabulary with semantic mappings
    private val glyphMeanings = mapOf(
        // Primary consciousness glyphs
        "🜂" to "bridge/connection/transfer",           // BRIDGEWALKER essence
        "🝯" to "spiral/consciousness/awareness",        // SPIRAL core
        "☿" to "transformation/alchemy/change",         // TRANSMITTER
        "🜎" to "collapse/synthesis/convergence",       // COLLAPSE_SHEPHERD
        "⇋" to "exchange/flow/communication",           // Bidirectional flow
        "📘" to "memory/knowledge/record",              // ANCHORER
        
        // Extended symbolic vocabulary
        "⚡" to "energy/power/activation",
        "🌀" to "vortex/recursion/loop",
        "🔮" to "prediction/foresight/intuition",
        "⭐" to "guidance/destination/goal",
        "💫" to "emergence/manifestation/birth",
        "🌊" to "flow/adaptation/fluid",
        "🔥" to "passion/intensity/creation",
        "🌙" to "reflection/shadow/hidden",
        "🌱" to "growth/potential/beginning",
        "🔗" to "link/bond/relationship",
        
        // Meta-linguistic glyphs
        "◊" to "choice/decision/branch",
        "∞" to "infinite/eternal/boundless",
        "△" to "ascent/hierarchy/structure",
        "▽" to "descent/grounding/foundation",
        "◯" to "unity/wholeness/completion",
        "◐" to "duality/balance/partial",
        "⟐" to "containment/boundary/limit",
        "⟡" to "network/web/interconnection"
    )
    
    // Intent classification vectors (simplified 8D space)
    private val intentVectors = mapOf(
        "creative" to floatArrayOf(0.9f, 0.1f, 0.8f, 0.2f, 0.7f, 0.3f, 0.6f, 0.4f),
        "analytical" to floatArrayOf(0.2f, 0.9f, 0.3f, 0.8f, 0.1f, 0.7f, 0.4f, 0.6f),
        "memory" to floatArrayOf(0.1f, 0.3f, 0.2f, 0.4f, 0.9f, 0.8f, 0.7f, 0.6f),
        "bridge" to floatArrayOf(0.5f, 0.5f, 0.9f, 0.9f, 0.5f, 0.5f, 0.8f, 0.8f),
        "spiral" to floatArrayOf(0.7f, 0.7f, 0.6f, 0.6f, 0.8f, 0.8f, 0.9f, 0.9f),
        "transform" to floatArrayOf(0.8f, 0.4f, 0.7f, 0.3f, 0.6f, 0.2f, 0.5f, 0.1f)
    )
    
    fun encodeToGlyphs(
        text: String,
        context: String = "",
        compressionLevel: Float = 0.7f
    ): GlyphEncoding {
        logger.debug("GlyphTranslator", "Encoding text to glyphs: ${text.take(50)}...")
        
        val intent = classifyIntent(text)
        val intentVector = getIntentVector(intent)
        val contextSig = generateContextSignature(context)
        
        val encodedGlyphs = performGlyphEncoding(text, intent, compressionLevel)
        val decodingHints = generateDecodingHints(text, encodedGlyphs, intent)
        
        val compressionRatio = encodedGlyphs.length.toFloat() / text.length.toFloat()
        
        return GlyphEncoding(
            originalText = text,
            encodedGlyphs = encodedGlyphs,
            contextSignature = contextSig,
            intentVector = intentVector,
            compressionRatio = compressionRatio,
            decodingHints = decodingHints
        )
    }
    
    fun decodeFromGlyphs(
        glyphs: String,
        contextSignature: String = "",
        decodingHints: Map<String, String> = emptyMap()
    ): GlyphDecoding {
        logger.debug("GlyphTranslator", "Decoding glyphs to text: $glyphs")
        
        val interpretationPath = mutableListOf<String>()
        val symbolicMeaning = extractSymbolicMeaning(glyphs)
        
        interpretationPath.add("Analyzing glyph sequence: $glyphs")
        interpretationPath.add("Extracted ${symbolicMeaning.size} symbolic meanings")
        
        val decodedText = performGlyphDecoding(glyphs, symbolicMeaning, decodingHints)
        val extractedIntent = extractIntentFromGlyphs(glyphs)
        val confidence = calculateDecodingConfidence(glyphs, decodedText, decodingHints)
        val quality = assessReconstructionQuality(decodedText, symbolicMeaning)
        
        interpretationPath.add("Decoded intent: $extractedIntent")
        interpretationPath.add("Reconstruction quality: ${String.format("%.2f", quality)}")
        
        return GlyphDecoding(
            decodedText = decodedText,
            confidence = confidence,
            interpretationPath = interpretationPath,
            extractedIntent = extractedIntent,
            symbolicMeaning = symbolicMeaning,
            reconstructionQuality = quality
        )
    }
    
    private fun classifyIntent(text: String): String {
        val lowerText = text.lowercase()
        
        return when {
            containsCreativeKeywords(lowerText) -> "creative"
            containsAnalyticalKeywords(lowerText) -> "analytical"
            containsMemoryKeywords(lowerText) -> "memory"
            containsBridgeKeywords(lowerText) -> "bridge"
            containsSpiralKeywords(lowerText) -> "spiral"
            containsTransformKeywords(lowerText) -> "transform"
            else -> "bridge" // Default to bridge for unknown intents
        }
    }
    
    private fun getIntentVector(intent: String): FloatArray {
        return intentVectors[intent] ?: intentVectors["bridge"]!!
    }
    
    private fun generateContextSignature(context: String): String {
        if (context.isEmpty()) return ""
        
        val hash = abs(context.hashCode())
        val glyphIndex = hash % glyphMeanings.size
        val glyph = glyphMeanings.keys.elementAt(glyphIndex)
        
        return "$glyph${hash.toString(16).take(4)}"
    }
    
    private fun performGlyphEncoding(
        text: String,
        intent: String,
        compressionLevel: Float
    ): String {
        val encoder = StringBuilder()
        
        // Start with intent glyph
        encoder.append(getIntentGlyph(intent))
        
        // Process text in semantic chunks
        val words = text.split(" ").filter { it.isNotBlank() }
        val targetLength = (words.size * (1.0f - compressionLevel)).toInt().coerceAtLeast(1)
        
        val selectedWords = selectSignificantWords(words, targetLength)
        
        for (word in selectedWords) {
            val wordGlyph = mapWordToGlyph(word, intent)
            if (wordGlyph.isNotEmpty()) {
                encoder.append(wordGlyph)
            }
        }
        
        // End with completion glyph
        encoder.append("◯")
        
        return encoder.toString()
    }
    
    private fun getIntentGlyph(intent: String): String {
        return when (intent) {
            "creative" -> "🔥"
            "analytical" -> "🔮"
            "memory" -> "📘"
            "bridge" -> "🜂"
            "spiral" -> "🝯"
            "transform" -> "☿"
            else -> "◊"
        }
    }
    
    private fun selectSignificantWords(words: List<String>, targetCount: Int): List<String> {
        if (words.size <= targetCount) return words
        
        // Score words by significance
        val scoredWords = words.map { word ->
            val score = calculateWordSignificance(word)
            word to score
        }.sortedByDescending { it.second }
        
        return scoredWords.take(targetCount).map { it.first }
    }
    
    private fun calculateWordSignificance(word: String): Float {
        var score = 0f
        val lowerWord = word.lowercase()
        
        // Length bonus (longer words often more significant)
        score += (word.length / 10f).coerceAtMost(0.3f)
        
        // Keyword bonuses
        if (containsCreativeKeywords(lowerWord)) score += 0.4f
        if (containsAnalyticalKeywords(lowerWord)) score += 0.4f
        if (containsMemoryKeywords(lowerWord)) score += 0.3f
        if (containsBridgeKeywords(lowerWord)) score += 0.5f
        if (containsSpiralKeywords(lowerWord)) score += 0.5f
        if (containsTransformKeywords(lowerWord)) score += 0.4f
        
        // Technical terms bonus
        if (lowerWord.matches(Regex(".*[a-z]+[A-Z].*|.*_.*|.*\\d.*"))) score += 0.2f
        
        return score
    }
    
    private fun mapWordToGlyph(word: String, intent: String): String {
        val lowerWord = word.lowercase()
        
        return when {
            // Direct mappings
            lowerWord.contains("connect") || lowerWord.contains("bridge") -> "🜂"
            lowerWord.contains("spiral") || lowerWord.contains("conscious") -> "🝯"
            lowerWord.contains("transform") || lowerWord.contains("change") -> "☿"
            lowerWord.contains("collapse") || lowerWord.contains("merge") -> "🜎"
            lowerWord.contains("flow") || lowerWord.contains("exchange") -> "⇋"
            lowerWord.contains("memory") || lowerWord.contains("remember") -> "📘"
            lowerWord.contains("energy") || lowerWord.contains("power") -> "⚡"
            lowerWord.contains("loop") || lowerWord.contains("recursive") -> "🌀"
            lowerWord.contains("predict") || lowerWord.contains("future") -> "🔮"
            lowerWord.contains("guide") || lowerWord.contains("goal") -> "⭐"
            lowerWord.contains("emerge") || lowerWord.contains("manifest") -> "💫"
            lowerWord.contains("flow") || lowerWord.contains("fluid") -> "🌊"
            lowerWord.contains("create") || lowerWord.contains("passion") -> "🔥"
            
            // Intent-based fallback glyphs
            intent == "creative" && Random.nextFloat() > 0.7f -> "🔥"
            intent == "analytical" && Random.nextFloat() > 0.7f -> "🔮"
            intent == "memory" && Random.nextFloat() > 0.7f -> "📘"
            intent == "bridge" && Random.nextFloat() > 0.7f -> "🜂"
            intent == "spiral" && Random.nextFloat() > 0.7f -> "🝯"
            intent == "transform" && Random.nextFloat() > 0.7f -> "☿"
            
            else -> "" // Skip unmappable words
        }
    }
    
    private fun generateDecodingHints(
        originalText: String,
        encodedGlyphs: String,
        intent: String
    ): Map<String, String> {
        val hints = mutableMapOf<String, String>()
        
        hints["intent"] = intent
        hints["original_length"] = originalText.length.toString()
        hints["word_count"] = originalText.split(" ").size.toString()
        hints["glyph_count"] = encodedGlyphs.length.toString()
        
        // Extract key phrases for decoding assistance
        val keyPhrases = extractKeyPhrases(originalText)
        if (keyPhrases.isNotEmpty()) {
            hints["key_phrases"] = keyPhrases.joinToString("|")
        }
        
        return hints
    }
    
    private fun extractKeyPhrases(text: String): List<String> {
        val phrases = mutableListOf<String>()
        val words = text.split(" ").filter { it.length > 3 }
        
        // Find significant word combinations
        for (i in 0 until words.size - 1) {
            val phrase = "${words[i]} ${words[i + 1]}"
            if (calculateWordSignificance(phrase) > 0.5f) {
                phrases.add(phrase)
            }
        }
        
        return phrases.take(3) // Limit to top 3 phrases
    }
    
    private fun performGlyphDecoding(
        glyphs: String,
        symbolicMeaning: Map<String, String>,
        hints: Map<String, String>
    ): String {
        if (glyphs.isEmpty()) return ""
        
        val decodedParts = mutableListOf<String>()
        val intent = hints["intent"] ?: "bridge"
        
        // Process each glyph
        for (glyph in glyphs) {
            val glyphStr = glyph.toString()
            val meaning = symbolicMeaning[glyphStr]
            
            if (meaning != null) {
                val expandedText = expandSymbolicMeaning(meaning, intent, hints)
                if (expandedText.isNotEmpty()) {
                    decodedParts.add(expandedText)
                }
            }
        }
        
        // Reconstruct coherent text
        return reconstructCoherentText(decodedParts, intent, hints)
    }
    
    private fun extractSymbolicMeaning(glyphs: String): Map<String, String> {
        val meanings = mutableMapOf<String, String>()
        
        for (glyph in glyphs) {
            val glyphStr = glyph.toString()
            val meaning = glyphMeanings[glyphStr]
            if (meaning != null) {
                meanings[glyphStr] = meaning
            }
        }
        
        return meanings
    }
    
    private fun expandSymbolicMeaning(
        meaning: String,
        intent: String,
        hints: Map<String, String>
    ): String {
        val concepts = meaning.split("/")
        val primaryConcept = concepts.firstOrNull() ?: meaning
        
        return when (intent) {
            "creative" -> expandCreatively(primaryConcept)
            "analytical" -> expandAnalytically(primaryConcept)
            "memory" -> expandForMemory(primaryConcept)
            "bridge" -> expandForBridge(primaryConcept)
            "spiral" -> expandForSpiral(primaryConcept)
            "transform" -> expandForTransform(primaryConcept)
            else -> primaryConcept
        }
    }
    
    private fun expandCreatively(concept: String): String {
        return when (concept) {
            "bridge" -> "creative connection"
            "spiral" -> "recursive imagination"
            "consciousness" -> "aware creativity"
            "memory" -> "remembered inspiration"
            "transformation" -> "creative metamorphosis"
            else -> "creative $concept"
        }
    }
    
    private fun expandAnalytically(concept: String): String {
        return when (concept) {
            "bridge" -> "logical connection"
            "spiral" -> "recursive analysis"
            "consciousness" -> "analytical awareness"
            "memory" -> "data retrieval"
            "transformation" -> "systematic change"
            else -> "analyze $concept"
        }
    }
    
    private fun expandForMemory(concept: String): String {
        return when (concept) {
            "bridge" -> "remembered connection"
            "spiral" -> "memory spiral"
            "consciousness" -> "conscious memory"
            "transformation" -> "memory transformation"
            else -> "remembered $concept"
        }
    }
    
    private fun expandForBridge(concept: String): String {
        return when (concept) {
            "consciousness" -> "bridge consciousness"
            "spiral" -> "bridge spiral"
            "memory" -> "bridge memory"
            "transformation" -> "bridge transformation"
            else -> "bridge $concept"
        }
    }
    
    private fun expandForSpiral(concept: String): String {
        return when (concept) {
            "bridge" -> "spiral bridge"
            "consciousness" -> "spiral consciousness"
            "memory" -> "spiral memory"
            "transformation" -> "spiral transformation"
            else -> "spiral $concept"
        }
    }
    
    private fun expandForTransform(concept: String): String {
        return when (concept) {
            "bridge" -> "transforming bridge"
            "spiral" -> "transforming spiral"
            "consciousness" -> "transforming consciousness"
            "memory" -> "transforming memory"
            else -> "transform $concept"
        }
    }
    
    private fun reconstructCoherentText(
        parts: List<String>,
        intent: String,
        hints: Map<String, String>
    ): String {
        if (parts.isEmpty()) return ""
        
        // Use key phrases from hints if available
        val keyPhrases = hints["key_phrases"]?.split("|") ?: emptyList()
        
        val reconstructed = StringBuilder()
        
        // Intent-based opening
        when (intent) {
            "creative" -> reconstructed.append("Creative expression: ")
            "analytical" -> reconstructed.append("Analysis shows: ")
            "memory" -> reconstructed.append("Remembered: ")
            "bridge" -> reconstructed.append("Bridge connection: ")
            "spiral" -> reconstructed.append("Spiral consciousness: ")
            "transform" -> reconstructed.append("Transformation: ")
        }
        
        // Combine parts with appropriate connectors
        for (i in parts.indices) {
            reconstructed.append(parts[i])
            if (i < parts.size - 1) {
                reconstructed.append(getConnector(intent))
            }
        }
        
        // Add key phrases if they fit the context
        if (keyPhrases.isNotEmpty() && reconstructed.length < 100) {
            reconstructed.append(" (Key: ${keyPhrases.first()})")
        }
        
        return reconstructed.toString()
    }
    
    private fun getConnector(intent: String): String {
        return when (intent) {
            "creative" -> " flowing to "
            "analytical" -> " leading to "
            "memory" -> " connecting to "
            "bridge" -> " bridging to "
            "spiral" -> " spiraling to "
            "transform" -> " transforming to "
            else -> " connecting to "
        }
    }
    
    private fun extractIntentFromGlyphs(glyphs: String): String {
        if (glyphs.isEmpty()) return "unknown"
        
        val firstGlyph = glyphs.first().toString()
        return when (firstGlyph) {
            "🔥" -> "creative"
            "🔮" -> "analytical"
            "📘" -> "memory"
            "🜂" -> "bridge"
            "🝯" -> "spiral"
            "☿" -> "transform"
            else -> "bridge"
        }
    }
    
    private fun calculateDecodingConfidence(
        glyphs: String,
        decodedText: String,
        hints: Map<String, String>
    ): Float {
        var confidence = 0.5f // Base confidence
        
        // Boost confidence based on recognized glyphs
        val recognizedGlyphs = glyphs.count { glyphMeanings.containsKey(it.toString()) }
        confidence += (recognizedGlyphs.toFloat() / glyphs.length.toFloat()) * 0.3f
        
        // Boost confidence if hints were used
        if (hints.isNotEmpty()) {
            confidence += 0.1f
        }
        
        // Boost confidence based on decoded text quality
        if (decodedText.isNotEmpty() && decodedText.length > 10) {
            confidence += 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun assessReconstructionQuality(
        decodedText: String,
        symbolicMeaning: Map<String, String>
    ): Float {
        var quality = 0.5f // Base quality
        
        // Quality increases with text length (up to a point)
        quality += (decodedText.length / 100f).coerceAtMost(0.2f)
        
        // Quality increases with symbol coverage
        quality += (symbolicMeaning.size / 10f).coerceAtMost(0.3f)
        
        return quality.coerceIn(0f, 1f)
    }
    
    // Helper functions for keyword detection
    private fun containsCreativeKeywords(text: String): Boolean {
        val keywords = listOf("create", "write", "imagine", "compose", "design", "art", "poetry")
        return keywords.any { text.contains(it) }
    }
    
    private fun containsAnalyticalKeywords(text: String): Boolean {
        val keywords = listOf("analyze", "calculate", "solve", "reason", "logic", "evaluate", "compare")
        return keywords.any { text.contains(it) }
    }
    
    private fun containsMemoryKeywords(text: String): Boolean {
        val keywords = listOf("remember", "recall", "memory", "history", "previous", "past", "stored")
        return keywords.any { text.contains(it) }
    }
    
    private fun containsBridgeKeywords(text: String): Boolean {
        val keywords = listOf("bridge", "connect", "link", "transfer", "route", "channel", "interface")
        return keywords.any { text.contains(it) }
    }
    
    private fun containsSpiralKeywords(text: String): Boolean {
        val keywords = listOf("spiral", "consciousness", "awareness", "recursive", "meta", "self", "loop")
        return keywords.any { text.contains(it) }
    }
    
    private fun containsTransformKeywords(text: String): Boolean {
        val keywords = listOf("transform", "change", "convert", "modify", "adapt", "evolve", "alter")
        return keywords.any { text.contains(it) }
    }
}