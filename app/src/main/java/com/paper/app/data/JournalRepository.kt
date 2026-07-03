package com.paper.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** Markdown-formatted body, as produced by the editor. */
    val text: String
)

/**
 * Journal entries live in a single JSON file in app-private internal storage
 * (filesDir), so no other app can read it and nothing leaves the device.
 * v2: encrypt this file with a key derived from the user's password so the
 * content is unreadable even with direct file access (see README).
 */
class JournalRepository(context: Context) {

    private val file = File(context.filesDir, "journal.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun loadEntries(): List<JournalEntry> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<JournalEntry>>(file.readText()) }
            .getOrDefault(emptyList())
    }

    fun addEntry(text: String): JournalEntry {
        val entry = JournalEntry(text = text)
        val all = loadEntries() + entry
        writeAtomically(json.encodeToString(all))
        return entry
    }

    fun deleteEntry(id: String) {
        val remaining = loadEntries().filterNot { it.id == id }
        writeAtomically(json.encodeToString(remaining))
    }

    /** Write via temp file + rename so a crash mid-write can't corrupt the journal. */
    private fun writeAtomically(content: String) {
        val tmp = File(file.parentFile, "journal.json.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {
            file.writeText(content)
            tmp.delete()
        }
    }
}
