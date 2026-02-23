package com.jsonshow.ui.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsonshow.viewmodel.JsonViewModel
import com.jsonshow.viewmodel.ViewMode

private val modeIcons = mapOf(
    ViewMode.LIST to Icons.Default.List,
    ViewMode.FLASHCARD to Icons.Default.Style,
    ViewMode.TREE to Icons.Default.AccountTree,
    ViewMode.SYNTAX to Icons.Default.Code,
    ViewMode.TABLE to Icons.Default.TableChart
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(viewModel: JsonViewModel, onBack: () -> Unit, onSave: (String) -> Unit) {
    var showSearch by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAppendSheet by remember { mutableStateOf(false) }
    var appendError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showSaveDialog) {
        SaveDialog(
            initialName = viewModel.currentFileName ?: "",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name -> onSave(name); showSaveDialog = false }
        )
    }

    if (showAppendSheet) {
        AppendSheet(
            error = appendError,
            onDismiss = { showAppendSheet = false; appendError = null },
            onAppend = { newData ->
                val (err, msg) = viewModel.appendJson(context, newData)
                if (err == null) {
                    showAppendSheet = false; appendError = null
                    msg?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
                } else appendError = err
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(viewModel.currentFileName ?: "JsonShow") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAppendSheet = true }) {
                        Icon(Icons.Default.PlaylistAdd, "增加数据")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                ViewMode.entries.forEach { mode ->
                    NavigationBarItem(
                        selected = viewModel.currentMode == mode,
                        onClick = { viewModel.currentMode = mode },
                        icon = { Icon(modeIcons[mode]!!, mode.label) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (viewModel.parseError != null) {
                Text(
                    "解析错误: ${viewModel.parseError}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column {
                    if (showSearch) {
                        SearchBar(
                            query = viewModel.searchQuery,
                            onQueryChange = { viewModel.searchQuery = it },
                            onDismiss = { showSearch = false; viewModel.searchQuery = "" }
                        )
                    }
                    viewModel.parsedNode?.let { node ->
                        when (viewModel.currentMode) {
                            ViewMode.LIST -> ListView(node, viewModel.searchQuery)
                            ViewMode.FLASHCARD -> FlashcardView(node)
                            ViewMode.TREE -> TreeView(node, viewModel)
                            ViewMode.SYNTAX -> SyntaxView(viewModel.rawJson)
                            ViewMode.TABLE -> TableView(node)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName.ifEmpty { com.jsonshow.util.JsonStorage.generateName() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存 JSON") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件名") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppendSheet(error: String?, onDismiss: () -> Unit, onAppend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("增加数据", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("粘贴新的 JSON 数据，已有数据不会被修改或删除",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                placeholder = { Text("在此粘贴新的 JSON 数据...") },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(12.dp),
                isError = error != null
            )
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (text.isNotBlank()) onAppend(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("追加数据", Modifier.padding(vertical = 4.dp)) }
        }
    }
}
