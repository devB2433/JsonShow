package com.jsonshow.parser

import com.jsonshow.model.JsonNode
import com.jsonshow.model.PrimitiveType
import kotlinx.serialization.json.*

object JsonParser {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    fun parse(raw: String): Result<JsonNode> = runCatching {
        json.parseToJsonElement(raw).toNode("$")
    }

    fun prettyPrint(raw: String): String = runCatching {
        val element = json.parseToJsonElement(raw)
        Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
    }.getOrDefault(raw)

    fun merge(existing: String, newData: String): Result<MergeResult> = runCatching {
        val old = json.parseToJsonElement(existing)
        val new = json.parseToJsonElement(newData)
        val compact = Json { prettyPrint = false }
        when {
            old is JsonArray && (new is JsonArray || new is JsonObject) -> {
                // Build HashSet of existing item fingerprints — O(n)
                val seen = HashSet<String>(old.size)
                old.forEach { seen.add(compact.encodeToString(JsonElement.serializer(), it)) }
                // Filter new items, skip duplicates — O(m)
                val newItems = if (new is JsonArray) new.toList() else listOf(new)
                var skipped = 0
                val unique = newItems.filter { item ->
                    val key = compact.encodeToString(JsonElement.serializer(), item)
                    if (key in seen) { skipped++; false } else { seen.add(key); true }
                }
                val merged = json.encodeToString(JsonElement.serializer(), JsonArray(old + unique))
                MergeResult(merged, unique.size, skipped)
            }
            old is JsonObject && new is JsonObject -> {
                val map = old.toMutableMap()
                var added = 0; var skipped = 0
                new.forEach { (k, v) ->
                    if (k !in map) { map[k] = v; added++ } else skipped++
                }
                MergeResult(json.encodeToString(JsonElement.serializer(), JsonObject(map)), added, skipped)
            }
            else -> {
                val merged = json.encodeToString(JsonElement.serializer(), JsonArray(listOf(old, new)))
                MergeResult(merged, 1, 0)
            }
        }
    }

    data class MergeResult(val json: String, val added: Int, val skipped: Int)

    private fun JsonElement.toNode(path: String): JsonNode = when (this) {
        is JsonObject -> JsonNode.Obj(
            path = path,
            entries = entries.map { (k, v) -> k to v.toNode("$path.$k") }
        )
        is JsonArray -> JsonNode.Arr(
            path = path,
            items = mapIndexed { i, v -> v.toNode("$path[$i]") }
        )
        is JsonPrimitive -> {
            val type = when {
                isString -> PrimitiveType.STRING
                content == "null" -> PrimitiveType.NULL
                content == "true" || content == "false" -> PrimitiveType.BOOLEAN
                else -> PrimitiveType.NUMBER
            }
            JsonNode.Primitive(path, content, type)
        }
    }
}
