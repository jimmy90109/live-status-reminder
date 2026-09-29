package com.github.jimmy90109.livestatus

import java.text.BreakIterator
import java.util.Locale

internal object ShortCriticalTextFormatter {
    private const val MAX_GRAPHEMES = 7
    private const val MAX_WIDTH = 8
    private const val NARROW_WIDTH = 1
    private const val WIDE_WIDTH = 2

    fun format(text: String): String {
        val graphemeIterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply {
            setText(text)
        }
        var graphemeCount = 0
        var usedWidth = 0
        var acceptedEnd = graphemeIterator.first()
        var graphemeEnd = graphemeIterator.next()
        while (graphemeEnd != BreakIterator.DONE && graphemeCount < MAX_GRAPHEMES) {
            val graphemeWidth = graphemeWidth(text, acceptedEnd, graphemeEnd)
            if (usedWidth + graphemeWidth > MAX_WIDTH) break
            usedWidth += graphemeWidth
            graphemeCount += 1
            acceptedEnd = graphemeEnd
            graphemeEnd = graphemeIterator.next()
        }
        return text.substring(0, acceptedEnd)
    }

    private fun graphemeWidth(text: String, startIndex: Int, endIndex: Int): Int {
        var index = startIndex
        while (index < endIndex) {
            val codePoint = text.codePointAt(index)
            if (codePoint.isWideCharacter()) return WIDE_WIDTH
            index += Character.charCount(codePoint)
        }
        return NARROW_WIDTH
    }

    private fun Int.isWideCharacter(): Boolean {
        val script = Character.UnicodeScript.of(this)
        return Character.isIdeographic(this) ||
            script == Character.UnicodeScript.HAN ||
            script == Character.UnicodeScript.HANGUL ||
            script == Character.UnicodeScript.HIRAGANA ||
            script == Character.UnicodeScript.KATAKANA ||
            this in 0x2E80..0xA4CF ||
            this in 0xFE10..0xFE6F ||
            this in 0xFF01..0xFF60 ||
            this in 0xFFE0..0xFFE6 ||
            Character.getType(this) == Character.OTHER_SYMBOL.toInt()
    }
}
