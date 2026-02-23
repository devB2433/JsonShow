package com.jsonshow.model

sealed class JsonNode {
    abstract val path: String

    data class Obj(
        override val path: String,
        val entries: List<Pair<String, JsonNode>>
    ) : JsonNode()

    data class Arr(
        override val path: String,
        val items: List<JsonNode>
    ) : JsonNode()

    data class Primitive(
        override val path: String,
        val value: String,
        val type: PrimitiveType
    ) : JsonNode()
}

enum class PrimitiveType { STRING, NUMBER, BOOLEAN, NULL }
