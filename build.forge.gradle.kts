import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.OptionalRunMods
import com.thirstwastaken2.buildlogic.flightRecorder
import com.thirstwastaken2.buildlogic.integrations
import com.thirstwastaken2.buildlogic.integrationsFor
import java.util.zip.ZipFile

plugins {
    // ModDevGradle's MinecraftForge flavour. Its version is on the plugin classpath from
    // settings.gradle.kts, which resolves it without applying it.
    id("net.neoforged.moddev.legacyforge")
    // The integration table and the rest of what the loader scripts share. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
    `maven-publish`
}

/*
 * The MinecraftForge nodes, `<version>-forge`: today only `1.20.1-forge`. The same sources as the Fabric
 * node of that version, arranged the way build.neoforge.gradle.kts arranges them, which this script
 * follows section by section. What differs is what Forge 1.20.1 does differently: the game runs under
 * SRG names outside development, so the jar that ships is the remapped `reobfJar`, mod dependencies are
 * remapped the other way through the `mod*` configurations, and the mixin configs are named in the jar
 * manifest and on the run command lines rather than in mods.toml. See docs/dev/VERSION-1.20.1.md.
 */

val modId = property("mod.id") as String
/** The dev tools' mod id; Forge ids cannot contain a hyphen either. */
val devModId = "thirstwastaken2_dev"
/** `-Pagent=<file>`: a file of agent requests to answer once, unattended. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val agentScript: String? = providers.gradleProperty("agent").orNull?.let { rootProject.file(it).absolutePath }

/** `-Pdriven`: this client is driven by an agent, not played. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val drivenClient: Boolean = providers.gradleProperty("driven").isPresent
/** Published artifact name; deliberately not the lowercase mod id. */
val modName = property("mod.name") as String
val modGroup = property("mod.group") as String
/** Minecraft range written into mods.toml, in Maven range syntax, e.g. `[1.20.1,1.20.2)`. */
val mcRange = property("mod.mc_compat") as String
val forgeVersion = property("deps.forge") as String

// The loader goes in the version so the jar never collides with the Fabric jar of the same Minecraft
// version in build/libs/.
version = "${property("mod.version")}+${sc.current.version}-forge"
base.archivesName = modName

/** Forge 47 runs on Java 17, as 1.20.1 does everywhere. */
val requiredJava: JavaVersion = JavaVersion.VERSION_17

