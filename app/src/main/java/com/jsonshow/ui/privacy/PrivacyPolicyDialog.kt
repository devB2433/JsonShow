package com.jsonshow.ui.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("隐私政策", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionText("云同步功能说明")
                BodyText(
                    "JsonShow 提供可选的 Google Drive 云同步功能，" +
                    "帮助你在多设备间同步 JSON 文件。"
                )
                Spacer(Modifier.height(12.dp))
                SectionText("数据存储")
                BodyText(
                    "你的文件存储在你自己的 Google Drive 应用专属文件夹中" +
                    "（appDataFolder），仅本应用可访问。" +
                    "我们不会在任何服务器上存储你的数据。"
                )
                Spacer(Modifier.height(12.dp))
                SectionText("权限范围")
                BodyText(
                    "我们仅请求 drive.appdata 权限，" +
                    "无法访问你 Google Drive 中的其他文件。"
                )
                Spacer(Modifier.height(12.dp))
                SectionText("你的权利")
                BodyText(
                    "你可以随时退出登录以停止云同步。" +
                    "退出后本地文件不受影响。" +
                    "你也可以在 Google 账号设置中撤销本应用的访问权限。"
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) { Text("接受并继续") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("拒绝") }
        }
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BodyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
