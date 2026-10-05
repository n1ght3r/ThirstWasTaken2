package com.thirstwastaken2.buildlogic

/**
 * The Fabric nodes' access widener: what the mod needs opened in vanilla that Fabric API opens only to
 * itself. NeoForge's and Forge's access transformers already make all of it public, so common code can
 * call it the same way on every loader.
 *
 * - The menu type's constructor and its supplier interface, and on the client `MenuScreens.register`
 *   and its constructor interface, for a menu and its screen.
 * - The block entity type's supplier interface, and from 1.21.2 its two-argument constructor, which
 *   replaced the builder. Before 1.21.2 the builder is public and the constructor takes a third
 *   argument, so naming that one there would point at nothing.
 *
 * 26.1 dropped obfuscation, and Loom with it the `named` namespace: from there the file is a class
 * tweaker in `official` names, the format Fabric API's own are in. Before, an access widener in `named`
 * names, which Loom remaps into the jar.
 *
 * @param unobfuscated whether the node is 26.1 or later
 * @param blockEntityTypeConstructor whether the node is 1.21.2 or later
 */
fun fabricAccessWidener(unobfuscated: Boolean, blockEntityTypeConstructor: Boolean): String = buildString {
    appendLine(if (unobfuscated) "classTweaker\tv1\tofficial" else "accessWidener\tv2\tnamed")
    val menuType = "net/minecraft/world/inventory/MenuType"
    val blockEntityType = "net/minecraft/world/level/block/entity/BlockEntityType"
    val menuScreens = "net/minecraft/client/gui/screens/MenuScreens"
    appendLine("accessible\tclass\t$menuType\$MenuSupplier")
    appendLine("accessible\tmethod\t$menuType\t<init>\t(L$menuType\$MenuSupplier;Lnet/minecraft/world/flag/FeatureFlagSet;)V")
    appendLine("accessible\tclass\t$blockEntityType\$BlockEntitySupplier")
    if (blockEntityTypeConstructor) {
        appendLine("accessible\tmethod\t$blockEntityType\t<init>\t(L$blockEntityType\$BlockEntitySupplier;Ljava/util/Set;)V")
    }
    appendLine("accessible\tclass\t$menuScreens\$ScreenConstructor")
    appendLine("accessible\tmethod\t$menuScreens\tregister\t(L$menuType;L$menuScreens\$ScreenConstructor;)V")
}
