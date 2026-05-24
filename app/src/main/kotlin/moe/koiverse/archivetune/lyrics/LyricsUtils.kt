/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.lyrics

import android.icu.text.Transliterator
import com.atilika.kuromoji.ipadic.Tokenizer
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.core.parser.TTMLParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Character.UnicodeScript

data class LyricsRomanizationPreferences(
    val romanizeJapanese: Boolean,
    val romanizeKorean: Boolean,
    val romanizeChinese: Boolean,
    val romanizeHindi: Boolean,
    val romanizeOther: Boolean,
) {
    val isEnabled: Boolean
        get() = romanizeJapanese || romanizeKorean || romanizeChinese || romanizeHindi || romanizeOther
}

data class ParsedLyricsDocument(
    val lyrics: SyncedLyrics,
    val isSynced: Boolean,
)

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private val autoParser = AutoParser()
    private val ttmlParser = TTMLParser()
    private const val GENERIC_ROMANIZATION_TRANSFORM = "Any-Latin; Latin-ASCII"
    private val OTHER_ROMANIZATION_EXCLUDED_SCRIPTS = setOf(
        UnicodeScript.LATIN,
        UnicodeScript.COMMON,
        UnicodeScript.INHERITED,
        UnicodeScript.HAN,
        UnicodeScript.HIRAGANA,
        UnicodeScript.KATAKANA,
        UnicodeScript.HANGUL,
        UnicodeScript.DEVANAGARI,
    )
    private val genericRomanizationTransliterator = ThreadLocal.withInitial {
        Transliterator.getInstance(GENERIC_ROMANIZATION_TRANSFORM)
    }

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Digraphs (Yōon - combinations like kya, sho)
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo", // ヂ variants, also commonly 'ja', 'ju', 'jo'
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",

        // Basic Katakana Characters
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", // ヲ is pronounced 'o'
        "ン" to "n",

        // Dakuten (voiced consonants)
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do", // ヂ and ヅ are often 'ji' and 'zu'

        // Handakuten (p-sounds for 'h' group) / Dakuten for 'h' group
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo", // Dakuten for ハ행 (ha-row)
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po", // Handakuten for ハ행 (ha-row)

        // Chōonpu (long vowel mark) - removed as per original logic
        "ー" to ""
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "ᄀ" to "g",  "ᄁ" to "kk", "ᄂ" to "n",  "ᄃ" to "d", 
            "ᄄ" to "tt", "ᄅ" to "r",  "ᄆ" to "m",  "ᄇ" to "b",
            "ᄈ" to "pp", "ᄉ" to "s",  "ᄊ" to "ss", "ᄋ" to "",
            "ᄌ" to "j",  "ᄍ" to "jj", "ᄎ" to "ch", "ᄏ" to "k",
            "ᄐ" to "t",  "ᄑ" to "p",  "ᄒ" to "h"
        ),
        "jung" to mapOf(
            "ᅡ" to "a",  "ᅢ" to "ae", "ᅣ" to "ya",  "ᅤ" to "yae", 
            "ᅥ" to "eo", "ᅦ" to "e",  "ᅧ" to "yeo", "ᅨ" to "ye", 
            "ᅩ" to "o",  "ᅪ" to "wa", "ᅫ" to "wae", "ᅬ" to "oe",
            "ᅭ" to "yo", "ᅮ" to "u",  "ᅯ" to "wo",  "ᅰ" to "we",
            "ᅱ" to "wi", "ᅲ" to "yu", "ᅳ" to "eu",  "ᅴ" to "eui",
            "ᅵ" to "i"
        ),
        "jong" to mapOf(
            "ᆨ" to "k",     "ᆨᄋ" to "g",   "ᆨᄂ" to "ngn", "ᆨᄅ" to "ngn", "ᆨᄆ" to "ngm", "ᆨᄒ" to "kh",
            "ᆩ" to "kk",    "ᆩᄋ" to "kg",  "ᆩᄂ" to "ngn", "ᆩᄅ" to "ngn", "ᆩᄆ" to "ngm", "ᆩᄒ" to "kh",
            "ᆪ" to "k",     "ᆪᄋ" to "ks",  "ᆪᄂ" to "ngn", "ᆪᄅ" to "ngn", "ᆪᄆ" to "ngm", "ᆪᄒ" to "kch",
            "ᆫ" to "n",     "ᆫᄅ" to "ll",  "ᆬ" to "n",     "ᆬᄋ" to "nj",  "ᆬᄂ" to "nn",  "ᆬᄅ" to "nn",
            "ᆬᄆ" to "nm",  "ᆬㅎ" to "nch", "ᆭ" to "n",     "ᆭᄋ" to "nh",  "ᆭᄅ" to "nn",  "ᆮ" to "t",
            "ᆮᄋ" to "d",   "ᆮᄂ" to "nn",  "ᆮᄅ" to "nn",  "ᆮᄆ" to "nm",  "ᆮᄒ" to "th",  "ᆯ" to "l",
            "ᆯᄋ" to "r",   "ᆯᄂ" to "ll",  "ᆯᄅ" to "ll",  "ᆰ" to "k",     "ᆰᄋ" to "lg",  "ᆰᄂ" to "ngn",
            "ᆰᄅ" to "ngn", "ᆰᄆ" to "ngm", "ᆰᄒ" to "lkh", "ᆱ" to "m",     "ᆱᄋ" to "lm",  "ᆱᄂ" to "mn",
            "ᆱᄅ" to "mn",  "ᆱᄆ" to "mm",  "ᆱᄒ" to "lmh", "ᆲ" to "p",     "ᆲᄋ" to "lb",  "ᆲᄂ" to "mn",
            "ᆲᄅ" to "mn",  "ᆲᄆ" to "mm",  "ᆲᄒ" to "lph", "ᆳ" to "t",     "ᆳᄋ" to "ls",  "ᆳᄂ" to "nn",
            "ᆳᄅ" to "nn",  "ᆳᄆ" to "nm",  "ᆳᄒ" to "lsh", "ᆴ" to "t",     "ᆴᄋ" to "lt",  "ᆴᄂ" to "nn",
            "ᆴᄅ" to "nn",  "ᆴᄆ" to "nm",  "ᆴᄒ" to "lth", "ᆵ" to "p",     "ᆵᄋ" to "lp",  "ᆵᄂ" to "mn",
            "ᆵᄅ" to "mn",  "ᆵᄆ" to "mm",  "ᆵᄒ" to "lph", "ᆶ" to "l",     "ᆶᄋ" to "lh",  "ᆶᄂ" to "ll",
            "ᆶᄅ" to "ll",  "ᆶᄆ" to "lm",  "ᆶᄒ" to "lh",  "ᆷ" to "m",     "ᆷᄅ" to "mn",  "ᆸ" to "p",
            "ᆸᄋ" to "b",   "ᆸᄂ" to "mn",  "ᆸᄅ" to "mn",  "ᆸᄆ" to "mm",  "ᆸᄒ" to "ph",  "ᆹ" to "p",
            "ᆹᄋ" to "ps",  "ᆹᄂ" to "mn",  "ᆹᄅ" to "mn",  "ᆹᄆ" to "mm",  "ᆹᄒ" to "psh", "ᆺ" to "t",
            "ᆺᄋ" to "s",   "ᆺᄂ" to "nn",  "ᆺᄅ" to "nn",  "ᆺᄆ" to "nm",  "ᆺᄒ" to "sh",  "ᆻ" to "t",
            "ᆻᄋ" to "ss",  "ᆻᄂ" to "tn",  "ᆻᄅ" to "tn",  "ᆻᄆ" to "nm",  "ᆻᄒ" to "th",  "ᆼ" to "ng",
            "ᆽ" to "t",     "ᆽᄋ" to "j",   "ᆽᄂ" to "nn",  "ᆽᄅ" to "nn",  "ᆽᄆ" to "nm",  "ᆽᄒ" to "ch",
            "ᆾ" to "t",     "ᆾᄋ" to "ch",  "ᆾᄂ" to "nn",  "ᆾᄅ" to "nn",  "ᆾᄆ" to "nm",  "ᆾᄒ" to "ch",
            "ᆿ" to "k",     "ᆿᄋ" to "k",   "ᆿᄂ" to "ngn", "ᆿᄅ" to "ngn", "ᆿᄆ" to "ngm", "ᆿᄒ" to "kh",
            "ᇀ" to "t",     "ᇀᄋ" to "t",   "ᇀᄂ" to "nn",  "ᇀᄅ" to "nn",  "ᇀᄆ" to "nm",  "ᇀᄒ" to "th",
            "ᇁ" to "p",     "ᇁᄋ" to "p",   "ᇁᄂ" to "mn",  "ᇁᄅ" to "mn",  "ᇁᄆ" to "mm",  "ᇁᄒ" to "ph",
            "ᇂ" to "t",     "ᇂᄋ" to "h",   "ᇂᄂ" to "nn",  "ᇂᄅ" to "nn",  "ᇂᄆ" to "mm",  "ᇂᄒ" to "t",
            "ᇂᄀ" to "k",
        )
    )

    // Lazy initialized Tokenizer
    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    fun isTtml(lyrics: String): Boolean {
        val trimmed = lyrics.trim()
        if (!trimmed.startsWith("<")) return false

        return trimmed.contains("<tt", ignoreCase = true) ||
                trimmed.contains("http://www.w3.org/ns/ttml", ignoreCase = true)
    }

    fun isSyncedLyrics(lyrics: String): Boolean {
        val trimmed = lyrics.trim()
        if (trimmed.isBlank()) return false
        return isTtml(trimmed) || autoParser.canParse(trimmed)
    }

    fun parseSyncedLyricsDocument(rawLyrics: String): ParsedLyricsDocument {
        val normalized = rawLyrics.replace("\uFEFF", "").trim()
        if (normalized.isBlank()) {
            return ParsedLyricsDocument(SyncedLyrics(emptyList()), false)
        }

        if (isTtml(normalized)) {
            val ttmlParsed = runCatching { ttmlParser.parse(normalized) }.getOrNull()
            if (ttmlParsed != null && ttmlParsed.lines.isNotEmpty()) {
                return ParsedLyricsDocument(ttmlParsed, true)
            }

            val betterTTMLParsed = runCatching { moe.koiverse.archivetune.betterlyrics.TTMLParser.parseTTML(normalized) }.getOrNull()
            if (!betterTTMLParsed.isNullOrEmpty()) {
                val betterLines = betterTTMLParsed.map { line ->
                    SyncedLine(
                        content = line.text,
                        translation = null,
                        start = (line.startTime * 1000).toInt(),
                        end = (line.endTime * 1000).toInt(),
                    )
                }
                return ParsedLyricsDocument(SyncedLyrics(betterLines), true)
            }
        }

        val parsed = runCatching {
            autoParser.parse(normalized)
        }.getOrElse {
            SyncedLyrics(emptyList())
        }

        if (parsed.lines.isNotEmpty()) {
            return ParsedLyricsDocument(parsed, true)
        }

        val plainLines = normalized
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { line ->
                SyncedLine(
                    content = line,
                    translation = null,
                    start = 0,
                    end = Int.MAX_VALUE,
                )
            }
            .toList()

        return ParsedLyricsDocument(SyncedLyrics(plainLines), false)
    }

    fun displayText(lyrics: String): String {
        val raw = lyrics.trim()
        if (raw.isBlank()) return raw

        val displayText = parseSyncedLyricsDocument(raw)
            .lyrics
            .lines
            .joinToString("\n") { lineText(it) }
            .trim()

        return displayText.ifBlank { raw }
    }

    fun lineText(line: ISyncedLine): String =
        when (line) {
            is SyncedLine -> line.content
            is KaraokeLine -> line.syllables.joinToString(separator = "") { it.content }.trim()
            else -> ""
        }

    fun lineTextWithTranslation(line: ISyncedLine): String {
        val mainText = lineText(line)
        val translation = when (line) {
            is SyncedLine -> line.translation
            is KaraokeLine -> line.translation
            else -> null
        }

        return listOfNotNull(
            mainText.takeIf(String::isNotBlank),
            translation?.takeIf(String::isNotBlank),
        ).joinToString("\n")
    }

    suspend fun applyRomanization(
        lyrics: SyncedLyrics,
        preferences: LyricsRomanizationPreferences,
    ): SyncedLyrics {
        if (!preferences.isEnabled || lyrics.lines.isEmpty()) return lyrics

        var changed = false
        val romanizedLines = lyrics.lines.map { line ->
            val text = lineText(line)
            val romanized = romanizeLyricsLine(text, preferences)
            if (romanized.isNullOrBlank()) {
                line
            } else {
                when (line) {
                    is SyncedLine -> if (line.translation == null) {
                        changed = true
                        line.copy(translation = romanized)
                    } else {
                        line
                    }
                    is KaraokeLine.MainKaraokeLine -> if (line.phonetic == null) {
                        changed = true
                        line.copy(phonetic = romanized)
                    } else {
                        line
                    }
                    is KaraokeLine.AccompanimentKaraokeLine -> if (line.phonetic == null) {
                        changed = true
                        line.copy(phonetic = romanized)
                    } else {
                        line
                    }
                    else -> line
                }
            }
        }

        return if (changed) lyrics.copy(lines = romanizedLines) else lyrics
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     */
    fun katakanaToRomaji(katakana: String?): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "キャ" before "キ")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    /**
     * Romanizes Japanese text using Kuromoji Tokenizer and the optimized katakanaToRomaji function.
     * Runs on Dispatchers.Default for CPU-intensive work.
     * Expected impact: Faster tokenization due to reused Tokenizer instance and faster
     * per-token romanization.
     */
    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        // Use the lazily initialized tokenizer
        val tokens = kuromojiTokenizer.tokenize(text)

        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }

            // Pass the next token's reading for sokuon handling if applicable
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     * @param katakana The Katakana string to convert.
     * @param nextKatakana Optional: The next Katakana string (from the next token) to help with sokuon (ッ) gemination.
     */
    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "キャ" before "キ")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            // Handle sokuon (ッ) - gemination
            if (!consumed && katakana[i] == 'ッ') {
                val nextCharToDouble = nextKatakana?.getOrNull(0)
                if (nextCharToDouble != null) {
                    val nextCharRomaji = KANA_ROMAJI_MAP[nextCharToDouble.toString()]?.getOrNull(0)?.toString()
                        ?: nextCharToDouble.toString()
                    romajiBuilder.append(nextCharRomaji.lowercase().trim())
                }
                // Sokuon itself doesn't have a direct romaji representation other than geminating the next consonant.
                // We just consume 'ッ' and let the next character (if any within the current token) be processed normally.
                i += 1 // Consume the 'ッ'
                consumed = true
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]

            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)

                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    /**
     * Checks if the given text contains any Japanese characters (Hiragana, Katakana, or common Kanji).
     * This function is generally efficient due to '.any' and early exit.
     * No major performance bottlenecks expected here for typical inputs.
     */
    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') || // Hiragana
            (char in '\u30A0'..'\u30FF') || // Katakana
            // CJK Unified Ideographs (covers most common Kanji)
            // Note: This range also includes many Chinese Hanzi.
            // Differentiating Japanese Kanji from Chinese Hanzi solely based on Unicode
            // ranges is challenging as they share many characters.
            // For more accurate Japanese detection, one might need to analyze
            // the presence of Hiragana/Katakana alongside Kanji.
            (char in '\u4E00'..'\u9FFF')
        }
    }

    /**
     * Checks if the given text contains any Korean characters (Hangul Syllables, Jamo, etc.).
     */
    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3') // Hangul Syllables
        }
    }
        
    /**
     * Checks if the given text contains any Chinese characters (common Hanzi).
     * This function is generally efficient due to '.any' and early exit.
     * To improve accuracy in distinguishing between Chinese and Japanese (which shares Kanji),
     * this function now checks if the text *predominantly* consists of CJK Unified Ideographs
     * and *lacks* significant amounts of Hiragana or Katakana.
     *
     * A simple threshold is used here. More sophisticated methods (e.g., frequency analysis,
     * dictionaries, or machine learning models) would be needed for higher accuracy.
     */
    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false

        val hanCharCount = text.count { hasScript(it, UnicodeScript.HAN) }
        if (hanCharCount == 0) return false

        val japaneseKanaCount = text.count { hasScript(it, UnicodeScript.HIRAGANA) || hasScript(it, UnicodeScript.KATAKANA) }
        val hangulCount = text.count { hasScript(it, UnicodeScript.HANGUL) }

        return japaneseKanaCount == 0 && hangulCount == 0
    }

    fun isHindi(text: String): Boolean = text.any { hasScript(it, UnicodeScript.DEVANAGARI) }

    fun hasOtherRomanizableScript(text: String): Boolean {
        return text.any { char ->
            if (!char.isLetter()) return@any false
            val script = UnicodeScript.of(char.code)
            script !in OTHER_ROMANIZATION_EXCLUDED_SCRIPTS
        }
    }

    fun shouldRomanizeLyricsLine(
        text: String,
        preferences: LyricsRomanizationPreferences,
    ): Boolean {
        if (!preferences.isEnabled || text.isBlank()) return false

        return when {
            preferences.romanizeJapanese && looksJapanese(text) -> true
            preferences.romanizeKorean && isKorean(text) -> true
            preferences.romanizeHindi && isHindi(text) -> true
            preferences.romanizeChinese && isChinese(text) -> true
            preferences.romanizeOther && hasOtherRomanizableScript(text) -> true
            else -> false
        }
    }

    suspend fun romanizeLyricsLine(
        text: String,
        preferences: LyricsRomanizationPreferences,
    ): String? {
        if (!shouldRomanizeLyricsLine(text, preferences)) return null

        val romanized = when {
            preferences.romanizeJapanese && looksJapanese(text) -> romanizeJapanese(text)
            preferences.romanizeKorean && isKorean(text) -> romanizeKorean(text)
            preferences.romanizeHindi && isHindi(text) -> romanizeWithIcu(text)
            preferences.romanizeChinese && isChinese(text) -> romanizeWithIcu(text)
            preferences.romanizeOther && hasOtherRomanizableScript(text) -> romanizeWithIcu(text)
            else -> null
        }

        return normalizeRomanizedText(text, romanized)
    }

    private suspend fun romanizeWithIcu(text: String): String = withContext(Dispatchers.Default) {
        genericRomanizationTransliterator.get().transliterate(text)
    }

    private fun normalizeRomanizedText(original: String, romanized: String?): String? {
        val normalized = romanized
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return normalized.takeUnless { it.equals(original.trim(), ignoreCase = true) }
    }

    private fun looksJapanese(text: String): Boolean {
        return text.any {
            hasScript(it, UnicodeScript.HIRAGANA) ||
                hasScript(it, UnicodeScript.KATAKANA) ||
                it == '々' ||
                it == '〆' ||
                it == 'ヶ'
        }
    }

    private fun hasScript(char: Char, script: UnicodeScript): Boolean {
        return char.isLetter() && UnicodeScript.of(char.code) == script
    }
}
