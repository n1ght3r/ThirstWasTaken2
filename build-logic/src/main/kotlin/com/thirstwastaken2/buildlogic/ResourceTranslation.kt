package com.thirstwastaken2.buildlogic

import org.gradle.api.GradleException

/*
 * Datagen runs on Fabric only, so the recipes and advancements it writes use Fabric's spellings for
 * things NeoForge and Forge 47 also have, under other names. The two ModDevGradle nodes rewrite them as
 * they copy the files (gradle/moddev.gradle.kts walks them), rather than datagen writing a second copy or
 * the loader registering Fabric's names: an alias cannot work, because NeoForge reads the ingredient type
 * from a different key altogether. These functions take and return the parsed JSON, maps and lists.
 *
 * | Fabric                                                     | NeoForge                                                                | Forge 47                                           |
 * |------------------------------------------------------------|-------------------------------------------------------------------------|----------------------------------------------------|
 * | `fabric:type` `fabric:components`, `base`, `components`    | `neoforge:ingredient_type` `neoforge:components`, `items`, `components` | none: 1.20.1 has no components                     |
 * | `fabric:type` `fabric:nbt`, `base`, `nbt`, `strict: false` | none: no NeoForge node is older than components                         | `type` `forge:partial_nbt`, `item`, `nbt`          |
 * | `fabric:type` `fabric:any`, `ingredients`                  | `neoforge:ingredient_type` `neoforge:compound`, `children`              | the ingredients as a plain array, Forge's compound |
 * | `fabric:load_conditions`, `fabric:all_mods_loaded`         | `neoforge:conditions`, one `neoforge:mod_loaded` per mod                | `conditions`, one `forge:mod_loaded` per mod       |
 * | `condition` `thirstwastaken2:item_enabled`, `item`         | `type` `thirstwastaken2:item_enabled`, `item`                           | the same as NeoForge                               |
 *
 * Both components ingredients take a `DataComponentPatch` and match a stack that carries at least the
 * listed values, which is NeoForge's default `strict: false`, so `strict` is left out. Both NBT
 * ingredients match a stack whose tag contains the listed one. Forge reads `conditions` on recipes and
 * advancements, which are the only files datagen gates.
 *
 * On 1.21.1 NeoForge 21.1 reads the ingredient type from vanilla's own `type` key, and Fabric writes
 * `base` as a whole ingredient, `{"item": ...}`, where NeoForge's `items` is a holder set, so the item
 * id is taken out of it. Later versions write `base` as the holder set already.
 *
 * Anything else Fabric-specific fails the build here, naming the file, and `checkNeoForgeResources` or
 * `checkForgeResources` catches whatever this does not look at. A generator that starts writing a new
 * Fabric shape therefore breaks the build rather than loading as a broken recipe.
 */

/**
 * [node] in NeoForge's spellings. [ingredientTypeKey] is the key NeoForge reads a custom ingredient's
 * type from: vanilla's `type` on 1.21.1, `neoforge:ingredient_type` after it.
 */
fun neoForgeJson(node: Any?, file: String, ingredientTypeKey: String): Any? = when (node) {
    is List<*> -> node.map { neoForgeJson(it, file, ingredientTypeKey) }
    is Map<*, *> -> when (val type = node["fabric:type"]) {
        null -> node.entries.associate { (key, value) ->
            if (key == "fabric:load_conditions") "neoforge:conditions" to loadConditions(value, file, "NeoForge", "neoforge")
            else key as String to neoForgeJson(value, file, ingredientTypeKey)
        }
        "fabric:components" -> {
            requireKeys(node, setOf("fabric:type", "base", "components"), file, "NeoForge")
            mapOf(ingredientTypeKey to "neoforge:components",
                "items" to neoForgeItems(node["base"], file), "components" to node["components"])
        }
        "fabric:any" -> {
            requireKeys(node, setOf("fabric:type", "ingredients"), file, "NeoForge")
            mapOf(ingredientTypeKey to "neoforge:compound",
                "children" to neoForgeJson(node["ingredients"], file, ingredientTypeKey))
        }
        else -> throw GradleException("$file: no NeoForge translation for the Fabric ingredient type $type")
    }
    else -> node
}