repositories {
    // ModDevGradle supplies the Forge, Mojang and Minecraft libraries repositories.
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // The libraries Create bundles, which the Sand Filter compiles against; see createLibraries.
    exclusiveContent {
        forRepository { maven("https://maven.createmod.net") { name = "Create" } }
        filter { includeGroup("dev.engine-room.flywheel"); includeGroup("net.createmod.ponder") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.tterrag.com") { name = "tterrag" } }
        filter { includeGroup("com.tterrag.registrate") }
    }
}

/** See build.neoforge.gradle.kts: the loader this node builds for, by its source directory name. */
val loader = "forge"

/*
 * One source set, with the client sources compiled into `main`, for the reason and in the way
 * build.neoforge.gradle.kts gives: the `client` source set exists only so Stonecutter preprocesses
 * `src/client` for this node.
 */
sourceSets.create("client")
val clientSources: File =
    if (sc.current.isActive) rootProject.file("src/client")
    else layout.buildDirectory.dir("generated/stonecutter/client").get().asFile

sourceSets.main {
    java.srcDir("src/main/$loader/java")
    resources.srcDir("src/main/$loader/resources")
    // Forge 47 has one fluid API, IFluidHandler through capabilities.
    java.srcDir("src/main/$loader-fluidhandler/java")
    java.srcDir(files(clientSources.resolve("java"), clientSources.resolve("$loader/java"))
        .builtBy("stonecutterGenerateClient"))
    resources.srcDir(files(clientSources.resolve("resources"), clientSources.resolve("$loader/resources"))
        .builtBy("stonecutterGenerateClient"))
    // Written by the Fabric node's `runDatagen`, shared by both nodes of a Minecraft version.
    resources.srcDir(rootProject.file("src/main/generated/${sc.current.version}"))
}

/*
 * The optional integrations this node builds, from the same table build.neoforge.gradle.kts reads: each
 * row's source directories, its optional dependencies appended to the built mods.toml, and its mixin
 * config, which Forge 47 reads from the jar manifest rather than mods.toml (see `mixin` below). Forge 47
 * has one fluid API, so a row split by fluid API generation takes its `fluidhandler` half.
 */
val nodeIntegrations = integrationsFor(Loader.FORGE) { findProperty(it) != null }

/**
 * The libraries Create bundles in its jar (Ponder with Catnip, Flywheel, Registrate), as the Maven
 * coordinates its `META-INF/jarjar/metadata.json` names, so a new Create brings its own. They are in SRG
 * names like Create, and a remapping configuration takes only modules, so they are compiled against
 * from their own Mavens rather than copied out of the jar as on NeoForge. MixinExtras is left out: this
 * mod nests its own.
 */
val createLibraries: List<String> = (findProperty("deps.create") as String?)?.let { version ->
    val jar = configurations.detachedConfiguration(dependencies.create("maven.modrinth:create:$version"))
        .apply { isTransitive = false }.singleFile
    ZipFile(jar).use { zip ->
        val metadata = zip.getInputStream(zip.getEntry("META-INF/jarjar/metadata.json")).reader().readText()
        @Suppress("UNCHECKED_CAST")
        val jars = (groovy.json.JsonSlurper().parseText(metadata) as Map<String, Any?>)["jars"] as List<Map<String, Map<String, String>>>
        jars.map { "${it.getValue("identifier")["group"]}:${it.getValue("identifier")["artifact"]}:${it.getValue("version")["artifactVersion"]}" }
            .filterNot { it.startsWith("io.github.llamalad7:") }
    }
}.orEmpty()

sourceSets.main {
    nodeIntegrations.forEach { integration ->
        integration.mainRoots(transferApi = false).forEach { root ->
            java.srcDir("$root/java")
            resources.srcDir("$root/resources")
        }
        integration.clientJava?.let { dir ->
            java.srcDir(files(clientSources.resolve(dir)).builtBy("stonecutterGenerateClient"))
        }
    }
}

/*
 * The gametests and the dev tools, each its own small mod, as on NeoForge. `src/gametest/forge` and
 * `src/dev/forge` hold this loader's harness and entrypoints.
 */
val gametest: SourceSet = sourceSets.create("gametest") {
    java.srcDir("src/gametest/$loader/java")
    resources.srcDir("src/gametest/$loader/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

val dev: SourceSet = sourceSets.create("dev") {
    java.srcDir("src/dev/$loader/java")
    resources.srcDir("src/dev/$loader/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

/*
 * The `-javaagent` that stops an unattended `-Pagent` run when FML fails before any mod loads. Forge 47
 * writes its crash report the same way NeoForge's FML does. See build.neoforge.gradle.kts.
 */
val watchdog: SourceSet = sourceSets.create("watchdog") {
    java.srcDir("src/watchdog/java")
}
val watchdogJar = tasks.register<Jar>("watchdogJar") {
    archiveBaseName.set("thirstwastaken2-watchdog")
    destinationDirectory.set(layout.buildDirectory.dir("watchdog"))
    from(watchdog.output)
    manifest.attributes("Premain-Class" to "com.thirstwastaken2.dev.watchdog.LoadingFailureWatchdog")
}
if (agentScript != null) tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(watchdogJar) }

/*
 * The optional mods runClient loads, on a source set of their own, for the reason build.neoforge.gradle.kts
 * gives. They are Forge mods published under SRG names, so the configuration is remapped to Mojang's names
 * the way `modImplementation` is.
 */
val clientRunMods: Configuration = configurations.create("clientRunMods")
/** Where the mods are declared: `modClientRunMods`, whose remapped jars `clientRunMods` resolves to. */
val modClientRunMods: Configuration = obfuscation.createRemappingConfiguration(clientRunMods)

/** `-PwithoutOptional=<name,...>`; see build-logic's OptionalRunMods. */
val optionalRunMods = OptionalRunMods(providers.gradleProperty("withoutOptional").orNull)

/** Adds a mod to the clients' run only, unless `-PwithoutOptional` names it or one of the libraries it lists. */
fun runClientMod(names: List<String>, notation: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    if (optionalRunMods.include(names)) dependencies.add(modClientRunMods.name, notation, configure)
}
val clientRun: SourceSet = sourceSets.create("clientRun") {
    runtimeClasspath += dev.output + sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath +
        clientRunMods
}
val serverRun: SourceSet = sourceSets.create("serverRun") {
    runtimeClasspath += dev.output + sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

legacyForge {
    version = "${sc.current.version}-$forgeVersion"
    // Opens what Fabric API's transitive access wideners open on the Fabric node. The same file ships in
    // the jar, where Forge applies it for players.
    accessTransformers.from(rootProject.file("src/main/$loader/resources/META-INF/accesstransformer.cfg"))

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
        create("thirstwastaken2_gametest") {
            sourceSet(gametest)
        }
        create(devModId) {
            sourceSet(dev)
        }
    }

    runs {
        val node = project.name
        configureEach {
            agentScript?.let {
                systemProperty("thirstwastaken2.agent.script", it)
                systemProperty("thirstwastaken2.agent.script.exit", "true")
                jvmArguments.add(watchdogJar.flatMap { jar -> jar.archiveFile }.map { "-javaagent:${it.asFile.absolutePath}" })
            }
            if (drivenClient) systemProperty("thirstwastaken2.agent.driven", "true")

            // One run directory per node, and one per extra client; see build.neoforge.gradle.kts.
            gameDirectory.set(rootProject.file(when (name) {
                "gametest" -> "run/$node/gametest"
                "manualA", "manualB" -> "run/manual-$node-${name.last()}"
                else -> "run/$node"
            }))
        }

        create("client") {
            client()
            sourceSet = clientRun
            providers.gradleProperty("quickplay").orNull?.let { world ->
                programArguments.addAll("--quickPlaySingleplayer", world)
                systemProperty("thirstwastaken2.agent.quickplay", world)
            }
        }
        create("server") {
            server()
            sourceSet = serverRun
        }
        create("benchmark") {
            server()
            sourceSet = serverRun
            systemProperty("thirstwastaken2.benchmark",
                providers.gradleProperty("benchmark").getOrElse("standard"))
            systemProperty("thirstwastaken2.benchmark.exit", "true")
            programArguments.addAll("--world", providers.gradleProperty("thirst.benchmark.world").get())
            if (providers.gradleProperty("profile").isPresent) {
                jvmArguments.addAll(flightRecorder(rootProject.file("run/$node/benchmark/latest.jfr")))
            }
        }
        listOf("A", "B").forEach { tester ->
            create("manual$tester") {
                client()
                sourceSet = clientRun
                programArguments.addAll(
                    "--username", "Tester$tester",
                    "--width", "1100", "--height", "700",
                    "--quickPlayMultiplayer", "localhost:25565",
                )
                systemProperty("thirstwastaken2.agent", tester)
            }
        }
        // The same contract as on the other loaders: a headless server that runs every registered test
        // and exits with the number of failed required tests. 1.20.1 has no --report option, so the
        // harness installs the JUnit reporter itself.
        create("gametest") {
            type = "gameTestServer"
            sourceSet = gametest
            systemProperty("thirstwastaken2.gametest.report",
                layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath)
        }

        val mainMod = mods.named(modId)
        val devMod = mods.named(devModId)
        val gametestMod = mods.named("thirstwastaken2_gametest")
        listOf("client", "server", "benchmark", "manualA", "manualB").forEach { run ->
            named(run) {
                loadedMods.set(mainMod.zip(devMod) { main, agent -> setOf(main, agent) })
                // The dev tools' mixins, only where the dev tools are loaded. The mod's own configs reach
                // every run through `mixin` below.
                programArguments.addAll("--mixin.config", "thirstwastaken2.dev.mixins.json")
            }
        }
        named("gametest") { loadedMods.set(mainMod.zip(gametestMod) { main, tests -> setOf(main, tests) }) }
    }
}

/*
 * The mixin configs: named in the jar manifest for players and passed to every run in development. The
 * refmap is what maps each injection's Mojang names to the SRG names the game runs under outside
 * development; the annotation processor writes it while compiling `main`.
 */
val refmap = "$modId.refmap.json"

/**
 * What a mixin config needs on Forge and the shared source configs leave out, added as each is copied: the
 * refmap's name, which Loom writes in on Fabric, and the `minVersion` Forge's Mixin logs an error without.
 * The shared configs stay the same on every node.
 */
fun forgeMixinConfig(line: String, withRefmap: Boolean): String {
    if (!line.trimStart().startsWith("\"package\":")) return line
    val indent = line.substringBefore("\"package\":")
    val added = listOfNotNull(if (withRefmap) "\"refmap\": \"$refmap\"," else null, "\"minVersion\": \"0.8\",")
    return added.joinToString("") { "$indent$it\n" } + line
}

// A mixin that names its target by string does so because the mod is optional and nothing compiles
// against it (Fruits Delight and the L2 Library nested in it). Forge's annotation processor, unlike Loom's,
// fails the build when such a target is not on the classpath; a warning is what Fabric gives.
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-AMSG_MIXIN_SOFT_TARGET_NOT_FOUND=warning")
}

mixin {
    add(sourceSets.main.get(), refmap)
    config("$modId.mixins.json")
    config("$modId.client.mixins.json")
    nodeIntegrations.mapNotNull { it.mixinConfig }.forEach { config(it) }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // MixinExtras, which the core mixins use. Fabric Loader and NeoForge ship it; Forge 47 does not, so
    // the jar carries it nested, and Forge loads the newest copy any mod nests.
    val mixinExtras = property("deps.mixinextras") as String
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtras")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:$mixinExtras")!!)

    // Optional integrations the client code compiles against; see build.neoforge.gradle.kts. Mod Menu is
    // Fabric only, and Forge's own config screen factory is used instead.
    modCompileOnly("maven.modrinth:appleskin:${property("deps.appleskin")}")
    modCompileOnly("maven.modrinth:jade:${property("deps.jade")}")

    runClientMod(listOf("appleskin"), "maven.modrinth:appleskin:${property("deps.appleskin")}")
    runClientMod(listOf("jade"), "maven.modrinth:jade:${property("deps.jade")}")
    findProperty("deps.cloth_config")?.let {
        runClientMod(listOf("cloth-config", "cloth_config"), "maven.modrinth:cloth-config:$it")
    }
    findProperty("deps.create")?.let { create ->
        // Create and the libraries it bundles (Ponder with Catnip, Flywheel, Registrate) are all in SRG
        // names, so all are remapped; the Sand Filter extends classes whose supertypes live in them. At
        // runtime FML reads the libraries out of Create's jar itself. See src/main/createforge/AGENTS.md.
        modCompileOnly("maven.modrinth:create:$create") { isTransitive = false }
        createLibraries.forEach { modCompileOnly(it) { isTransitive = false } }
        runClientMod(listOf("create"), "maven.modrinth:create:$create") { isTransitive = false }
    }
    findProperty("deps.serene_seasons")?.let { sereneSeasons ->
        val glitchCore = property("deps.glitchcore").toString()
        // Read through its API alone, but its Forge jar is in SRG names, so it is remapped like the other
        // mods. GlitchCore is compiled against too: the dimension check reads Serene Seasons' config,
        // whose class extends GlitchCore's. Forge ships the Night Config under that.
        modCompileOnly("maven.modrinth:serene-seasons:$sereneSeasons") { isTransitive = false }
        modCompileOnly("maven.modrinth:glitchcore:$glitchCore") { isTransitive = false }
        // Off in runClient, for the reason build.gradle.kts gives. Uncomment the two lines below only to
        // work on the Serene Seasons integration.
        val names = listOf("serene-seasons", "sereneseasons")
        // runClientMod(names, "maven.modrinth:serene-seasons:$sereneSeasons") { isTransitive = false }
        // runClientMod(names + listOf("glitchcore"), "maven.modrinth:glitchcore:$glitchCore") { isTransitive = false }
        // Keeps `-PwithoutOptional=serene-seasons` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(names + listOf("glitchcore"))
    }
    findProperty("deps.kaleidoscope_cookery")?.let { kaleidoscopeCookery ->
        // Mixed into, and in SRG names, so remapped. See src/main/kaleidoscope/AGENTS.md.
        modCompileOnly("maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookery") { isTransitive = false }
        runClientMod(listOf("kaleidoscope-cookery", "kaleidoscope-cookery-refabricated", "kaleidoscope_cookery"),
            "maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookery") { isTransitive = false }
    }
    // Fruits Delight: nothing compiles against it, its mixins name their targets by string. Only here to
    // test them. Its L2 libraries are nested in its jar, which Forge loads itself.
    findProperty("deps.fruits_delight")?.let {
        runClientMod(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"),
            "maven.modrinth:fruits-delight:$it") { isTransitive = false }
    }
    // Test the drinks and meals Farmer's Delight adds, and the Cooking Pot recipes. Reached by id only.
    findProperty("deps.farmersdelight")?.let {
        runClientMod(listOf("farmers-delight", "farmersdelight"), "maven.modrinth:farmers-delight:$it")
    }

    project.extra["thirst.optionalRunMods"] = optionalRunMods.offered
}

/*
 * Datagen runs on Fabric only; this node rewrites the Fabric spellings in the generated JSON as it copies
 * it, the same arrangement as build.neoforge.gradle.kts, into Forge 47's own:
 *
 * | Fabric                                                   | Forge 47                                          |
 * |----------------------------------------------------------|---------------------------------------------------|
 * | `fabric:type` `fabric:nbt`, `base`, `nbt`, `strict: false` | `type` `forge:partial_nbt`, `item`, `nbt`       |
 * | `fabric:type` `fabric:any`, `ingredients`                | the ingredients as a plain array, Forge's compound |
 * | `fabric:load_conditions`, `fabric:all_mods_loaded`       | `conditions`, one `forge:mod_loaded` per mod      |
 * | `condition` `thirstwastaken2:item_enabled`, `item`       | `type` `thirstwastaken2:item_enabled`, `item`     |
 *
 * Both NBT ingredients match a stack whose tag contains the listed one. Forge reads `conditions` on
 * recipes and advancements, which are the only files datagen gates. Anything else Fabric-specific fails
 * the build here, naming the file, and `checkForgeResources` catches what this does not look at.
 */
fun forgeJson(node: Any?, file: String): Any? = when (node) {
    is List<*> -> node.map { forgeJson(it, file) }
    is Map<*, *> -> when (val type = node["fabric:type"]) {
        null -> node.entries.associate { (key, value) ->
            if (key == "fabric:load_conditions") "conditions" to forgeConditions(value, file)
            else key as String to forgeJson(value, file)
        }
        "fabric:nbt" -> {
            val unknown = node.keys - setOf("fabric:type", "base", "nbt", "strict")
            if (unknown.isNotEmpty()) throw GradleException("$file: no Forge translation for $unknown in fabric:nbt")
            if (node["strict"] == true) throw GradleException("$file: no Forge translation for a strict fabric:nbt")
            val base = node["base"]
            if (base !is Map<*, *> || base.keys != setOf("item")) {
                throw GradleException("$file: no Forge translation for the fabric:nbt base $base")
            }
            mapOf("type" to "forge:partial_nbt", "item" to base["item"], "nbt" to node["nbt"])
        }
        "fabric:any" -> {
            val unknown = node.keys - setOf("fabric:type", "ingredients")
            if (unknown.isNotEmpty()) throw GradleException("$file: no Forge translation for $unknown in fabric:any")
            forgeJson(node["ingredients"], file)
        }
        else -> throw GradleException("$file: no Forge translation for the Fabric ingredient type $type")
    }
    else -> node
}

fun forgeConditions(conditions: Any?, file: String): List<Map<String, Any?>> =
    (conditions as List<*>).flatMap { condition ->
        condition as Map<*, *>
        when (condition["condition"]) {
            "fabric:all_mods_loaded" ->
                (condition["values"] as List<*>).map { mapOf("type" to "forge:mod_loaded", "modid" to it) }
            "thirstwastaken2:item_enabled" -> {
                if (condition.keys != setOf("condition", "item")) {
                    throw GradleException("$file: no Forge translation for ${condition.keys} in thirstwastaken2:item_enabled")
                }
                listOf(mapOf("type" to "thirstwastaken2:item_enabled", "item" to condition["item"]))
            }
            else -> throw GradleException("$file: no Forge translation for the Fabric load condition ${condition["condition"]}")
        }
    }

val generatedResources = rootProject.file("src/main/generated/${sc.current.version}")

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to mcRange,
    )
    inputs.properties(props)
    inputs.property("refmap", refmap)
    filesMatching("META-INF/mods.toml") { expand(props) }
    // See forgeMixinConfig.
    filesMatching("*.mixins.json") {
        expand("java" to "JAVA_${requiredJava.majorVersion}")
        filter { line -> forgeMixinConfig(line, withRefmap = true) }
    }
    exclude("**/.cache/**")

    // An integration's mods are optional dependencies in the built manifest, only on a node that builds
    // it, as on NeoForge. One input per integration, so a change of its version reruns this.
    integrations.filter { Loader.FORGE in it.loaders }.forEach { integration ->
        inputs.property(integration.dir, findProperty(integration.depsKey)?.toString() ?: "")
    }
    if (nodeIntegrations.isNotEmpty()) {
        val manifest = destinationDir.resolve("META-INF/mods.toml")
        val appended = nodeIntegrations.joinToString("") { it.forgeManifest(modId) }
        doLast { manifest.appendText(appended) }
    }

    val output = destinationDir
    doLast {
        generatedResources.walk().filter { it.isFile && it.extension == "json" }.forEach { source ->
            val relative = source.relativeTo(generatedResources).invariantSeparatorsPath
            val target = output.resolve(relative)
            if (!target.isFile) return@forEach
            val text = target.readText()
            if (!text.contains("\"fabric:")) return@forEach
            val translated = forgeJson(groovy.json.JsonSlurper().parseText(text), relative)
            target.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(translated)))
        }
    }
}

