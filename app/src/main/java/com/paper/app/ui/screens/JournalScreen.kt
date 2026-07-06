package com.paper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paper.app.data.JournalEntry
import com.paper.app.data.JournalRepository
import com.paper.app.ui.editor.renderMarkdown
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat =
    DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy · h:mm a", Locale.getDefault())

@Composable
fun JournalScreen(
    repository: JournalRepository,
    onWrite: () -> Unit,
    onEditSchedule: () -> Unit,
    onOpenPrompts: () -> Unit,
    onChangePassword: () -> Unit,
    onInfo: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var entries by remember { mutableStateOf(repository.loadEntries().asReversed()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onWrite) {
                Icon(Icons.Outlined.Create, contentDescription = "Write")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    "Paper",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onChangePassword) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Change password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditSchedule) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = "Edit schedule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenPrompts) {
                    Icon(
                        Icons.Outlined.Category,
                        contentDescription = "Manage prompts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onInfo) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "About Paper",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nothing here yet.\nPaper will ask when it's time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            onDelete = {
                                repository.deleteEntry(entry.id)
                                entries = repository.loadEntries().asReversed()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: JournalEntry, onDelete: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dateFormat.format(
                    Instant.ofEpochMilli(entry.createdAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(renderMarkdown(entry.text), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