/** [node] in Forge 47's spellings. */
fun forgeJson(node: Any?, file: String): Any? = when (node) {
    is List<*> -> node.map { forgeJson(it, file) }
    is Map<*, *> -> when (val type = node["fabric:type"]) {
        null -> node.entries.associate { (key, value) ->
            if (key == "fabric:load_conditions") "conditions" to loadConditions(value, file, "Forge", "forge")
            else key as String to forgeJson(value, file)
        }
        "fabric:nbt" -> {
            requireKeys(node, setOf("fabric:type", "base", "nbt", "strict"), file, "Forge")
            if (node["strict"] == true) throw GradleException("$file: no Forge translation for a strict fabric:nbt")
            val base = node["base"]
            if (base !is Map<*, *> || base.keys != setOf("item")) {
                throw GradleException("$file: no Forge translation for the fabric:nbt base $base")
            }
            mapOf("type" to "forge:partial_nbt", "item" to base["item"], "nbt" to node["nbt"])
        }
        "fabric:any" -> {
            requireKeys(node, setOf("fabric:type", "ingredients"), file, "Forge")
            forgeJson(node["ingredients"], file)
        }
        else -> throw GradleException("$file: no Forge translation for the Fabric ingredient type $type")
    }
    else -> node
}

/**
 * A components ingredient's `base` as the holder set NeoForge's `items` reads: an id, a `#tag` or a list
 * of ids. It already is one from 1.21.2; on 1.21.1 it is a vanilla ingredient, a single item or tag.
 */
private fun neoForgeItems(base: Any?, file: String): Any? = when {
    base is String -> base
    base is List<*> && base.all { it is String } -> base
    base is Map<*, *> && base.keys == setOf("item") -> base["item"]
    base is Map<*, *> && base.keys == setOf("tag") -> "#${base["tag"]}"
    else -> throw GradleException("$file: no NeoForge translation for the components ingredient base $base")
}

/** Fabric's load conditions as [namespace]`:mod_loaded` conditions; the two loaders differ only in the namespace. */
private fun loadConditions(conditions: Any?, file: String, loader: String, namespace: String): List<Map<String, Any?>> =
    (conditions as List<*>).flatMap { condition ->
        condition as Map<*, *>
        when (condition["condition"]) {
            "fabric:all_mods_loaded" ->
                (condition["values"] as List<*>).map { mapOf("type" to "$namespace:mod_loaded", "modid" to it) }
            // The mod's own condition, registered under the same id on every loader; only its key moves.
            "thirstwastaken2:item_enabled" -> {
                if (condition.keys != setOf("condition", "item")) {
                    throw GradleException("$file: no $loader translation for ${condition.keys} in thirstwastaken2:item_enabled")
                }
                listOf(mapOf("type" to "thirstwastaken2:item_enabled", "item" to condition["item"]))
            }
            else -> throw GradleException("$file: no $loader translation for the Fabric load condition ${condition["condition"]}")
        }
    }

private fun requireKeys(node: Map<*, *>, keys: Set<String>, file: String, loader: String) {
    val unknown = node.keys - keys
    if (unknown.isNotEmpty()) throw GradleException("$file: no $loader translation for $unknown in ${node["fabric:type"]}")
}

/**
 * One line of a mixin config as Forge 47 needs it, which the shared source configs leave out: before the
 * `"package"` line, the refmap's name, which Loom writes in on Fabric, and the `minVersion` Forge's Mixin
 * logs an error without. [refmap] is null for a config whose mixins are not remapped (the dev tools').
 */
fun forgeMixinConfigLine(line: String, refmap: String?): String {
    if (!line.trimStart().startsWith("\"package\":")) return line
    val indent = line.substringBefore("\"package\":")
    val added = listOfNotNull(refmap?.let { "\"refmap\": \"$it\"," }, "\"minVersion\": \"0.8\",")
    return added.joinToString("") { "$indent$it\n" } + line
}
