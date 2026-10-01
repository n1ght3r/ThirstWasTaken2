import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.OptionalRunMods
import com.thirstwastaken2.buildlogic.flightRecorder
import com.thirstwastaken2.buildlogic.integrations
import com.thirstwastaken2.buildlogic.integrationsFor

plugins {
    // NeoForge's build plugin. Its version is on the plugin classpath from settings.gradle.kts,
    // which resolves it without applying it.
    id("net.neoforged.moddev")
    // The integration table and the rest of what both loader scripts share. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
    `maven-publish`
}

/*
 * The NeoForge nodes, `<version>-neoforge`. Each is one Minecraft version and one loader, with the same
 * sources as the Fabric node of that version. Everything both this and the Fabric nodes need is in
 * gradle/shared.gradle.kts, applied below. See src/main/java/com/thirstwastaken2/platform/AGENTS.md.
 */

// Stonecutter supplies `mod.*` and `deps.*` for this node from stonecutter.properties.toml.
// `group` stays unset for the same reason build.gradle.kts leaves it unset; the publication sets
// its own groupId below.
val modId = property("mod.id") as String
/** The dev tools' mod id. NeoForge ids cannot contain a hyphen, so it is not the Fabric `-dev` spelling. */
val devModId = "thirstwastaken2_dev"
/** `-Pagent=<file>`: a file of agent requests to answer once, unattended. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val agentScript: String? = providers.gradleProperty("agent").orNull?.let { rootProject.file(it).absolutePath }

/** `-Pdriven`: this client is driven by an agent, not played. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val drivenClient: Boolean = providers.gradleProperty("driven").isPresent
/** Published artifact name; deliberately not the lowercase mod id. */
val modName = property("mod.name") as String
val modGroup = property("mod.group") as String
/** Minecraft range written into neoforge.mods.toml, in Maven range syntax, e.g. `[26.2,26.3)`. */
val mcRange = property("mod.mc_compat") as String
val neoForgeVersion = property("deps.neoforge") as String

// The loader goes in the version so the jar is `ThirstWasTaken2-1.0.7+26.2-neoforge.jar` and never
// collides with the Fabric jar of the same Minecraft version in build/libs/.
version = "${property("mod.version")}+${sc.current.version}-neoforge"
base.archivesName = modName

/** Minecraft 26.1 moved to Java 25; 1.21.x still runs on Java 21. The same rule as the Fabric nodes. */
val requiredJava: JavaVersion =
    if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

