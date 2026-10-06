package com.thirstwastaken2.extradelight;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.extradelight.mixins.json} only where Extra Delight is installed, and
 * each mixin only while its target still declares every method it injects into.
 */
public final class ExtraDelightMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "com.lance5057.extradelight.";
    /** Always in the mod, so it says whether the mod is there for the mixin on vanilla's class. */
    private static final String REGISTRY = PACKAGE + "util.BottleFluidRegistry";

    /** The methods each target declares and a mixin injects into, all of which must still be there. */
    private static final Map<String, List<String>> NEEDS_ALL_OF = Map.ofEntries(
            Map.entry(REGISTRY, List.of("getFluidFromBottle", "getBottleFromFluid")),
            Map.entry(PACKAGE + "capabilities.WellFluidCapability", List.of("getFluid", "drain")),
            Map.entry(PACKAGE + "blocks.TapBlock", List.of("useItemOn")),
            Map.entry(PACKAGE + "blocks.sink.SinkCabinetBlock", List.of("useItemOn")),
            Map.entry(PACKAGE + "blocks.funnel.FunnelBlockEntity", List.of("pullFluidIn")),
            Map.entry(PACKAGE + "workstations.chiller.ChillerBlockEntity", List.of("tick", "drainDripTray")),
            Map.entry(PACKAGE + "workstations.meltingpot.MeltingPotBlockEntity", List.of("tick")),
            Map.entry(PACKAGE + "workstations.vat.recipes.VatRecipe", List.of("matches")),
            Map.entry(PACKAGE + "workstations.mixingbowl.recipes.MixingBowlRecipe", List.of("matches")),
            Map.entry(PACKAGE + "workstations.evaporator.recipes.EvaporatorRecipe", List.of("matches")),
            Map.entry(PACKAGE + "blocks.jar.JarBlockEntity", List.of("use")),
            Map.entry(PACKAGE + "blocks.keg.KegBlockEntity", List.of("use")),
            Map.entry(PACKAGE + "workstations.evaporator.EvaporatorBlock", List.of("useItemOn")),
            Map.entry(PACKAGE + "workstations.IFancyTankHandler", List.of("fillInternal", "drainInternal")),
            Map.entry("net.neoforged.neoforge.fluids.FluidUtil", List.of("getFluidHandler")),
            Map.entry("net.minecraft.world.item.crafting.ShapelessRecipe", List.of("matches")));

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!ExtraDelightPresence.hasTarget(REGISTRY)) return false;
        List<String> methods = NEEDS_ALL_OF.get(targetClassName);
        if (methods == null) return false;
        for (String method : methods) {
            if (!ExtraDelightPresence.hasMethod(targetClassName, method)) return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
