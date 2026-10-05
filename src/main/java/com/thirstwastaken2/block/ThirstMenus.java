package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.inventory.MenuType;

public final class ThirstMenus {
    /** The copper distiller's GUI. */
    public static final MenuType<DistillerMenu> COPPER_DISTILLER = Vanilla.registerMenu("copper_distiller",
            DistillerMenu::new);

    private ThirstMenus() { }

    /** Builds and registers the menu types, in this class's static initializer, as {@code ThirstBlocks.register} does. */
    public static void register() { }
}