repositories {
    // ModDevGradle supplies the NeoForged, Mojang and Minecraft libraries repositories.
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

/**
 * The mod loader this node builds for. Loader code lives beside the source set it belongs to, in
 * `src/main/<loader>` and `src/client/<loader>`, and only this loader's directories are compiled.
 * Everything else in `src/main/java` and `src/client/java` is loader independent; `checkLoaderSeam`
 * enforces it. See src/main/java/com/thirstwastaken2/platform/AGENTS.md.
 */
val loader = "neoforge"

/*
 * One source set on NeoForge. ModDevGradle has no split between `main` and `client`, so the client
 * sources compile into `main` here. The four Fabric nodes keep `loom.splitEnvironmentSourceSets()`,
 * so the compiler still catches client code reached from common code on four nodes out of five.
 *
 * `src/datagen` is absent on purpose: the generators stay Fabric only, and this node reads the
 * datapack and asset JSON that the Fabric node of the same Minecraft version writes. `src/gametest`
 * and `src/dev` are both here, each as a mod of its own, below.
 *
 * `src/client` cannot simply be listed as a directory of `main`. Stonecutter preprocesses
 * `src/<name>` only for a source set called `<name>`, so without a `client` source set nothing
 * writes this node's copy of the client sources, and a `src/client/...` directory on `main` then
 * resolves to nothing and compiles nothing, without an error. The `client` source set below exists
 * only to be preprocessed; it is never compiled or packaged. Its output goes into `main` by path:
 * the sources themselves when this is the active node, which Stonecutter leaves in place, and
 * Stonecutter's generated copy otherwise.
 */
sourceSets.create("client")
val clientSources: File =
    if (sc.current.isActive) rootProject.file("src/client")
    else layout.buildDirectory.dir("generated/stonecutter/client").get().asFile

/**
 * NeoForge's two fluid APIs: IFluidHandler on 21.1, the transfer API (ResourceHandler) from 21.9. Code
 * that implements one of them lives in the directory of its generation, here and in an integration whose
 * other mod moved with NeoForge.
 */
val transferApi = sc.current.parsed >= "1.21.2"

sourceSets.main {
    java.srcDir("src/main/$loader/java")
    resources.srcDir("src/main/$loader/resources")
    java.srcDir(if (transferApi) "src/main/$loader-transfer/java" else "src/main/$loader-fluidhandler/java")
    java.srcDir(files(clientSources.resolve("java"), clientSources.resolve("$loader/java"))
        .builtBy("stonecutterGenerateClient"))
    resources.srcDir(files(clientSources.resolve("resources"), clientSources.resolve("$loader/resources"))
        .builtBy("stonecutterGenerateClient"))
    // Written by the Fabric node's `runDatagen`, keyed by Minecraft version rather than by node so both
    // nodes of a Minecraft version share one directory. See src/datagen/java/AGENTS.md.
    resources.srcDir(rootProject.file("src/main/generated/${sc.current.version}"))
}

/**
 * The optional integrations this node builds: each is a set of source directories only a node that sets
 * its deps key compiles, and only there does the manifest name its mixin config. What each one adds is a
 * row of the table in build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt, which
 * build.gradle.kts reads too; what is specific to one mod, such as its dependencies, stays below with its
 * own comment.
 */
val nodeIntegrations = integrationsFor(Loader.NEOFORGE) { findProperty(it) != null }

sourceSets.main {
    nodeIntegrations.forEach { integration ->
        integration.mainRoots(transferApi).forEach { root ->
            java.srcDir("$root/java")
            resources.srcDir("$root/resources")
        }
        // From the preprocessed client sources like the rest of them.
        integration.clientJava?.let { dir ->
            java.srcDir(files(clientSources.resolve(dir)).builtBy("stonecutterGenerateClient"))
        }
    }
}

/** Create's Modrinth version id, set only on the nodes that build the Sand Filter. See src/main/create/AGENTS.md. */
val createVersion = findProperty("deps.create") as String?

/**
 * The libraries Create bundles inside its jar: Ponder (with Catnip), Flywheel and Registrate. The Sand
 * Filter extends classes whose supertypes live there, so the compiler needs them; at runtime FML reads
 * them out of Create's jar itself.
 */
val createLibraries = createVersion?.let { version ->
    val resolved = configurations.detachedConfiguration(dependencies.create("maven.modrinth:create:$version"))
        .apply { isTransitive = false }
    tasks.register<Sync>("createLibraries") {
        description = "Copies the libraries Create bundles out of its jar, for the compiler"
        from(resolved.elements.map { jars -> jars.map { zipTree(it) } }) {
            include("META-INF/jarjar/*.jar")
            eachFile { path = name }
        }
        includeEmptyDirs = false
        into(layout.buildDirectory.dir("create"))
    }
}

/** Sophisticated Core's Modrinth version id, where it has a release. See src/main/sophisticated/AGENTS.md. */
val sophisticatedCoreVersion = findProperty("deps.sophisticated_core") as String?

/** Supplementaries' Modrinth version id, on `1.21.1-neoforge` only. See src/main/supplementaries/AGENTS.md. */
val supplementariesVersion = findProperty("deps.supplementaries") as String?

/**
 * Kaleidoscope Cookery's Modrinth version id, set on `1.21.1-neoforge` and nowhere else: the official
 * mod has no NeoForge build past 1.21.1. See docs/dev/integration/KALEIDOSCOPE-COOKERY-INTEGRATION.md.
 */
val kaleidoscopeCookeryVersion = findProperty("deps.kaleidoscope_cookery") as String?

/**
 * Brewin' and Chewin's Modrinth version id, set on `1.21.1-neoforge` and nowhere else among the NeoForge
 * nodes. See docs/dev/integration/BREWIN-AND-CHEWIN-INTEGRATION.md.
 */
val brewinAndChewinVersion = findProperty("deps.brewin_and_chewin") as String?

/**
 * Let's Do: Farm & Charm's Modrinth version id, and Candlelight's, its addon, set on `1.21.1-neoforge`
 * and nowhere else among the NeoForge nodes. See docs/dev/integration/FARM-AND-CHARM-INTEGRATION.md.
 */
val farmAndCharmVersion = findProperty("deps.farm_and_charm") as String?

/** Cold Sweat's Modrinth version id, on `1.21.1-neoforge` only. See src/main/coldsweat/AGENTS.md. */
val coldSweatVersion = findProperty("deps.cold_sweat") as String?

/** Cultural Delights' Modrinth version id, on `1.21.1-neoforge` only. See src/main/culturaldelights/AGENTS.md. */
val culturalDelightsVersion = findProperty("deps.cultural_delights") as String?

/** Serene Seasons' Modrinth version id, on every node. See src/main/sereneseasons/AGENTS.md. */
val sereneSeasonsVersion = findProperty("deps.serene_seasons") as String?

/*
 * The same gametests the Fabric nodes run, as their own small mod, so none of it reaches the jar.
 * `src/gametest/neoforge` holds the harness that finds and registers them, in place of Fabric API's;
 * the test classes themselves are shared, and stonecutter.gradle.kts swaps their one Fabric import.
 * See src/gametest/java/AGENTS.md.
 */
val gametest: SourceSet = sourceSets.create("gametest") {
    java.srcDir("src/gametest/$loader/java")
    resources.srcDir("src/gametest/$loader/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

/*
 * The dev tools, as their own small mod, the same arrangement as the gametests: the agent and
 * `/thirst benchmark`, both of them here. `src/dev/neoforge` holds the entrypoints, the loader seam the
 * agent needs and this loader's `BenchmarkPlayer`, which is the one class of the benchmark that names a
 * loader: it extends NeoForge's `FakePlayer` where the Fabric copy extends Fabric API's.
 * See src/dev/java/AGENTS.md.
 */
val dev: SourceSet = sourceSets.create("dev") {
    java.srcDir("src/dev/$loader/java")
    resources.srcDir("src/dev/$loader/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

/*
 * A `-javaagent` for `-Pagent` runs, JDK only: it stops the run when FML writes a loading failure's
 * crash report. A missing dependency fails before any mod mixin applies, so the dev mod's own
 * LoadingErrorScreenMixin never runs and the error screen would hold the task open. See
 * src/watchdog/java/com/thirstwastaken2/dev/watchdog/LoadingFailureWatchdog.java.
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
// The runs name the jar by path, which carries no task dependency.
if (agentScript != null) tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(watchdogJar) }

/*
 * The optional mods runClient loads, the same set the Fabric runClient has minus Mod Menu, which is
 * Fabric only. They go on that run alone: on `runtimeOnly` they would load into runServer and
 * runGametest too, and the gametests expect a server without AppleSkin. ModDevGradle's per-run
 * `additionalRuntimeClasspath` would be the place, but it refuses dependencies from Minecraft 26.2 on,
 * and a run's classpath is its source set's runtime classpath. So runClient gets a source set with no
 * sources of its own, whose runtime classpath is `main`'s plus these.
 *
 * The dev tools ride along on both of these, so an agent can drive runServer and every client through
 * run/<node>/agent/<name>/.
 */
val clientRunMods: Configuration = configurations.create("clientRunMods")

/**
 * `-PwithoutOptional=<name,...>`, which leaves optional mods out of runClient and the two extra clients.
 * The same flag and names as build.gradle.kts; see build-logic's OptionalRunMods.
 */
val optionalRunMods = OptionalRunMods(providers.gradleProperty("withoutOptional").orNull)

/** Adds a mod to the clients' run only, unless `-PwithoutOptional` names it or one of the libraries it lists. */
fun runClientMod(names: List<String>, notation: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    if (optionalRunMods.include(names)) dependencies.add(clientRunMods.name, notation, configure)
}
/*
 * Added to, never replaced. A source set's own runtime classpath is where ModDevGradle puts DevLaunch,
 * whose `Main` is the class every run is launched through, so a source set that assigns its classpath
 * outright launches nothing: `Could not find or load main class net.neoforged.devlaunch.Main`.
 */
val clientRun: SourceSet = sourceSets.create("clientRun") {
    runtimeClasspath += dev.output + sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath +
        clientRunMods
}
val serverRun: SourceSet = sourceSets.create("serverRun") {
    runtimeClasspath += dev.output + sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

neoForge {
    version = neoForgeVersion

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
        // NeoForge mod ids cannot contain a hyphen; the Fabric nodes use the same id.
        create("thirstwastaken2_gametest") {
            sourceSet(gametest)
        }
        create(devModId) {
            sourceSet(dev)
        }
    }

    runs {
        // Read out here: inside a run, `project` is the run model's own deprecated accessor.
        val node = project.name
        configureEach {
            // `-Pagent=<file>` answers that file of agent requests once the game is up and then stops
            // it, which is what an unattended run is. Without it the agent is still there, waiting on
            // run/<node>/agent/<name>/in.jsonl. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
            agentScript?.let {
                systemProperty("thirstwastaken2.agent.script", it)
                systemProperty("thirstwastaken2.agent.script.exit", "true")
                jvmArguments.add(watchdogJar.flatMap { jar -> jar.archiveFile }.map { "-javaagent:${it.asFile.absolutePath}" })
            }

            // `-Pdriven` says this client is driven by an agent rather than played: it opens
            // maximised and never takes the mouse pointer, so the desktop stays usable while a
            // script drives it. An unattended `-Pagent=<file>` run implies it.
            if (drivenClient) systemProperty("thirstwastaken2.agent.driven", "true")

            // One run directory per node, for the reason build.gradle.kts gives: a world saved by
            // one Minecraft version is not readable by another, and a failed test run must not
            // leave a broken world behind for runServer. The extra clients get one each as well:
            // two running clients cannot share a directory, and theirs must not touch runClient's.
            // The benchmark falls into the last branch on purpose: it shares runServer's world and
            // accepted EULA, exactly as it does on the Fabric nodes.
            gameDirectory.set(rootProject.file(when (name) {
                "gametest" -> "run/$node/gametest"
                "manualA", "manualB" -> "run/manual-$node-${name.last()}"
                else -> "run/$node"
            }))
        }

        create("client") {
            client()
            sourceSet = clientRun
            // `-Pquickplay=<world>` opens that singleplayer world straight from launch, so an
            // unattended `-Pagent` script starts inside it. The world has to exist in run/<node>/saves.
            providers.gradleProperty("quickplay").orNull?.let { world ->
                programArguments.addAll("--quickPlaySingleplayer", world)
                // So client.info can say the world was asked for and never opened.
                systemProperty("thirstwastaken2.agent.quickplay", world)
            }
        }
        create("server") {
            server()
            sourceSet = serverRun
        }
        // Unattended benchmark: starts the dedicated server, runs `/thirst benchmark <-Pbenchmark>` from
        // the console once it is up, writes run/<node>/benchmark/latest.json and stops the server. It
        // shares runServer's directory, world and accepted EULA, so the two cannot run at the same time.
        create("benchmark") {
            server()
            sourceSet = serverRun
            systemProperty("thirstwastaken2.benchmark",
                providers.gradleProperty("benchmark").getOrElse("standard"))
            systemProperty("thirstwastaken2.benchmark.exit", "true")
            // A world of its own inside runServer's directory, generated with the fixed seed
            // gradle/shared.gradle.kts puts in server.properties, so a run measures the same terrain
            // on every machine and on both loaders of this Minecraft version, and never the dev world.
            programArguments.addAll("--world", providers.gradleProperty("thirst.benchmark.world").get())

            // `-Pprofile` records the run with JFR, which is in every JDK, and writes
            // run/<node>/benchmark/latest.jfr for JDK Mission Control to open. `settings=profile` is the
            // heavier of JFR's two built-in profiles; the deep stack depth is what makes an allocation
            // event name the mod's own call site rather than a truncated vanilla frame, and
            // DebugNonSafepoints lets a sample land where the code really was rather than at the nearest
            // safepoint. A recording slows the run down and skews every figure in the report, which is
            // why the report says so and aggregate.py refuses a set with one in it.
            if (providers.gradleProperty("profile").isPresent) {
                jvmArguments.addAll(flightRecorder(rootProject.file("run/$node/benchmark/latest.jfr")))
            }
        }
        // Two more clients, for the checklist items that need a second player: MANUAL-TESTING.md's
        // "Sync to the client" section, where each player has to see their own bar and no one else's.
        // Two dev clients cannot both be `Dev` on one server, so each is named here, and
        // --quickPlayMultiplayer joins runServer on this machine from the title screen.
        listOf("A", "B").forEach { tester ->
            create("manual$tester") {
                client()
                sourceSet = clientRun
                programArguments.addAll(
                    "--username", "Tester$tester",
                    "--width", "1100", "--height", "700",
                    "--quickPlayMultiplayer", "localhost:25565",
                )
                // Each client answers its own queue, run/manual-<node>-<A|B>/agent/<A|B>/, which is
                // what "each player sees only their own bar" is read out of.
                systemProperty("thirstwastaken2.agent", tester)
            }
        }
        // The GameTest runner: a dedicated server that runs every registered test headlessly, skips
        // the EULA prompt and exits with the number of failed required tests, the same contract as the
        // Fabric runner. `runGametest` is the task name on every node.
        create("gametest") {
            type = "gameTestServer"
            sourceSet = gametest
            // Vanilla's own JUnit report, at the path the Fabric nodes write theirs to, so CI uploads
            // it the same way. The server's --report option arrived with 1.21.5; before it the harness
            // installs the same reporter itself when this property names a file.
            val report = layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath
            if (sc.current.parsed >= "1.21.5") programArguments.addAll("--report", report)
            else systemProperty("thirstwastaken2.gametest.report", report)
        }

        // ModDevGradle loads every declared mod by default, and each run only has the classes of the
        // source sets on its own classpath. So each one is told exactly which mods to load: the
        // gametests are their own mod and belong to the gametest run alone, and the dev tools belong
        // to every run an agent may drive.
        val mainMod = mods.named(modId)
        val devMod = mods.named(devModId)
        val gametestMod = mods.named("thirstwastaken2_gametest")
        listOf("client", "server", "benchmark", "manualA", "manualB").forEach { run ->
            named(run) { loadedMods.set(mainMod.zip(devMod) { main, agent -> setOf(main, agent) }) }
        }
        named("gametest") { loadedMods.set(mainMod.zip(gametestMod) { main, tests -> setOf(main, tests) }) }
    }
}

dependencies {
    // Optional integrations the client code compiles against. The mod runs without either; it never
    // takes a hard dependency, and `Loader.isModLoaded` gates every use. Mod Menu is Fabric only, so
    // `ModMenuIntegration` stays in src/client/fabric and the config screen is registered through
    // NeoForge's own IConfigScreenFactory instead.
    compileOnly("maven.modrinth:appleskin:${property("deps.appleskin")}")
    compileOnly("maven.modrinth:jade:${property("deps.jade")}")

    runClientMod(listOf("appleskin"), "maven.modrinth:appleskin:${property("deps.appleskin")}")
    runClientMod(listOf("jade"), "maven.modrinth:jade:${property("deps.jade")}")
    // AppleSkin's own config screen, on the nodes that set it. Nothing compiles against Cloth Config and
    // the mod never reaches for it, so a node whose Minecraft version has no NeoForge build of it yet
    // simply runs the dev client without it.
    findProperty("deps.cloth_config")?.let {
        runClientMod(listOf("cloth-config", "cloth_config"), "maven.modrinth:cloth-config:$it")
    }

    // Farmer's Delight, on the nodes that set it: nothing compiles against it, since the mod reaches it
    // by registry id alone, so it is only here to test its drinks, the Cooking Pot and Nourishment.
    findProperty("deps.farmersdelight")?.let {
        runClientMod(listOf("farmers-delight", "farmersdelight"), "maven.modrinth:farmers-delight:$it")
    }

    // Fruits Delight, on the node that sets it. Nothing compiles against it: its drinks are reached by
    // registry id, and src/main/fruitsdelight's mixins name their targets by string. Only here to test
    // them. Its L2 libraries are nested in its jar, which NeoForge loads itself.
    findProperty("deps.fruits_delight")?.let {
        runClientMod(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"), "maven.modrinth:fruits-delight:$it") { isTransitive = false }
    }

    // Expanded Delight, on the node that sets it. Nothing compiles against it: its foods are reached by
    // registry id, and src/main/expandeddelight's mixin names its target by string. Only here to test it.
    findProperty("deps.expanded_delight")?.let {
        runClientMod(listOf("expanded-delight", "expandeddelight", "farmers-delight", "farmersdelight"), "maven.modrinth:expanded-delight:$it") { isTransitive = false }
    }

    if (createVersion != null && createLibraries != null) {
        compileOnly("maven.modrinth:create:$createVersion") { isTransitive = false }
        compileOnly(files(createLibraries.map { it.destinationDir.listFiles().orEmpty().toList() })
            .builtBy(createLibraries))
        // Test the Sand Filter with pipes, pumps and spouts in runClient.
        runClientMod(listOf("create"), "maven.modrinth:create:$createVersion") { isTransitive = false }
    }

    if (sophisticatedCoreVersion != null) {
        compileOnly("maven.modrinth:sophisticated-core:$sophisticatedCoreVersion") { isTransitive = false }
        // Test the upgrades in runClient, inside a backpack.
        runClientMod(listOf("sophisticated-core", "sophisticatedcore"),
            "maven.modrinth:sophisticated-core:$sophisticatedCoreVersion") { isTransitive = false }
        findProperty("deps.sophisticated_backpacks")?.let {
            runClientMod(listOf("sophisticated-backpacks", "sophisticatedbackpacks", "sophisticated-core", "sophisticatedcore"),
                "maven.modrinth:sophisticated-backpacks:$it") { isTransitive = false }
        }
        findProperty("deps.sophisticated_storage")?.let {
            runClientMod(listOf("sophisticated-storage", "sophisticatedstorage", "sophisticated-core", "sophisticatedcore"),
                "maven.modrinth:sophisticated-storage:$it") { isTransitive = false }
        }
    }

    if (supplementariesVersion != null) {
        // Moonlight carries the soft fluid system three of the mixins target; Supplementaries itself only
        // the faucet's cauldron behaviour.
        compileOnly("maven.modrinth:supplementaries:$supplementariesVersion") { isTransitive = false }
        compileOnly("maven.modrinth:moonlight:${property("deps.moonlight")}") { isTransitive = false }
        // Test jars, goblets and faucets in runClient. The gametests and runServer run without them, which
        // is what proves the mod is unchanged when they are absent.
        runClientMod(listOf("supplementaries", "moonlight"), "maven.modrinth:supplementaries:$supplementariesVersion") { isTransitive = false }
        runClientMod(listOf("moonlight"), "maven.modrinth:moonlight:${property("deps.moonlight")}") { isTransitive = false }
    }

    if (kaleidoscopeCookeryVersion != null) {
        compileOnly("maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookeryVersion") { isTransitive = false }
        // Test the stockpot and the teapot in runClient. The gametests and runServer run without it, which
        // is what proves the mod is unchanged when it is absent.
        runClientMod(listOf("kaleidoscope-cookery", "kaleidoscope-cookery-refabricated", "kaleidoscope_cookery"),
            "maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookeryVersion") { isTransitive = false }
    }

    if (brewinAndChewinVersion != null) {
        compileOnly("maven.modrinth:brewin-and-chewin:$brewinAndChewinVersion") { isTransitive = false }
        // Test the keg in runClient. The gametests and runServer run without it, which is what proves the
        // mod is unchanged when it is absent. NeoForge loads the Greenhouse Config nested in its jar;
        // Farmer's Delight is already above, and leaving it out leaves this out.
        runClientMod(listOf("brewin-and-chewin", "brewinandchewin", "farmers-delight", "farmersdelight"),
            "maven.modrinth:brewin-and-chewin:$brewinAndChewinVersion") { isTransitive = false }
    }

    if (farmAndCharmVersion != null) {
        compileOnly("maven.modrinth:lets-do-farm-charm:$farmAndCharmVersion") { isTransitive = false }
        // Test the well, the trough, the Cooking Pot and Candlelight's kitchen sinks in runClient. The
        // gametests and runServer run without them, which is what proves the mod is unchanged when they are
        // absent. Both require Architectury API. Leaving Farm & Charm out leaves all three out; leaving
        // Candlelight out keeps the other two.
        val names = listOf("farm-and-charm", "farm_and_charm", "lets-do-farm-charm")
        runClientMod(names, "maven.modrinth:lets-do-farm-charm:$farmAndCharmVersion") { isTransitive = false }
        runClientMod(names + listOf("candlelight"),
            "maven.modrinth:lets-do-candlelight-farmcharm-compat:${property("deps.candlelight")}") { isTransitive = false }
        runClientMod(names + listOf("architectury"),
            "maven.modrinth:architectury-api:${property("deps.architectury")}") { isTransitive = false }
    }

    if (coldSweatVersion != null) {
        compileOnly("maven.modrinth:cold-sweat:$coldSweatVersion") { isTransitive = false }
        // Off in runClient by default: its temperature gauge, HUD icon and world changes get into every
        // other test and screenshot. Uncomment the line below only to work on the Cold Sweat integration
        // (the climate, the waterskin, the Boiler). The gametests and runServer run without it, which is
        // what proves the mod is unchanged when it is absent.
        // runClientMod(listOf("cold-sweat", "cold_sweat"), "maven.modrinth:cold-sweat:$coldSweatVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=cold-sweat` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("cold-sweat", "cold_sweat"))
    }

    if (culturalDelightsVersion != null) {
        compileOnly("maven.modrinth:cultural-delights:$culturalDelightsVersion") { isTransitive = false }
        // Test the vat in runClient. The gametests and runServer run without it, which is what proves the
        // mod is unchanged when it is absent. It requires Cook's Collection; Farmer's Delight is above, and
        // leaving it out leaves both out.
        runClientMod(listOf("cultural-delights", "culturaldelights", "cooks-collection", "cookscollection",
            "farmers-delight", "farmersdelight"),
            "maven.modrinth:cultural-delights:$culturalDelightsVersion") { isTransitive = false }
        runClientMod(listOf("cooks-collection", "cookscollection", "cultural-delights", "culturaldelights", "farmers-delight", "farmersdelight"),
            "maven.modrinth:cooks-collection:${property("deps.cooks_collection")}") { isTransitive = false }
    }

    if (sereneSeasonsVersion != null) {
        val glitchCoreVersion = property("deps.glitchcore").toString()
        // GlitchCore is compiled against too: before 26.2 the dimension check reads Serene Seasons'
        // config, whose class extends one of GlitchCore's.
        compileOnly("maven.modrinth:serene-seasons:$sereneSeasonsVersion") { isTransitive = false }
        compileOnly("maven.modrinth:glitchcore:$glitchCoreVersion") { isTransitive = false }
        // Off in runClient, for the reason build.gradle.kts gives. Uncomment the two lines below only to
        // work on the Serene Seasons integration. The gametests and runServer run without it, which is
        // what proves the mod is unchanged when it is absent. NeoForge loads the Night Config nested in
        // their jars itself.
        val names = listOf("serene-seasons", "sereneseasons")
        // runClientMod(names, "maven.modrinth:serene-seasons:$sereneSeasonsVersion") { isTransitive = false }
        // runClientMod(names + listOf("glitchcore"), "maven.modrinth:glitchcore:$glitchCoreVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=serene-seasons` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(names + listOf("glitchcore"))
    }

    // A name no node loads is refused in stonecutter.gradle.kts, once every node has said what it takes.
    project.extra["thirst.optionalRunMods"] = optionalRunMods.offered
}

/*
 * Datagen runs on Fabric only, so the recipes and advancements it writes use Fabric's spellings for
 * three things NeoForge also has, under other names. This node rewrites them as it copies the files,
 * rather than datagen writing a second copy or NeoForge registering Fabric's names: an alias cannot
 * work, because NeoForge reads the ingredient type from a different key altogether.
 *
 * | Fabric                                                   | NeoForge                                             |
 * |----------------------------------------------------------|------------------------------------------------------|
 * | `fabric:type` `fabric:components`, `base`, `components`  | `neoforge:ingredient_type` `neoforge:components`, `items`, `components` |
 * | `fabric:type` `fabric:any`, `ingredients`                | `neoforge:ingredient_type` `neoforge:compound`, `children` |
 * | `fabric:load_conditions`, `fabric:all_mods_loaded`       | `neoforge:conditions`, one `neoforge:mod_loaded` per mod |
 * | `condition` `thirstwastaken2:item_enabled`, `item`       | `type` `thirstwastaken2:item_enabled`, `item`        |
 *
 * Both components ingredients take a `DataComponentPatch` and match a stack that carries at least the
 * listed values, which is NeoForge's default `strict: false`, so `strict` is left out.
 *
 * On 1.21.1 NeoForge 21.1 reads the ingredient type from vanilla's own `type` key, and Fabric writes
 * `base` as a whole ingredient, `{"item": ...}`, where NeoForge's `items` is a holder set, so the item
 * id is taken out of it. Later versions write `base` as the holder set already.
 *
 * Anything else Fabric-specific fails the build here, naming the file, and `checkNeoForgeResources`
 * catches whatever this does not look at. A generator that starts writing a new Fabric shape therefore
 * breaks this node's build rather than loading as a broken recipe.
 */
/** The key NeoForge reads a custom ingredient's type from: vanilla's `type` until it took one of its own. */
val ingredientTypeKey = if (sc.current.parsed > "1.21.1") "neoforge:ingredient_type" else "type"

fun neoForgeJson(node: Any?, file: String): Any? = when (node) {
    is List<*> -> node.map { neoForgeJson(it, file) }
    is Map<*, *> -> when (val type = node["fabric:type"]) {
        null -> node.entries.associate { (key, value) ->
            if (key == "fabric:load_conditions") "neoforge:conditions" to neoForgeConditions(value, file)
            else key as String to neoForgeJson(value, file)
        }
        "fabric:components" -> {
            requireKeys(node, setOf("fabric:type", "base", "components"), file)
            mapOf(ingredientTypeKey to "neoforge:components",
                "items" to neoForgeItems(node["base"], file), "components" to node["components"])
        }
        "fabric:any" -> {
            requireKeys(node, setOf("fabric:type", "ingredients"), file)
            mapOf(ingredientTypeKey to "neoforge:compound",
                "children" to neoForgeJson(node["ingredients"], file))
        }
        else -> throw GradleException("$file: no NeoForge translation for the Fabric ingredient type $type")
    }
    else -> node
}

/**
 * A components ingredient's `base` as the holder set NeoForge's `items` reads: an id, a `#tag` or a list
 * of ids. It already is one from 1.21.2; on 1.21.1 it is a vanilla ingredient, a single item or tag.
 */
fun neoForgeItems(base: Any?, file: String): Any? = when {
    base is String -> base
    base is List<*> && base.all { it is String } -> base
    base is Map<*, *> && base.keys == setOf("item") -> base["item"]
    base is Map<*, *> && base.keys == setOf("tag") -> "#${base["tag"]}"
    else -> throw GradleException("$file: no NeoForge translation for the components ingredient base $base")
}

fun neoForgeConditions(conditions: Any?, file: String): List<Map<String, Any?>> =
    (conditions as List<*>).flatMap { condition ->
        condition as Map<*, *>
        when (condition["condition"]) {
            "fabric:all_mods_loaded" ->
                (condition["values"] as List<*>).map { mapOf("type" to "neoforge:mod_loaded", "modid" to it) }
            // The mod's own condition, registered under the same id on both loaders; only its key moves.
            "thirstwastaken2:item_enabled" -> {
                if (condition.keys != setOf("condition", "item")) {
                    throw GradleException("$file: no NeoForge translation for ${condition.keys} in thirstwastaken2:item_enabled")
                }
                listOf(mapOf("type" to "thirstwastaken2:item_enabled", "item" to condition["item"]))
            }
            else -> throw GradleException("$file: no NeoForge translation for the Fabric load condition ${condition["condition"]}")
        }
    }

fun requireKeys(node: Map<*, *>, keys: Set<String>, file: String) {
    val unknown = node.keys - keys
    if (unknown.isNotEmpty()) throw GradleException("$file: no NeoForge translation for $unknown in ${node["fabric:type"]}")
}

/** Written by the Fabric node's `runDatagen`; the only files whose Fabric spellings are translated. */
val generatedResources = rootProject.file("src/main/generated/${sc.current.version}")

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to mcRange,
        "neoforge" to neoForgeVersion,
        "java" to requiredJava.majorVersion,
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
    // Datagen's hash cache, which Loom keeps out of the Fabric mod jar and nothing keeps out of
    // this one.
    exclude("**/.cache/**")

    // Only a node that builds an integration may name its mixin config, or FML would fail to find it on
    // every other one. It is appended to the built manifest, with the integration's mods as optional
    // dependencies, rather than templated into the source manifest every NeoForge node shares. One input
    // per integration, so a change of its version reruns this.
    integrations.filter { Loader.NEOFORGE in it.loaders }.forEach { integration ->
        inputs.property(integration.dir, findProperty(integration.depsKey)?.toString() ?: "")
    }
    if (nodeIntegrations.isNotEmpty()) {
        val manifest = destinationDir.resolve("META-INF/neoforge.mods.toml")
        val appended = nodeIntegrations.joinToString("") { it.neoForgeManifest(modId) }
        doLast { manifest.appendText(appended) }
    }

    // Translates the generated JSON in place, once it is copied. Only files that came from the
    // generated root are read, and only those naming Fabric are rewritten, so the rest keep their
    // bytes. The Copy task copies every file again whenever any input changes, so a translated file
    // is never translated twice.
    val output = destinationDir
    doLast {
        generatedResources.walk().filter { it.isFile && it.extension == "json" }.forEach { source ->
            val relative = source.relativeTo(generatedResources).invariantSeparatorsPath
            val target = output.resolve(relative)
            if (!target.isFile) return@forEach
            val text = target.readText()
            if (!text.contains("\"fabric:")) return@forEach
            val translated = neoForgeJson(groovy.json.JsonSlurper().parseText(text), relative)
            target.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(translated)))
        }
    }
}

// The dev tools have a mixin config of their own, for the one mixin that records where the HUD drew
// the bar. It needs the same compatibility level as the rest, for the same reason: a node on Java 25
// writes class files Mixin refuses to read at level 21.
tasks.named<ProcessResources>("processDevResources") {
    inputs.property("java", requiredJava.majorVersion)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
}

/**
 * Fails when a Fabric key survives in the processed resources, from datagen or from a hand-written
 * file, because NeoForge would load it as a broken recipe or ignore the condition. CI runs it on the
 * NeoForge job; see the translation above processResources.
 */
tasks.register("checkNeoForgeResources") {
    group = "verification"
    description = "Fails when a Fabric-only JSON key survives into the NeoForge resources"

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
            "Fabric keys in the NeoForge resources. Translate the shape in build.neoforge.gradle.kts " +
                "or stop writing it:\n" + offenders.joinToString("\n")
        }
    }
}

// Stonecutter rewrites the versioned comments in `src/` into this node's own source tree, so
// everything that reads those files has to wait for it. Loom needs this for processResources;
// ModDevGradle needs it before it builds the Minecraft artifacts it compiles against.
tasks.named("processResources") { dependsOn("stonecutterGenerate") }

/*
 * Every NeoForge node decompiles and recompiles Minecraft in createMinecraftArtifacts, which takes
 * minutes and gigabytes each. With org.gradle.parallel=true several nodes would do it at once, so a
 * shared build service lets one run at a time. The class is compiled once for this script, so every
 * node that applies it registers the same service.
 */
abstract class CreateMinecraftArtifactsMutex : BuildService<BuildServiceParameters.None>
val createMinecraftArtifactsMutex = gradle.sharedServices
    .registerIfAbsent("createMinecraftArtifactsMutex", CreateMinecraftArtifactsMutex::class) { maxParallelUsages = 1 }

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
    usesService(createMinecraftArtifactsMutex)
}

