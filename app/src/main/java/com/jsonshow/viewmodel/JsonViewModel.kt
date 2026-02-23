package com.jsonshow.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jsonshow.model.JsonNode
import com.jsonshow.parser.JsonParser
import com.jsonshow.sync.DriveSync
import com.jsonshow.sync.SyncManager
import com.jsonshow.sync.SyncResult
import com.jsonshow.util.AppPrefs
import com.jsonshow.util.FileUtils
import com.jsonshow.util.JsonStorage
import com.jsonshow.util.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ViewMode(val label: String) {
    LIST("列表"), FLASHCARD("闪卡"), TREE("树形"),
    SYNTAX("语法"), TABLE("表格")
}

class JsonViewModel : ViewModel() {
    var rawJson by mutableStateOf("")
    var parsedNode by mutableStateOf<JsonNode?>(null)
    var parseError by mutableStateOf<String?>(null)
    var currentMode by mutableStateOf(ViewMode.LIST)
    var searchQuery by mutableStateOf("")
    var expandedPaths by mutableStateOf(setOf("$"))
    var isLoading by mutableStateOf(false)
    var isDarkTheme by mutableStateOf<Boolean?>(null)

    var savedFiles by mutableStateOf<List<SavedFile>>(emptyList())
    var currentFileName by mutableStateOf<String?>(null)

    // --- Cloud sync state ---
    var googleAccount by mutableStateOf<GoogleSignInAccount?>(null)
    var isSyncing by mutableStateOf(false)
    var syncResult by mutableStateOf<SyncResult?>(null)
    var privacyAccepted by mutableStateOf(false)

    fun loadPrivacyState(context: Context) {
        viewModelScope.launch {
            privacyAccepted = AppPrefs.privacyAccepted(context).first()
        }
    }

    fun acceptPrivacy(context: Context) {
        viewModelScope.launch {
            AppPrefs.setPrivacyAccepted(context, true)
            privacyAccepted = true
        }
    }

    fun onSignedIn(account: GoogleSignInAccount) {
        googleAccount = account
    }

    fun onSignedOut() {
        googleAccount = null
        syncResult = null
    }

    fun syncWithDrive(context: Context) {
        val account = googleAccount ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            syncResult = null
            try {
                val driveSync = DriveSync(context, account)
                val manager = SyncManager(driveSync)
                syncResult = manager.sync(context)
                // Refresh local list after sync
                savedFiles = JsonStorage.list(context)
            } catch (e: Exception) {
                syncResult = SyncResult(errors = listOf("同步失败: ${e.message}"))
            } finally {
                isSyncing = false
            }
        }
    }

    fun loadJson(text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            isLoading = true
            rawJson = text
            expandedPaths = setOf("$")
            JsonParser.parse(text).fold(
                onSuccess = { parsedNode = it; parseError = null },
                onFailure = { parseError = it.message }
            )
            isLoading = false
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.readUri(context, uri)?.let { loadJson(it) }
                ?: run { parseError = "无法读取文件" }
        }
    }

    fun refreshSavedFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            savedFiles = JsonStorage.list(context)
        }
    }

    fun saveJson(context: Context, name: String): Boolean {
        if (rawJson.isBlank()) return false
        val success = JsonStorage.save(context, name, rawJson)
        if (success) {
            currentFileName = name
            refreshSavedFiles(context)
        }
        return success
    }

    /** Returns Pair(error, successMsg) — error is null on success */
    fun appendJson(context: Context, newData: String): Pair<String?, String?> {
        val result = JsonParser.merge(rawJson, newData)
        return result.fold(
            onSuccess = { mr ->
                loadJson(mr.json)
                currentFileName?.let { JsonStorage.save(context, it, mr.json) }
                val msg = "新增 ${mr.added} 条" +
                    if (mr.skipped > 0) "，跳过 ${mr.skipped} 条重复" else ""
                Pair(null, msg)
            },
            onFailure = { Pair(it.message, null) }
        )
    }

    fun loadSaved(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            JsonStorage.read(context, name)?.let {
                currentFileName = name
                loadJson(it)
            }
        }
    }

    fun deleteSaved(context: Context, name: String) {
        JsonStorage.delete(context, name)
        refreshSavedFiles(context)
    }

    fun toggleExpand(path: String) {
        expandedPaths = if (path in expandedPaths)
            expandedPaths - path else expandedPaths + path
    }

    fun expandAll() {
        parsedNode?.let { expandedPaths = collectAllPaths(it) }
    }

    fun collapseAll() {
        expandedPaths = setOf("$")
    }

    private fun collectAllPaths(node: JsonNode): Set<String> = buildSet {
        add(node.path)
        when (node) {
            is JsonNode.Obj -> node.entries.forEach { addAll(collectAllPaths(it.second)) }
            is JsonNode.Arr -> node.items.forEach { addAll(collectAllPaths(it)) }
            is JsonNode.Primitive -> {}
        }
    }
}
