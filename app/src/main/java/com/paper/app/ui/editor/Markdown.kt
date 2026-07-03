package com.paper.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * Slack-flavored markdown subset: *bold*, _italic_, ~strike~, `code`,
 * ```code block```, and line prefixes "> " (quote), "- " (bullet), "1. " (numbered).
 * Entries are stored as this plain markup; styling is applied at display time.
 */

private data class InlineRule(val marker: String, val style: SpanStyle)

private val inlineRules = listOf(
    InlineRule("*", SpanStyle(fontWeight = FontWeight.Bold)),
    InlineRule("_", SpanStyle(fontStyle = FontStyle.Italic)),
    InlineRule("~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
    InlineRule(
        "`",
        SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFB0503C),
            background = Color(0x14808080)
        )
    )
)

private val codeBlockStyle = SpanStyle(
    fontFamily = FontFamily.Monospace,
    background = Color(0x14808080)
)

private fun findSpans(text: String): List<Pair<TextRange, SpanStyle>> {
    val spans = mutableListOf<Pair<TextRange, SpanStyle>>()

    // Code blocks first; inline rules are not applied inside them.
    val blocked = BooleanArray(text.length)
    var i = 0
    while (i < text.length) {
        val open = text.indexOf("```", i)
        if (open == -1) break
        val close = text.indexOf("```", open + 3)
        if (close == -1) break
        spans += TextRange(open, close + 3) to codeBlockStyle
        for (j in open..close + 2) blocked[j] = true
        i = close + 3
    }

    for (rule in inlineRules) {
        var from = 0
        while (true) {
            val open = text.indexOf(rule.marker, from)
            if (open == -1) break
            if (blocked[open]) { from = open + 1; continue }
            val close = text.indexOf(rule.marker, open + 1)
            if (close == -1) break
            if (blocked[close] || close == open + 1) { from = close; continue }
            spans += TextRange(open, close + 1) to rule.style
            from = close + 1
        }
    }
    return spans
}

/**
 * Styles the raw markup in place without hiding markers, so cursor offsets in
 * the editor stay identical to the underlying string.
 */
class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = buildAnnotatedString {
            append(text.text)
            findSpans(text.text).forEach { (range, style) ->
                addStyle(style, range.start, range.end)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/** Read-view rendering for saved entries: same styles, markers still visible. */
fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    findSpans(text).forEach { (range, style) ->
        addStyle(style, range.start, range.end)
    }
}
