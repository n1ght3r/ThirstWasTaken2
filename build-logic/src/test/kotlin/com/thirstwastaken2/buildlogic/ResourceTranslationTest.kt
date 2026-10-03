package com.thirstwastaken2.buildlogic

import org.gradle.api.GradleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceTranslationTest {
    private val conditions = mapOf("fabric:load_conditions" to listOf(
        mapOf("condition" to "fabric:all_mods_loaded", "values" to listOf("farmersdelight")),
        mapOf("condition" to "thirstwastaken2:item_enabled", "item" to "thirstwastaken2:waterskin"),
    ))

    @Test
    fun neoForgeReadsComponentsAndCompoundsUnderItsOwnKeys() {
        val any = mapOf("fabric:type" to "fabric:any", "ingredients" to listOf(
            mapOf("fabric:type" to "fabric:components", "base" to mapOf("item" to "minecraft:potion"),
                "components" to mapOf("thirstwastaken2:purity" to 3)),
            mapOf("item" to "minecraft:bowl"),
        ))
        assertEquals(mapOf("type" to "neoforge:compound", "children" to listOf(
            mapOf("type" to "neoforge:components", "items" to "minecraft:potion",
                "components" to mapOf("thirstwastaken2:purity" to 3)),
            mapOf("item" to "minecraft:bowl"),
        )), neoForgeJson(any, "a.json", "type"))
        assertEquals("#c:bottles", (neoForgeJson(mapOf("fabric:type" to "fabric:components",
            "base" to mapOf("tag" to "c:bottles"), "components" to emptyMap<String, Any?>()),
            "a.json", "neoforge:ingredient_type") as Map<*, *>)["items"])
    }

    @Test
    fun loadConditionsBecomeEachLoadersModLoaded() {
        assertEquals(mapOf("neoforge:conditions" to listOf(
            mapOf("type" to "neoforge:mod_loaded", "modid" to "farmersdelight"),
            mapOf("type" to "thirstwastaken2:item_enabled", "item" to "thirstwastaken2:waterskin"),
        )), neoForgeJson(conditions, "a.json", "type"))
        assertEquals(mapOf("conditions" to listOf(
            mapOf("type" to "forge:mod_loaded", "modid" to "farmersdelight"),
            mapOf("type" to "thirstwastaken2:item_enabled", "item" to "thirstwastaken2:waterskin"),
        )), forgeJson(conditions, "a.json"))
    }

    @Test
    fun forgeReadsNbtAsPartialNbtAndAnyAsAPlainArray() {
        val nbt = mapOf("fabric:type" to "fabric:nbt", "base" to mapOf("item" to "minecraft:potion"),
            "nbt" to "{purity:3}", "strict" to false)
        assertEquals(listOf(mapOf("type" to "forge:partial_nbt", "item" to "minecraft:potion", "nbt" to "{purity:3}")),
            forgeJson(mapOf("fabric:type" to "fabric:any", "ingredients" to listOf(nbt)), "a.json"))
        assertFailsWith<GradleException> { forgeJson(nbt + ("strict" to true), "a.json") }
    }

    @Test
    fun anUnknownFabricShapeFailsTheBuild() {
        val unknown = mapOf("fabric:type" to "fabric:custom_data")
        assertFailsWith<GradleException> { neoForgeJson(unknown, "a.json", "type") }
        assertFailsWith<GradleException> { forgeJson(unknown, "a.json") }
        assertFailsWith<GradleException> {
            forgeJson(mapOf("fabric:load_conditions" to listOf(mapOf("condition" to "fabric:not"))), "a.json")
        }
    }

    @Test
    fun forgeMixinConfigsGainTheirRefmapBeforeThePackage() {
        assertEquals("  \"refmap\": \"r.json\",\n  \"minVersion\": \"0.8\",\n  \"package\": \"a\",",
            forgeMixinConfigLine("  \"package\": \"a\",", "r.json"))
        assertEquals("  \"minVersion\": \"0.8\",\n  \"package\": \"a\",", forgeMixinConfigLine("  \"package\": \"a\",", null))
        assertEquals("  \"required\": true,", forgeMixinConfigLine("  \"required\": true,", "r.json"))
    }
}
