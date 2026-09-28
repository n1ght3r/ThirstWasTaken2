package com.thirstwastaken2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.thirstwastaken2.platform.Vanilla;

/**
 * Hand drinking from water the crosshair does not target.
 *
 * <p>The crosshair ignores fluids, so looking at water it lands on the block under the surface, and
 * {@code ThirstManager.drinkByHand} takes that block's water. When that block is out of reach, or
 * there is none (deep water, a waterfall), the crosshair misses, and vanilla sends nothing at all for
 * an empty hand. This picks again with fluids and sends the use on the water itself, through the
 * same vanilla packet a targeted block would, so the server decides exactly as it always does.
 */
public final class HandDrinking {
    private HandDrinking() { }

    /** Called as a right click starts, before vanilla looks at the crosshair. */
    public static void useWaterOutsideTheCrosshair(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        // The same early outs vanilla's startUseItem takes before any use.
        if (player == null || gameMode == null || gameMode.isDestroying() || player.isHandsBusy()) return;
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) return;
        if (!player.isCrouching()) return;

        // Only the first empty hand: the server would take a second packet as a second sip.
        InteractionHand hand = player.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : null;
        if (hand == null) return;

        if (!(player.pick(Vanilla.blockReach(player), 1.0F, true) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return;
        }
        gameMode.useItemOn(player, hand, hit);
    }
}
