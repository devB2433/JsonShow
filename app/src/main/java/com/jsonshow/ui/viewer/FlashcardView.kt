package com.jsonshow.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsonshow.model.JsonNode
import com.jsonshow.ui.theme.CardColors

private data class CardItem(val key: String, val value: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlashcardView(node: JsonNode) {
    val cards = remember(node) { collectCards(node) }

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有可展示的数据", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val pagerState = rememberPagerState { cards.size }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Progress indicator
        Text(
            "${pagerState.currentPage + 1} / ${cards.size}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Progress bar
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / cards.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Text(
            "点击卡片翻转  ←  左右滑动  →",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).padding(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            FlipCard(cards[page], page)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FlipCard(card: CardItem, index: Int) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )
    val colors = CardColors[index % CardColors.size]

    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable { flipped = !flipped },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (rotation <= 90f) listOf(colors.first, colors.second)
                        else listOf(colors.second, colors.first)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front - key
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "KEY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        card.key,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        lineHeight = 32.sp
                    )
                }
            } else {
                // Back - value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    Text(
                        "VALUE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        card.value,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        lineHeight = 28.sp
                    )
                }
            }
        }
    }
}

private fun collectCards(node: JsonNode): List<CardItem> = buildList {
    when (node) {
        is JsonNode.Arr -> node.items.forEach { item ->
            if (item is JsonNode.Obj && item.entries.size >= 2) {
                // Array of objects: first value = front, second value = back
                val front = nodeText(item.entries[0].second)
                val back = item.entries.drop(1).joinToString("\n") { (k, v) -> nodeText(v) }
                add(CardItem(front, back))
            } else {
                add(CardItem("#${size + 1}", nodeText(item)))
            }
        }
        is JsonNode.Obj -> node.entries.forEach { (k, v) ->
            add(CardItem(k, nodeText(v)))
        }
        is JsonNode.Primitive -> add(CardItem("value", node.value))
    }
}

private fun nodeText(node: JsonNode): String = when (node) {
    is JsonNode.Primitive -> node.value
    is JsonNode.Obj -> node.entries.joinToString(", ") { "${it.first}: ${nodeText(it.second)}" }
    is JsonNode.Arr -> node.items.joinToString(", ") { nodeText(it) }
}