tasks.named<ProcessResources>("processDevResources") {
    inputs.property("java", requiredJava.majorVersion)
    filesMatching("*.mixins.json") {
        expand("java" to "JAVA_${requiredJava.majorVersion}")
        filter { line -> forgeMixinConfig(line, withRefmap = false) }
    }
}

// 1.21 renamed the data pack directories from plural to singular; the gametests' own data is written the
// newer way and renamed here, as build.gradle.kts does for the Fabric node.
tasks.named<ProcessResources>("processGametestResources") {
    eachFile { path = path.replace("/tags/item/", "/tags/items/") }
    includeEmptyDirs = false
}

/**
 * Fails when a Fabric key survives in the processed resources, the counterpart of
 * `checkNeoForgeResources`. CI runs it on the Forge job.
 */
tasks.register("checkForgeResources") {
    group = "verification"
    description = "Fails when a Fabric-only JSON key survives into the Forge resources"

    val processed = tasks.processResources.map { it.destinationDir }
    inputs.dir(processed)
    val fabricKey = Regex(""""fabric:[^"]*"\s*:""")

    doLast {
        val root = processed.get()
        val offenders = root.walk().filter { it.isFile && it.extension == "json" }.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> fabricKey.containsMatchIn(line) }
                .map { (index, line) -> "${file.relativeTo(root).invariantSeparatorsPath}:${index + 1}: ${line.trim()}" }
        }.toList()
        check(offenders.isEmpty()) {
            "Fabric keys in the Forge resources. Translate the shape in build.forge.gradle.kts " +
                "or stop writing it:\n" + offenders.joinToString("\n")
        }
    }
}

