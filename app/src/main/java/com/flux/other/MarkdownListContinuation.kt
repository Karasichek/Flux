package com.flux.other

/**
 * Describes the prefix that should be inserted after the user starts a new line.
 * Keeping this logic independent from Compose makes editor behavior deterministic and testable.
 */
data class ListContinuation(
    val prefix: String,
    val removeExistingPrefix: Boolean = false
)

/**
 * Calculates Markdown list continuation for the line immediately before [cursor].
 * Returns null for ordinary paragraphs and blank lines.
 */
fun markdownListContinuation(text: CharSequence, cursor: Int): ListContinuation? {
    if (cursor <= 0 || cursor > text.length || text[cursor - 1] != '\n') return null

    val lineStart = text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
    val previousLine = text.substring(lineStart, cursor - 1)
    if (previousLine.isBlank()) return null

    val indentation = Regex("^\\s*").find(previousLine)?.value.orEmpty()
    val body = previousLine.removePrefix(indentation)

    val unordered = Regex("^([-*+])(?:\\s+)(.*)$").matchEntire(body)
    if (unordered != null) {
        val marker = unordered.groupValues[1]
        val content = unordered.groupValues[2]
        val prefix = "$indentation$marker "
        return if (content.isBlank()) {
            ListContinuation("", removeExistingPrefix = true)
        } else {
            ListContinuation(prefix)
        }
    }

    val ordered = Regex("^(\\d+)([.)])(?:\\s+)(.*)$").matchEntire(body)
    if (ordered != null) {
        val number = ordered.groupValues[1].toIntOrNull() ?: return null
        val delimiter = ordered.groupValues[2]
        val content = ordered.groupValues[3]
        val prefix = "$indentation${number + 1}$delimiter "
        return if (content.isBlank()) {
            ListContinuation("", removeExistingPrefix = true)
        } else {
            ListContinuation(prefix)
        }
    }

    return null
}