// Vanilla's reporter writes the file but not the directory it goes in.
tasks.named("runGametest") {
    val reportDir = layout.buildDirectory.dir("gametest")
    doFirst { reportDir.get().asFile.mkdirs() }
}

// The toolchain, the seam checks, the jar excludes and `buildAndCollect` are shared with the Fabric
// nodes. The Java version is passed in because it follows from the node's Minecraft version, which
// only this script can read.
extra["thirst.requiredJava"] = requiredJava.majorVersion
// And the integration table, as far as the seam checks need it: a script applied with `apply(from)`
// cannot see the classes of build-logic, so it cannot read the table itself.
extra["thirst.integrations"] = integrations.map { it.dir }
extra["thirst.loaderIndependentIntegrations"] = integrations.filter { it.loaderIndependent }.map { it.dir }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.jar {
    // The MIT notice of the code this mod is based on has to ship with it, alongside the GPL.
    from(files(rootProject.file("LICENSE"), rootProject.file("CREDITS.md"),
            rootProject.file("licenses/ThirstWasTaken-MIT.txt"))) {
        rename { name -> name.substringBeforeLast('.') + "_$modId" + name.removePrefix(name.substringBeforeLast('.')) }
    }
}

// What this node hands `buildAndCollect`, which gradle/shared.gradle.kts registers: ModDevGradle
// does not remap, so the plain jar is the one that ships.
tasks.named<Copy>("buildAndCollect") {
    from(tasks.jar.flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
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
