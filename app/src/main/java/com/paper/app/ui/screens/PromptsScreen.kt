package com.paper.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paper.app.data.PromptCategory

@Composable
fun PromptsScreen(
    enabledCategoryIds: Set<String>,
    onToggle: (categoryId: String, enabled: Boolean) -> Unit,
    onEditSchedule: (categoryId: String) -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
        ) {
            Text(
                "Prompts",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Turn on the topics you'd like to be asked about. Each one keeps its own schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            PromptCategory.entries.forEach { category ->
                val enabled = category.id in enabledCategoryIds
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = enabled,
                                onValueChange = { onToggle(category.id, it) }
                            )
                    ) {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = enabled, onCheckedChange = { onToggle(category.id, it) })
                    }
                    if (enabled) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Tap to edit schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onEditSchedule(category.id) }
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
