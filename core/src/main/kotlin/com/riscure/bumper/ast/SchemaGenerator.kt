package com.riscure.bumper.ast

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

@OptIn(ExperimentalSerializationApi::class)
fun main(): Unit {
    val schemas = ProtoBufSchemaGenerator.generateSchemaText(listOf(TranslationUnit.serializer<Unit,Unit>(
        Unit.serializer(),
        Unit.serializer(),
    ).descriptor))
    println(schemas)
}