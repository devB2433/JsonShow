package com.jsonshow.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsonshow.model.JsonNode
import com.jsonshow.model.PrimitiveType
import com.jsonshow.viewmodel.JsonViewModel

@Composable
fun TreeView(node: JsonNode, viewModel: JsonViewModel) {
    Column {
        Row(Modifier.padding(8.dp)) {
            TextButton(onClick = { viewModel.expandAll() }) { Text("全部展开") }
            TextButton(onClick = { viewModel.collapseAll() }) { Text("全部折叠") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            item { TreeNodeContent(node, 0, null, viewModel) }
        }
    }
}

@Composable
private fun TreeNodeContent(
    node: JsonNode, depth: Int, key: String?, viewModel: JsonViewModel
) {
    val expanded = node.path in viewModel.expandedPaths
    val indent = (depth * 20).dp

    when (node) {
        is JsonNode.Primitive -> {
            Row(
                Modifier.padding(start = indent, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Circle, null, Modifier.size(8.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                if (key != null) {
                    Text("$key: ", fontWeight = FontWeight.Medium)
                }
                Text(
                    node.value,
                    fontFamily = FontFamily.Monospace,
                    color = when (node.type) {
                        PrimitiveType.STRING -> MaterialTheme.colorScheme.primary
                        PrimitiveType.NUMBER -> MaterialTheme.colorScheme.tertiary
                        PrimitiveType.BOOLEAN -> MaterialTheme.colorScheme.secondary
                        PrimitiveType.NULL -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
        is JsonNode.Obj -> {
            Row(
                Modifier
                    .padding(start = indent, top = 2.dp, bottom = 2.dp)
                    .clickable { viewModel.toggleExpand(node.path) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    "toggle", Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    buildString {
                        if (key != null) append("$key: ")
                        append("{ ${node.entries.size} 项 }")
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    node.entries.forEach { (k, v) ->
                        TreeNodeContent(v, depth + 1, k, viewModel)
                    }
                }
            }
        }
        is JsonNode.Arr -> {
            Row(
                Modifier
                    .padding(start = indent, top = 2.dp, bottom = 2.dp)
                    .clickable { viewModel.toggleExpand(node.path) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    "toggle", Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    buildString {
                        if (key != null) append("$key: ")
                        append("[ ${node.items.size} 项 ]")
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    node.items.forEachIndexed { i, v ->
                        TreeNodeContent(v, depth + 1, "#${i + 1}", viewModel)
                    }
                }
            }
        }
    }
}
