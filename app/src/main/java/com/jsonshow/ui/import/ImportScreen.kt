package com.jsonshow.ui.import

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsonshow.util.SavedFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SyncIcon = Icons.Default.CloudSync
private val LoginIcon = Icons.Default.AccountCircle
private val LogoutIcon = Icons.Default.Logout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    savedFiles: List<SavedFile>,
    onJsonLoaded: (String) -> Unit,
    onOpenSaved: (String) -> Unit,
    onDeleteSaved: (String) -> Unit,
    isSignedIn: Boolean = false,
    isSyncing: Boolean = false,
    syncResultText: String? = null,
    onSignInClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    var showPasteSheet by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showSyncSnackbar by remember { mutableStateOf(false) }

    // Show sync result as snackbar
    LaunchedEffect(syncResultText) {
        if (syncResultText != null) showSyncSnackbar = true
    }

    if (showPasteSheet) {
        PasteBottomSheet(
            onDismiss = { showPasteSheet = false },
            onLoad = { onJsonLoaded(it); showPasteSheet = false }
        )
    }

    if (showPromptDialog) {
        PromptTemplateDialog(onDismiss = { showPromptDialog = false })
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("JsonShow", fontWeight = FontWeight.Bold)
                        Text("把你的数据json化并展示出来的小工具",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    if (isSignedIn) {
                        IconButton(
                            onClick = onSyncClick,
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(SyncIcon, "同步")
                            }
                        }
                        IconButton(onClick = onSignOutClick) {
                            Icon(LogoutIcon, "退出登录")
                        }
                    } else {
                        IconButton(onClick = onSignInClick) {
                            Icon(LoginIcon, "登录 Google")
                        }
                    }
                    IconButton(onClick = { showPromptDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, "Prompt 模板")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPasteSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("新建") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (savedFiles.isEmpty()) {
                EmptyState(Modifier.fillMaxSize()) { showPasteSheet = true }
            } else {
                SavedFilesList(
                    files = savedFiles,
                    onOpen = onOpenSaved,
                    onDelete = onDeleteSaved,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Sync result snackbar
            if (showSyncSnackbar && syncResultText != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { showSyncSnackbar = false }) {
                            Text("确定")
                        }
                    }
                ) {
                    Text(syncResultText)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DataObject, null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("还没有保存的文件", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("点击下方按钮粘贴 JSON 开始使用",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium)
    }
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
private fun SavedFilesList(
    files: List<SavedFile>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "${files.size} 个文件",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        items(files, key = { it.name }) { saved ->
            var showDelete by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(saved.name) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DataObject, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(saved.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(dateFormat.format(Date(saved.lastModified)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (showDelete) {
                        TextButton(onClick = { onDelete(saved.name); showDelete = false }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { showDelete = false }) { Text("取消") }
                    } else {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, "删除",
                                tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) } // FAB spacing
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteBottomSheet(onDismiss: () -> Unit, onLoad: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("粘贴 JSON", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(260.dp),
                placeholder = { Text("在此粘贴 JSON 内容...") },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (text.isNotBlank()) onLoad(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("加载", Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

private val PROMPT_TEMPLATE = """
You are a professional translator. I give you Chinese or English, you give me the definition only. No explanations, no examples.

Output ONLY valid JSON — a JSON array of objects, no extra text:

[
  {"word": "ephemeral", "meaning": "短暂的"},
  {"word": "你好", "meaning": "hello; hi"}
]

Rules:
- The first field is the FRONT of a flashcard, the second field is the BACK.
- You may use any field names (e.g. "word"/"meaning", "question"/"answer"), but order matters: first = front, second = back.
- Output raw JSON only. No markdown, no code fences, no explanation.

Now generate the JSON based on the user's request.
""".trimIndent()

@Composable
private fun PromptTemplateDialog(onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prompt 模板") },
        text = {
            Column {
                Text(
                    "复制以下 Prompt 发给 AI，让它生成 JsonShow 可用的 JSON 格式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        PROMPT_TEMPLATE,
                        modifier = Modifier
                            .padding(12.dp)
                            .height(240.dp)
                            .verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                clipboardManager.setText(AnnotatedString(PROMPT_TEMPLATE))
                copied = true
            }) {
                Text(if (copied) "已复制!" else "复制 Prompt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
