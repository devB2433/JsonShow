package com.jsonshow.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedFile(val name: String, val file: File, val lastModified: Long)

object JsonStorage {
    private fun dir(context: Context): File =
        File(context.filesDir, "saved_json").also { it.mkdirs() }

    fun list(context: Context): List<SavedFile> =
        dir(context).listFiles()?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { SavedFile(it.nameWithoutExtension, it, it.lastModified()) }
            ?: emptyList()

    fun save(context: Context, name: String, content: String): Boolean = runCatching {
        File(dir(context), "$name.json").writeText(content)
    }.isSuccess

    fun read(context: Context, name: String): String? = runCatching {
        File(dir(context), "$name.json").readText()
    }.getOrNull()

    fun delete(context: Context, name: String): Boolean = runCatching {
        File(dir(context), "$name.json").delete()
    }.getOrDefault(false)

    fun generateName(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
