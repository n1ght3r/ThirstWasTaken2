import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.loaderOf
import com.thirstwastaken2.buildlogic.minecraftOf
import com.thirstwastaken2.buildlogic.parseWithoutOptional

plugins {
    id("dev.kikugie.stonecutter")
    // parseWithoutOptional, below. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
}

// The version the source tree is currently checked out for. Switch it with
// `./gradlew "Set active project to <name>"`; every version is built regardless by `chiseledBuild`.
stonecutter active "26.3.x"


stonecutter parameters {
    // Every node is `<version>` on Fabric or `<version>-<loader>` on another loader, `26.2.x-neoforge`.
    // Tagging both parts lets stonecutter.properties.toml keep what the loaders share in `["26.2.x"]` and
    // the rest in `[fabric."26.2.x"]` or `[neoforge."26.2.x"]`; see the top of that file.
    properties {
        tags(minecraftOf(current.project), loaderOf(current.project).id)
    }

    // Bakes the target Minecraft version into the jar, so the startup log line is never stale.
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        // 1.21.11 renamed ResourceLocation to Identifier, ResourceKey#location to #identifier, and
        // moved Util into net.minecraft.util, changing nothing else about any of them.
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
            replace(".identifier()", ".location()")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
        }

        // 26.1 renamed the HUD draw target while keeping the drawing methods identical but one, so
        // the whole difference is two names.
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            // The one: its item icon method lost its prefix in the same rename.
            replace(".fakeItem(", ".renderFakeItem(")
            // The Fabric data generation API renamed both of these for 26.1 without changing what
            // they do, so the datagen providers name the newer pair and get the older one here.
            replace("FabricPackOutput", "FabricDataOutput")
            replace("FabricTagsProvider", "FabricTagProvider")
        }

        // 26.2 split the advancement trigger classes out of `net.minecraft.advancements` into
        // `triggers` and `predicates`, keeping every class name. Only the datagen providers name
        // them, and only in imports, so each one is replaced whole rather than by package prefix:
        // `CriteriaTriggers` and `Criterion` did not move into `criterion` with the rest.
        // The package they came from was spelled `critereon` until 1.21.11. Replacements do not
        // chain, so the older spelling has to be chosen here rather than by a rule of its own.
        val criterion = if (current.parsed < "1.21.11") "critereon" else "criterion"
        string(current.parsed < "26.2") {
            replace("net.minecraft.advancements.triggers.CriteriaTriggers",
                    "net.minecraft.advancements.CriteriaTriggers")
            replace("net.minecraft.advancements.triggers.Criterion",
                    "net.minecraft.advancements.Criterion")
            replace("net.minecraft.advancements.triggers.InventoryChangeTrigger",
                    "net.minecraft.advancements.$criterion.InventoryChangeTrigger")
            replace("net.minecraft.advancements.triggers.ImpossibleTrigger",
                    "net.minecraft.advancements.$criterion.ImpossibleTrigger")
            replace("net.minecraft.advancements.triggers.PlayerTrigger",
                    "net.minecraft.advancements.$criterion.PlayerTrigger")
            replace("net.minecraft.advancements.triggers.RecipeCraftedTrigger",
                    "net.minecraft.advancements.$criterion.RecipeCraftedTrigger")
            replace("net.minecraft.advancements.triggers.RecipeUnlockedTrigger",
                    "net.minecraft.advancements.$criterion.RecipeUnlockedTrigger")
        }

        // 26.3 renamed every PushReaction constant and split LootPoolSingletonContainer into three
        // classes, of which UniformContainerBase is the one the entry builders are typed on. Neither
        // changed what the name means, and both keep the package they were in.
        string(current.parsed < "26.3") {
            replace("PushReaction.POPPED", "PushReaction.DESTROY")
            replace("UniformContainerBase", "LootPoolSingletonContainer")
            // 26.3 moved the renderer's pipeline type out of Blaze3D into Renderpearl, keeping the
            // class name. The dev HUD mixin names it twice: once as an import, once inside the
            // descriptor of the method it injects into.
            replace("com.mojang.renderpearl.api.pipeline.RenderPipeline", "com.mojang.blaze3d.pipeline.RenderPipeline")
            replace("Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;", "Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
        }

        // 1.21.2 renamed the server-side CONSUME result to SUCCESS_SERVER, both meaning "done, and the
        // client already swung", and Registry#get(ResourceKey) to getValue.
        string(current.parsed < "1.21.2") {
            replace("InteractionResult.SUCCESS_SERVER", "InteractionResult.CONSUME")
            replace("CREATIVE_MODE_TAB.getValue(", "CREATIVE_MODE_TAB.get(")
        }

        // 1.21.4 moved the data generator's model classes under net.minecraft.client, unchanged, and
        // Fabric API moved its model provider into its client package with them.
        string(current.parsed < "1.21.4") {
            replace("net.minecraft.client.data.models.", "net.minecraft.data.models.")
            replace("net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider",
                    "net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider")
        }

        // 1.21.5 renamed the Confusion effect to Nausea and Entity#moveTo to snapTo.
        string(current.parsed < "1.21.5") {
            replace("MobEffects.NAUSEA", "MobEffects.CONFUSION")
            replace(".snapTo(", ".moveTo(")
        }

        // The NeoForge node runs the same tests through a harness of its own, which reads a
        // @GameTest annotation from src/gametest/neoforge instead of Fabric API's. Only the import
        // changes, so a test keeps writing `@GameTest` with no arguments. See src/gametest/java/AGENTS.md.
        // That harness sets the template itself on every Minecraft version, so the rule below is for
        // Fabric only; two rules rewriting the same import would be ambiguous anyway.
        val neoForge = loaderOf(current.project) == Loader.NEOFORGE
        string(neoForge) {
            replace("import net.fabricmc.fabric.api.gametest.v1.GameTest;",
                    "import com.thirstwastaken2.gametest.neoforge.GameTest;")
        }

        // Fabric API's own @GameTest arrived with 1.21.5. Before it a test uses vanilla's annotation and
        // names Fabric's empty structure as its template; no test body changes.
        string(!neoForge && current.parsed < "1.21.5") {
            replace("import net.fabricmc.fabric.api.gametest.v1.GameTest;",
                    "import net.minecraft.gametest.framework.GameTest;")
            replace("@GameTest",
                    "@GameTest(template = net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE)")
        }
    }
}

// `-PwithoutOptional=<name,...>` leaves optional mods out of a node's dev clients (build.gradle.kts and
// build.neoforge.gradle.kts). Every node sees the flag, and most names only mean something on some
// nodes, Sophisticated Core on NeoForge for one, so a node quietly ignores a name it does not load. A
// name no node loads is a typo, and would leave the mod in the run while claiming to test without it.
gradle.projectsEvaluated {
    val asked = parseWithoutOptional(providers.gradleProperty("withoutOptional").orNull)
    if (asked.isEmpty()) return@projectsEvaluated
    val known = rootProject.subprojects.flatMap { node ->
        (node.extensions.extraProperties.properties["thirst.optionalRunMods"] as Set<*>?).orEmpty().map(Any?::toString)
    }.toSet()
    val unknown = asked - known
    if (unknown.isNotEmpty()) {
        throw GradleException("-PwithoutOptional names $unknown, which no node's runClient loads. " +
            "It takes: ${known.sorted().joinToString()}")
    }
}
