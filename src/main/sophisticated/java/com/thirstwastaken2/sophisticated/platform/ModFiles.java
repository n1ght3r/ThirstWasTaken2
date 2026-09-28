package com.thirstwastaken2.sophisticated.platform;

import net.neoforged.fml.loading.moddiscovery.ModFileInfo;

/**
 * FML's mod files, whose API changed shape between the NeoForge versions the integration builds for.
 *
 * <p>Names no Sophisticated class and no Minecraft class, like {@code SophisticatedPresence}, its only
 * caller, which the mixin plugin asks before anything is loaded.
 */
public final class ModFiles {
    private ModFiles() { }

    /** Whether a mod's own jar holds a file, found without loading it. NeoForge 21.2 moved the lookup into the file's contents. */
    public static boolean contains(ModFileInfo mod, String path) {
        //? if >=1.21.2 {
        return mod.getFile().getContents().containsFile(path);
        //?} else
        //return java.nio.file.Files.exists(mod.getFile().findResource(path));
    }
}