tasks.named("processResources") { dependsOn("stonecutterGenerate") }

/** One node at a time decompiles Minecraft; see build.neoforge.gradle.kts. */
abstract class CreateMinecraftArtifactsMutex : BuildService<BuildServiceParameters.None>
val createMinecraftArtifactsMutex = gradle.sharedServices
    .registerIfAbsent("createMinecraftArtifactsMutex", CreateMinecraftArtifactsMutex::class) { maxParallelUsages = 1 }

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
    usesService(createMinecraftArtifactsMutex)
}

tasks.named("runGametest") {
    val reportDir = layout.buildDirectory.dir("gametest")
    doFirst { reportDir.get().asFile.mkdirs() }
}

extra["thirst.requiredJava"] = requiredJava.majorVersion
extra["thirst.integrations"] = integrations.map { it.dir }
extra["thirst.loaderIndependentIntegrations"] = integrations.filter { it.loaderIndependent }.map { it.dir }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.jar {
    // Forge 47 finds a mod's mixin configs here and nowhere else in a jar; `mixin` above only reaches
    // the development runs.
    manifest.attributes("MixinConfigs" to mixin.configs.map { it.joinToString(",") })
    from(files(rootProject.file("LICENSE"), rootProject.file("CREDITS.md"),
            rootProject.file("licenses/ThirstWasTaken-MIT.txt"))) {
        rename { name -> name.substringBeforeLast('.') + "_$modId" + name.removePrefix(name.substringBeforeLast('.')) }
    }
}

// What this node hands `buildAndCollect`: the jar remapped to SRG names, which is the one players run.
// The plain `jar` keeps Mojang's names and only loads in development.
tasks.named<Copy>("buildAndCollect") {
    from(tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = modGroup
            from(components["java"])
        }
    }
    repositories { }
}
