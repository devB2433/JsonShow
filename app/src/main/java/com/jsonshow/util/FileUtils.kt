package com.jsonshow.util

import android.content.Context
import android.net.Uri

object FileUtils {
    fun readUri(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
}
