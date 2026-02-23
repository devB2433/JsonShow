package com.jsonshow.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsonshow.parser.JsonParser
import com.jsonshow.ui.theme.*

@Composable
fun SyntaxView(rawJson: String) {
    val pretty = remember(rawJson) { JsonParser.prettyPrint(rawJson) }
    val annotated = remember(pretty) { highlightJson(pretty) }

    SelectionContainer {
        Text(
            text = annotated,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}

private fun highlightJson(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // String (key or value)
            text[i] == '"' -> {
                val end = findStringEnd(text, i)
                val str = text.substring(i, end)
                // Check if this is a key (followed by ':')
                val afterStr = text.substring(end).trimStart()
                val color = if (afterStr.startsWith(":")) SyntaxKey else SyntaxString
                withStyle(SpanStyle(color = color)) { append(str) }
                i = end
            }
            // Number
            text[i].isDigit() || (text[i] == '-' && i + 1 < text.length && text[i + 1].isDigit()) -> {
                val start = i
                while (i < text.length && (text[i].isDigit() || text[i] in ".eE+-")) i++
                withStyle(SpanStyle(color = SyntaxNumber)) { append(text.substring(start, i)) }
            }
            // true/false
            text.startsWith("true", i) -> {
                withStyle(SpanStyle(color = SyntaxBoolean)) { append("true") }; i += 4
            }
            text.startsWith("false", i) -> {
                withStyle(SpanStyle(color = SyntaxBoolean)) { append("false") }; i += 5
            }
            // null
            text.startsWith("null", i) -> {
                withStyle(SpanStyle(color = SyntaxNull)) { append("null") }; i += 4
            }
            // Braces/brackets
            text[i] in "{}[]" -> {
                withStyle(SpanStyle(color = SyntaxBrace)) { append(text[i].toString()) }; i++
            }
            else -> { append(text[i].toString()); i++ }
        }
    }
}

private fun findStringEnd(text: String, start: Int): Int {
    var i = start + 1
    while (i < text.length) {
        if (text[i] == '\\') { i += 2; continue }
        if (text[i] == '"') return i + 1
        i++
    }
    return text.length
}
