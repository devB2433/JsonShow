package com.jsonshow.sync

import android.content.Context
import com.jsonshow.util.JsonStorage

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val unchanged: Int = 0,
    val errors: List<String> = emptyList()
) {
    val summary: String
        get() = buildString {
            if (uploaded > 0) append("上传 $uploaded 个")
            if (downloaded > 0) {
                if (isNotEmpty()) append("，")
                append("下载 $downloaded 个")
            }
            if (unchanged > 0) {
                if (isNotEmpty()) append("，")
                append("$unchanged 个无变化")
            }
            if (errors.isNotEmpty()) {
                if (isNotEmpty()) append("，")
                append("${errors.size} 个失败")
            }
            if (isEmpty()) append("已是最新")
        }
}

class SyncManager(private val driveSync: DriveSync) {

    /**
     * Two-way sync: compare local files with cloud by name.
     * Conflict resolution: last-modified-wins.
     */
    suspend fun sync(context: Context): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var unchanged = 0
        val errors = mutableListOf<String>()

        try {
            val cloudFiles = driveSync.listFiles()
            val localFiles = JsonStorage.list(context)

            val cloudByName = cloudFiles.associateBy {
                it.name.removeSuffix(".json")
            }
            val localByName = localFiles.associateBy { it.name }

            // All unique file names
            val allNames = cloudByName.keys + localByName.keys

            for (name in allNames) {
                try {
                    val cloud = cloudByName[name]
                    val local = localByName[name]

                    when {
                        // Local only → upload
                        cloud == null && local != null -> {
                            val content = JsonStorage.read(context, name) ?: continue
                            driveSync.uploadFile("$name.json", content)
                            uploaded++
                        }
                        // Cloud only → download
                        cloud != null && local == null -> {
                            val content = driveSync.downloadFile(cloud.id)
                            JsonStorage.save(context, name, content)
                            downloaded++
                        }
                        // Both exist → last-modified-wins
                        cloud != null && local != null -> {
                            if (local.lastModified > cloud.modifiedTime) {
                                val content = JsonStorage.read(context, name) ?: continue
                                driveSync.uploadFile("$name.json", content)
                                uploaded++
                            } else if (cloud.modifiedTime > local.lastModified) {
                                val content = driveSync.downloadFile(cloud.id)
                                JsonStorage.save(context, name, content)
                                downloaded++
                            } else {
                                unchanged++
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.add("$name: ${e.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("同步失败: ${e.message}")
        }

        return SyncResult(uploaded, downloaded, unchanged, errors)
    }
}
