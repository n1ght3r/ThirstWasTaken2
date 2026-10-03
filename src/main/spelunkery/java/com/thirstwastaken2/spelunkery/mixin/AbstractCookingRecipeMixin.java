package com.thirstwastaken2.spelunkery.mixin;

import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spelunkery smelts any water bucket into a salt bucket ({@code spelunkery:salt_from_boiling}), which
 * takes a fresh bucket our purification recipes also take, and the furnace picks between them by recipe
 * order. Now a cooking recipe that makes a salt bucket takes only sea water: fresh water always purifies,
 * and sea water, which nothing of ours cooks, boils down to its salt. Matched by result rather than by
 * recipe id, so a data pack's smoker or campfire copy is covered too.
 */
@Mixin(AbstractCookingRecipe.class)
abstract class AbstractCookingRecipeMixin {
    @Unique
    private static final Identifier thirst$SALT_BUCKET = Identifier.fromNamespaceAndPath("spelunkery", "salt_bucket");

    @Shadow @Final protected ItemStack result;

    /** Whether this recipe makes a salt bucket, worked out on its first match: a recipe never changes. */
    @Unique
    private Boolean thirst$makesSalt;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void thirst$onlySeaWaterMakesSalt(SingleRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (thirst$makesSalt == null) thirst$makesSalt = Vanilla.itemId(result.getItem()).equals(thirst$SALT_BUCKET);
        if (thirst$makesSalt && !WaterPurity.isSalty(input.item())) cir.setReturnValue(false);
    }
}
