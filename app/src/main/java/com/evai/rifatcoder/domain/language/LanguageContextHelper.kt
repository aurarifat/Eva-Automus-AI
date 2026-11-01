package com.evai.rifatcoder.domain.language

/**
 * Natural language classifications supported by EVA AI (Phase 14).
 */
enum class DetectedLanguage(val displayName: String, val code: String) {
    BENGALI("Bengali", "bn"),
    BANGLISH("Banglish", "bn-Latn"),
    ENGLISH("English", "en"),
}

/**
 * Helper for detecting and managing multilingual context across Bengali, English, and Banglish.
 * Ensures natural responses while preserving code, URLs, package names, and technical terms.
 */
object LanguageContextHelper {

    // Common phonetic Banglish marker words
    private val BANGLISH_MARKERS = setOf(
        "ami", "tumi", "apni", "amader", "tomader", "apnader",
        "kemon", "acho", "achhen", "bhalo", "kharap",
        "ki", "kobe", "kothay", "keno", "kivabe", "kibhabe",
        "korbo", "korcho", "korchen", "korte", "kore", "paro", "paren",
        "hobe", "hoy", "hoyeche", "hoye",
        "shob", "thik", "ache", "bujhte", "parsi", "parchi", "parlam",
        "dhonnobad", "bolun", "bolo", "bolte", "shuno", "shunun",
        "kichu", "onek", "ektu", "shobai", "kokhon"
    )

    /**
     * Detects whether input text is Bengali (native script), Banglish (phonetic Latin), or English.
     */
    fun detectLanguage(text: String): DetectedLanguage {
        val clean = text.trim()
        if (clean.isEmpty()) return DetectedLanguage.ENGLISH

        // Check for Bengali Unicode block (U+0980 to U+09FF)
        var bengaliCharCount = 0
        var latinCharCount = 0

        for (char in clean) {
            val code = char.code
            if (code in 0x0980..0x09FF) {
                bengaliCharCount++
            } else if ((code in 'a'.code..'z'.code) || (code in 'A'.code..'Z'.code)) {
                latinCharCount++
            }
        }

        // If native Bengali characters are present, prioritize Bengali
        if (bengaliCharCount > 0 && bengaliCharCount >= latinCharCount * 0.2f) {
            return DetectedLanguage.BENGALI
        }

        // Check for Banglish keywords in Latin-script tokens
        if (latinCharCount > 0) {
            val tokens = clean.lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.isNotEmpty() }

            var banglishHits = 0
            for (token in tokens) {
                if (token in BANGLISH_MARKERS) {
                    banglishHits++
                }
            }

            if (banglishHits >= 2 || (tokens.size <= 4 && banglishHits >= 1)) {
                return DetectedLanguage.BANGLISH
            }
        }

        return DetectedLanguage.ENGLISH
    }

    /**
     * Builds contextual system guidance for the AI provider based on language and user preferences.
     * Prevents awkward translation of code identifiers, filenames, and package names.
     */
    fun buildLanguageGuidance(
        detectedLanguage: DetectedLanguage,
        userPreferredLanguage: String? = null,
    ): String {
        val target = if (!userPreferredLanguage.isNullOrBlank()) {
            when (userPreferredLanguage.lowercase()) {
                "bengali", "bangla", "বাংলা" -> DetectedLanguage.BENGALI
                "banglish" -> DetectedLanguage.BANGLISH
                else -> detectedLanguage
            }
        } else {
            detectedLanguage
        }

        return when (target) {
            DetectedLanguage.BENGALI -> {
                "[LANGUAGE CONTEXT: User prompt is in Bengali (বাংলা). Respond naturally in fluent Bengali. " +
                        "CRITICAL: Do NOT translate programming code, URLs, technical identifiers, package names (e.g. com.evai.rifatcoder), or configuration filenames (e.g. build.gradle.kts, AndroidManifest.xml) into Bengali script. Keep technical tokens in standard Latin/English.]"
            }
            DetectedLanguage.BANGLISH -> {
                "[LANGUAGE CONTEXT: User prompt is in Banglish (Bengali written in Latin letters). " +
                        "Respond conversationally and naturally in Bengali or Banglish as contextually appropriate. Keep code, package names, and technical terms in standard English.]"
            }
            DetectedLanguage.ENGLISH -> {
                "[LANGUAGE CONTEXT: User prompt is in English. Respond in clear, helpful English.]"
            }
        }
    }
}
