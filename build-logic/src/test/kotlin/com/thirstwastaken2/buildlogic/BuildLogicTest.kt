package com.thirstwastaken2.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildLogicTest {
    private fun integration(dir: String) = integrations.single { it.dir == dir }

    @Test
    fun withoutOptionalIsTrimmedAndLowercased() {
        assertEquals(setOf("jade", "appleskin"), parseWithoutOptional(" Jade, ,APPLESKIN "))
        assertEquals(emptySet(), parseWithoutOptional(null))
    }

    @Test
    fun optionalRunModsLeavesOutWhatIsAskedAndRecordsEveryName() {
        val mods = OptionalRunMods("moonlight")
        assertTrue(mods.include(listOf("jade")))
        assertFalse(mods.include(listOf("supplementaries", "moonlight")))
        assertEquals(setOf("all", "jade", "supplementaries", "moonlight"), mods.offered)
        assertFalse(OptionalRunMods("all").include(listOf("jade")))
    }

    @Test
    fun aNodeNamesItsLoaderAfterItsMinecraftVersion() {
        assertEquals(Loader.FABRIC, loaderOf("26.2.x"))
        assertEquals(Loader.FABRIC, loaderOf("1.21.1"))
        assertEquals(Loader.NEOFORGE, loaderOf("26.2.x-neoforge"))
        assertEquals(Loader.FORGE, loaderOf("1.20.1-forge"))
        assertEquals("26.2.x", minecraftOf("26.2.x-neoforge"))
        assertEquals("1.20.1", minecraftOf("1.20.1-forge"))
        assertEquals("1.21.11", minecraftOf("1.21.11"))
    }

    @Test
    fun dirsAreUnique() {
        assertEquals(integrations.size, integrations.map { it.dir }.toSet().size)
    }

    @Test
    fun onlyTheFluidApiSplitAddsAGenerationDirectory() {
        assertEquals(listOf("src/main/sophisticated", "src/main/sophisticated-transfer"),
            integration("sophisticated").mainRoots(transferApi = true))
        assertEquals(listOf("src/main/sophisticated", "src/main/sophisticated-fluidhandler"),
            integration("sophisticated").mainRoots(transferApi = false))
        assertEquals(listOf("src/main/create"), integration("create").mainRoots(transferApi = true))
    }

    @Test
    fun nodesGetOnlyWhatTheirLoaderCompiles() {
        val everything: (String) -> Boolean = { true }
        assertEquals(listOf("createfly", "supplementaries", "kaleidoscope", "brewinandchewin", "sereneseasons"),
            integrationsFor(Loader.FABRIC, everything).map { it.dir })
        assertEquals(listOf("create", "sophisticated", "supplementaries", "kaleidoscope", "brewinandchewin", "coldsweat", "culturaldelights", "fruitsdelight", "expandeddelight", "sereneseasons"),
            integrationsFor(Loader.NEOFORGE, everything).map { it.dir })
        assertEquals(listOf("supplementaries", "kaleidoscope", "brewinandchewin", "sereneseasons"), integrations.filter { it.loaderIndependent }.map { it.dir })
    }

    @Test
    fun neoForgeManifestNamesTheMixinConfigThenEachDependency() {
        assertEquals("""
            |
            |[[mixins]]
            |config = "thirstwastaken2.supplementaries.mixins.json"
            |
            |[[dependencies.thirstwastaken2]]
            |modId = "supplementaries"
            |type = "optional"
            |ordering = "NONE"
            |side = "BOTH"
            |
            |[[dependencies.thirstwastaken2]]
            |modId = "moonlight"
            |type = "optional"
            |ordering = "NONE"
            |side = "BOTH"
            |""".trimMargin(), integration("supplementaries").neoForgeManifest("thirstwastaken2"))
    }

    @Test
    fun anIntegrationWithoutMixinsNamesOnlyItsDependency() {
        assertEquals("""
            |
            |[[dependencies.thirstwastaken2]]
            |modId = "sereneseasons"
            |type = "optional"
            |ordering = "NONE"
            |side = "BOTH"
            |""".trimMargin(), integration("sereneseasons").neoForgeManifest("thirstwastaken2"))
    }

    @Test
    fun fabricManifestKeepsCreateFlyAtIndexOneAndAppendsTheRest() {
        val json = mutableMapOf<String, Any?>(
            "mixins" to mutableListOf<Any?>("thirstwastaken2.mixins.json", "thirstwastaken2.client.mixins.json"),
            "entrypoints" to mutableMapOf<String, Any?>("jade" to mutableListOf<Any?>("a.Jade")),
        )
        integrationsFor(Loader.FABRIC) { true }.forEach { it.patchFabricManifest(json) }
        assertEquals(listOf("thirstwastaken2.mixins.json", "thirstwastaken2.createfly.mixins.json",
            "thirstwastaken2.client.mixins.json", "thirstwastaken2.supplementaries.mixins.json",
            "thirstwastaken2.kaleidoscope.mixins.json", "thirstwastaken2.brewinandchewin.mixins.json"), json["mixins"])
        @Suppress("UNCHECKED_CAST")
        val entrypoints = json["entrypoints"] as Map<String, Any?>
        assertEquals(listOf("a.Jade", "com.thirstwastaken2.client.supplementaries.SupplementariesJade",
            "com.thirstwastaken2.client.kaleidoscope.KaleidoscopeJade",
            "com.thirstwastaken2.client.brewinandchewin.BrewinAndChewinJade"), entrypoints["jade"])
        assertEquals(listOf("com.thirstwastaken2.sereneseasons.SereneSeasonsEntrypoint"), entrypoints["thirstwastaken2:integration"])
        assertEquals(listOf("jade", "thirstwastaken2:createfly", "thirstwastaken2:createfly_client", "thirstwastaken2:integration"),
            entrypoints.keys.toList())
    }
}
