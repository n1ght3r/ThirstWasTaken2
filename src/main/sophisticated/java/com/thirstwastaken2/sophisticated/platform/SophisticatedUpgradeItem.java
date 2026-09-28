package com.thirstwastaken2.sophisticated.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;

/**
 * Sophisticated's upgrade item, with one constructor on every version. From 1.21.2 an item has to know
 * the id it registers under before it is built, and Core's constructor takes the properties that carry
 * it; before that it built its own. A constructor's {@code super(...)} cannot be moved into a static
 * method, so the branch lives in this class, the same shape as core's {@code platform/DrinkItem}.
 */
public abstract class SophisticatedUpgradeItem<T extends IUpgradeWrapper> extends UpgradeItemBase<T> {
    protected SophisticatedUpgradeItem(IUpgradeCountLimitConfig limits, Identifier id) {
        //? if >=1.21.2 {
        super(limits, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        //?} else
        //super(limits);
    }
}
