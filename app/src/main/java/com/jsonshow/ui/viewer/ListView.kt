package com.jsonshow.ui.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsonshow.model.JsonNode
import com.jsonshow.model.PrimitiveType

private data class FlatItem(
    val depth: Int,
    val key: String,
    val display: String,
    val type: PrimitiveType?,
    val isContainer: Boolean = false
)

@Composable
fun ListView(node: JsonNode, searchQuery: String) {
    val items = remember(node, searchQuery) {
        flatten(node, 0).let { list ->
            if (searchQuery.isBlank()) list
            else {
                val q = searchQuery.lowercase()
                list.filter { it.key.lowercase().contains(q) || it.display.lowercase().contains(q) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            ListItem(
                headlineContent = {
                    Text(
                        item.key,
                        fontWeight = if (item.isContainer) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                },
                supportingContent = {
                    Text(
                        item.display,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = when (item.type) {
                            PrimitiveType.STRING -> MaterialTheme.colorScheme.primary
                            PrimitiveType.NUMBER -> MaterialTheme.colorScheme.tertiary
                            PrimitiveType.BOOLEAN -> MaterialTheme.colorScheme.secondary
                            PrimitiveType.NULL -> MaterialTheme.colorScheme.outline
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                modifier = Modifier.padding(start = (item.depth * 16).dp)
            )
            if (index < items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 8.dp))
        }
    }
}

private fun flatten(node: JsonNode, depth: Int): List<FlatItem> = buildList {
    when (node) {
        is JsonNode.Obj -> node.entries.forEach { (k, v) ->
            when (v) {
                is JsonNode.Primitive -> add(FlatItem(depth, k, v.value, v.type))
                is JsonNode.Obj -> {
                    add(FlatItem(depth, k, "{ ${v.entries.size} 项 }", null, true))
                    addAll(flatten(v, depth + 1))
                }
                is JsonNode.Arr -> {
                    add(FlatItem(depth, k, "[ ${v.items.size} 项 ]", null, true))
                    addAll(flatten(v, depth + 1))
                }
            }
        }
        is JsonNode.Arr -> node.items.forEachIndexed { i, v ->
            when {
                v is JsonNode.Obj && v.entries.size >= 2 -> {
                    val front = nodeText(v.entries[0].second)
                    val back = v.entries.drop(1).joinToString("  ·  ") { (_, n) -> nodeText(n) }
                    add(FlatItem(depth, front, back, PrimitiveType.STRING))
                }
                v is JsonNode.Primitive -> add(FlatItem(depth, "#${i + 1}", v.value, v.type))
                v is JsonNode.Obj -> {
                    add(FlatItem(depth, "#${i + 1}", "{ ${v.entries.size} 项 }", null, true))
                    addAll(flatten(v, depth + 1))
                }
                v is JsonNode.Arr -> {
                    add(FlatItem(depth, "#${i + 1}", "[ ${v.items.size} 项 ]", null, true))
                    addAll(flatten(v, depth + 1))
                }
            }
        }
        is JsonNode.Primitive -> add(FlatItem(depth, "value", node.value, node.type))
    }
}

private fun nodeText(node: JsonNode): String = when (node) {
    is JsonNode.Primitive -> node.value
    is JsonNode.Obj -> node.entries.joinToString(", ") { "${it.first}: ${nodeText(it.second)}" }
    is JsonNode.Arr -> node.items.joinToString(", ") { nodeText(it) }
}
