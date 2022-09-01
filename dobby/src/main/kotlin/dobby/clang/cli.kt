package com.riscure.dobby.clang.cli

import arrow.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.net.URL

data class Spec(val options: Set<OptionSpec>) {

    val byName: Map<String, OptionSpec> by lazy {
        options.associateBy { it.name }
    }

    val byKey: Map<String, OptionSpec> by lazy {
        options.associateBy { it.key }
    }

    /**
     * Map of aliases for every option by name.
     * An alias may only be an alias in combination with a specific argument.
     * For example, the map should contain an entry: mcpu_EQ to { .., "mv5"},
     * If we look at byName["mv5"], we find that this option is equivalent
     * to "mcpu=hexagonv5", encoded by the fact that aliasFor = Some("mcpu_EQ") and
     * aliasArgs = ["hexagonv5"].
     */
    val aliasing: Map<String, Set<String>> by lazy {
        val ali:Map<String,MutableSet<String>> = byKey.mapValues { mutableSetOf() }

        // iterate over all options,
        // add a reverse alias mapping to all declared aliases
        byKey.forEach { (key, opt) ->
            opt.aliasFor.tap { aliasName ->
                // only insert if the aliasName exists in our option spec
                ali[aliasName].toOption().tap { it.add(key) }
            }
        }

        ali
    }

    sealed class OptType {
        object Toggle : OptType()
        object Joined : OptType()
        object CommaJoined : OptType()
        object Separate : OptType()
        object JoinedOrSeparate : OptType()
        object JoinedAndSeparate : OptType()
        data class MultiArg(val num: Int): OptType()
    }

    data class OptionSpec(
        /* Unique name that the clang spec uses to reference this option */
        val key       : String,
        /* Human readable name */
        val name      : String,
        val prefixes  : Set<String>,
        val type      : OptType = OptType.Toggle,
        val aliasFor  : Option<String> = None, // set of declared aliased options
        val aliasArgs : List<String> = listOf() // list of arguments passed to the aliases
    ) {
        companion object {
            private fun readClangKind(json: JsonElement): Option<OptType> {
                val err = "Could not decode json for clang kind %s"
                return try {
                    val kindEl = json.jsonObject["Kind"]!!.jsonObject["def"]!!
                    when (val kind = kindEl.jsonPrimitive.content) {
                        "KIND_COMMAJOINED" -> OptType.CommaJoined.some()
                        "KIND_JOINED_AND_SEPARATE" -> OptType.JoinedAndSeparate.some()
                        "KIND_JOINED" -> OptType.Joined.some()
                        "KIND_SEPARATE" -> OptType.Separate.some()
                        "KIND_FLAG" -> OptType.Toggle.some()
                        "KIND_JOINED_OR_SEPARATE" -> OptType.JoinedOrSeparate.some()
                        "KIND_MULTIARG" -> OptType.MultiArg(0).some() // TODO
                        else -> None
                    }
                }
                catch (e: Throwable) { throw RuntimeException(err.format(json)) }
            }


            /**
             *  Read an option descriptions from the json spec.
             * @throws RuntimeException when the json is unexpected
             */
            fun readsClangOpt(key: String, json: JsonElement): Option<OptionSpec> =
                try {
                    val jobj = json.jsonObject
                    readClangKind(jobj)
                        .map { kind ->
                            OptionSpec(
                                key,
                                jobj["Name"]!!.jsonPrimitive.content,
                                jobj["Prefixes"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
                                kind,
                                jobj["Alias"]
                                    .toOption()
                                    .flatMap { if (it is JsonNull) { None } else { it.jsonObject.some() }}
                                    .flatMap { it.jsonObject["def"].toOption().map { d -> d.jsonPrimitive.content } }
                            )
                        }
                } catch (e : Throwable) {
                    throw RuntimeException("Failed to parse json as clang option spec: ${json.toString()}")
                }
            }
    }

    companion object {
        private val specURL: URL = Spec::class
            .java.getResource("/clang.options.json")
            .let {
                if (it == null) {
                    throw RuntimeException("Missing resource 'clang.options.json'")
                }
                it
            }

        @OptIn(ExperimentalSerializationApi::class)
        private fun specJSON(): JsonElement = Json.decodeFromStream(specURL.openStream())

        fun reads(json: JsonElement): Spec {
            val jobj = json.jsonObject

            // some meta-keys are prefixed with an exclamation mark
            // the rest are option defs
            val cats = jobj["!instanceof"]
            val opts = jobj
                .filterKeys { !it.startsWith("!") }
                .filterValues {
                    try {
                        // we select defs by the superclass "Option"
                        it.jsonObject["!superclasses"]!!.jsonArray
                            .map { it.jsonPrimitive.content }
                            .contains("Option")
                    } catch (e:Exception) { false }
                }
                .entries
                .flatMap { (key, value) -> OptionSpec.readsClangOpt(key, value).toList() }
                .toSet()

            return Spec(opts)
        }

        val clang11: Spec by lazy { reads(specJSON()) }
    }

}
