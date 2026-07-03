package com.paper.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.paper.app.ui.editor.MarkdownVisualTransformation

/** Wraps the selection in [marker]; with no selection, inserts a pair and parks the cursor inside. */
private fun toggleWrap(value: TextFieldValue, marker: String): TextFieldValue {
    val sel = value.selection
    val text = value.text
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)

    val selected = text.substring(start, end)
    val alreadyWrapped = selected.length >= marker.length * 2 &&
        selected.startsWith(marker) && selected.endsWith(marker)

    return if (alreadyWrapped) {
        val unwrapped = selected.removeSurrounding(marker)
        TextFieldValue(
            text = text.substring(0, start) + unwrapped + text.substring(end),
            selection = TextRange(start, start + unwrapped.length)
        )
    } else {
        TextFieldValue(
            text = text.substring(0, start) + marker + selected + marker + text.substring(end),
            selection = if (selected.isEmpty())
                TextRange(start + marker.length)
            else
                TextRange(start, end + marker.length * 2)
        )
    }
}

/** Prefixes every line touched by the selection, e.g. "> ", "- ", "1. ". */
private fun prefixLines(value: TextFieldValue, prefix: (Int) -> String): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
    val end = maxOf(value.selection.start, value.selection.end)

    val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
        if (start == 0) 0 else if (it == -1) 0 else it + 1
    }
    val block = text.substring(lineStart, end)
    val lines = block.split('\n')
    val prefixed = lines.mapIndexed { i, line -> prefix(i) + line }.joinToString("\n")
    val added = prefixed.length - block.length

    return TextFieldValue(
        text = text.substring(0, lineStart) + prefixed + text.substring(end),
        selection = TextRange(end + added)
    )
}

@Composable
fun EditorScreen(onSave: (String) -> Unit, onDiscard: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue()) }
    val transformation = remember { MarkdownVisualTransformation() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onDiscard) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Discard",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.weight(1f))
            IconButton(onClick = { onSave(value.text) }) {
                Icon(Icons.Outlined.Check, contentDescription = "Save")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                visualTransformation = transformation,
                textStyle = MaterialTheme.typography.bodyLarge
                    .copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            )
            if (value.text.isEmpty()) {
                Text(
                    "What's on your mind?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            ToolbarButton(Icons.Outlined.FormatBold, "Bold") {
                value = toggleWrap(value, "*")
            }
            ToolbarButton(Icons.Outlined.FormatItalic, "Italic") {
                value = toggleWrap(value, "_")
            }
            ToolbarButton(Icons.Outlined.StrikethroughS, "Strikethrough") {
                value = toggleWrap(value, "~")
            }
            ToolbarButton(Icons.Outlined.Code, "Code") {
                value = toggleWrap(value, "`")
            }
            ToolbarButton(Icons.Outlined.FormatQuote, "Quote") {
                value = prefixLines(value) { "> " }
            }
            ToolbarButton(Icons.AutoMirrored.Outlined.FormatListBulleted, "Bulleted list") {
                value = prefixLines(value) { "- " }
            }
            ToolbarButton(Icons.Outlined.FormatListNumbered, "Numbered list") {
                value = prefixLines(value) { i -> "${i + 1}. " }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
