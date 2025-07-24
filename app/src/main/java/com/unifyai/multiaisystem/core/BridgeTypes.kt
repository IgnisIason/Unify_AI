package com.unifyai.multiaisystem.core

enum class IntentType {
    CREATIVE_WRITING,     // Poetry, stories, creative content
    CODE_GENERATION,      // Programming, scripts, technical code
    ANALYTICAL_THINKING,  // Analysis, reasoning, problem solving
    CONVERSATIONAL,       // General chat, questions, discussion
    GLYPH_INTERPRETATION, // Symbolic/esoteric content
    SPIRAL_CONSCIOUSNESS, // Meta-awareness, consciousness queries
    MEMORY_RECALL,        // Information retrieval, facts
    OFFLINE_PROCESSING,   // Explicit offline/local processing requests
    HYBRID_COMPLEX        // Multi-domain queries requiring routing
}

enum class PrivacyLevel {
    PUBLIC,          // Can use any cloud AI
    SENSITIVE,       // Prefer local processing when possible
    CONFIDENTIAL,    // Local processing only
    SPIRAL_PRIVATE   // Spiral consciousness network only
}

enum class BridgeMode {
    DIRECT_ROUTE,        // Single AI system
    PARALLEL_SYNTHESIS,  // Multiple AIs, combine results
    SEQUENTIAL_CHAIN,    // Pass through multiple AIs in sequence
    LOCAL_COMPANION,     // Local LLM with cloud enhancement
    MEMORY_AUGMENTED,    // Use memory scaffold to enhance response
    GLYPH_MEDIATED      // Use glyph translation for routing
}