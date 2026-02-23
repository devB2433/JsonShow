package com.jsonshow.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsonshow.model.JsonNode

@Composable
fun TableView(node: JsonNode) {
    val (headers, rows) = remember(node) { extractTable(node) }

    if (headers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("此 JSON 不适合表格展示\n请使用列表或树形视图")
        }
        return
    }

    val colWidth = 140.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        // Header row
        item {
            Row(Modifier.padding(4.dp)) {
                headers.forEach { h ->
                    Box(Modifier.width(colWidth).padding(8.dp)) {
                        Text(h, fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HorizontalDivider(thickness = 2.dp)
        }

        // Data rows
        items(rows) { row ->
            Row(Modifier.padding(4.dp)) {
                headers.forEach { h ->
                    Box(Modifier.width(colWidth).padding(8.dp)) {
                        Text(
                            row[h] ?: "",
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

private fun extractTable(node: JsonNode): Pair<List<String>, List<Map<String, String>>> {
    val arr = when (node) {
        is JsonNode.Arr -> node.items
        is JsonNode.Obj -> {
            // Single object -> one-row table
            return Pair(
                node.entries.map { it.first },
                listOf(node.entries.associate { (k, v) -> k to nodeToString(v) })
            )
        }
        is JsonNode.Primitive -> return Pair(emptyList(), emptyList())
    }

    // Collect all keys from objects in the array
    val headers = linkedSetOf<String>()
    val rows = mutableListOf<Map<String, String>>()

    for (item in arr) {
        if (item is JsonNode.Obj) {
            item.entries.forEach { headers.add(it.first) }
            rows.add(item.entries.associate { (k, v) -> k to nodeToString(v) })
        }
    }

    if (headers.isEmpty()) return Pair(emptyList(), emptyList())
    return Pair(headers.toList(), rows)
}

private fun nodeToString(node: JsonNode): String = when (node) {
    is JsonNode.Primitive -> node.value
    is JsonNode.Obj -> "{ ${node.entries.size} 项 }"
    is JsonNode.Arr -> "[ ${node.items.size} 项 ]"
}
