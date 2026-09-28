package com.thirstwastaken2.client.compat;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.HangingPotBlock;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Adds the grade of the water under the crosshair to Jade's overlay, for water in the world, a
 * waterlogged block, a water cauldron and the mod's own hanging pot alike.
 *
 * <p>Jade is a compile-only dependency and resolves this class through the {@code jade} entrypoint,
 * which it reads on the dedicated server too. Only {@link #registerClient} touches client classes, so
 * the class itself loads safely there. The annotation is how Jade finds plugins on other loaders.
 */
@WailaPlugin
public final class JadeIntegration implements IWailaPlugin {
    /**
     * Blocks that keep water somewhere this class may not name: a Supplementaries jar keeps it in a
     * Moonlight soft fluid tank, and common code names no foreign class. An integration adds itself
     * here from its own client entry point, and answers null for a block that is not its own.
     *
     * <p>Client thread only, like everything else here: added to while Jade registers, read while it
     * builds its overlay.
     */
    private static final List<Function<BlockEntity, WaterQuality>> CONTAINERS = new ArrayList<>();

    /** @see #CONTAINERS */
    public static void addContainer(Function<BlockEntity, WaterQuality> container) {
        CONTAINERS.add(container);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Registered on Block rather than LiquidBlock, because a waterlogged block holds water that a
        // bottle can be filled from just the same.
        registration.registerBlockComponent(WaterPurityProvider.INSTANCE, Block.class);
    }

    /**
     * Grades the water on the client. Everything {@link WaterPurity#sampleAt} reads - blockstates, the
     * biome and its tags - is synced, so the grade shown is the one the server stamps on a container
     * filled there.
     */
    private enum WaterPurityProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final Identifier UID = ThirstWasTaken2.id("water_purity");
        /**
         * How long a sample stays valid while the crosshair rests on one block. Jade rebuilds its
         * overlay every client tick; a block placed nearby shows up within half a second.
         */
        private static final int RESAMPLE_TICKS = 10;

        // Client thread only, which is the only thread Jade collects tooltips on.
        private ResourceKey<Level> lastDimension;
        private long lastPos;
        private BlockState lastState;
        private long lastSampleTime;
        private WaterQuality lastQuality;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            // A block that holds its water somewhere an integration knows about answers first: it has
            // the grade already, where everything below has to read a blockstate or sample the world.
            WaterQuality held = fromContainer(accessor.getBlockEntity());
            if (held != null) {
                tooltip.add(line(held));
                return;
            }

            BlockState state = accessor.getBlockState();
            // Only the water cauldron and the hanging pot carry the stored-quality property, and an
            // empty pot has nothing to grade.
            if (!state.getFluidState().is(FluidTags.WATER) && !state.hasProperty(WaterPurity.BLOCK_PURITY)) return;
            if (state.getBlock() instanceof HangingPotBlock && HangingPotBlock.quality(state) == null) return;

            tooltip.add(line(sample(accessor.getLevel(), accessor.getPosition(), state)));
        }

        private static Component line(WaterQuality quality) {
            return quality instanceof WaterQuality.Fresh fresh
                    ? WaterPurity.tooltip(fresh.purity())
                    : WaterPurity.saltTooltip();
        }

        private static WaterQuality fromContainer(BlockEntity blockEntity) {
            if (blockEntity == null) return null;
            for (Function<BlockEntity, WaterQuality> container : CONTAINERS) {
                WaterQuality quality = container.apply(blockEntity);
                if (quality != null) return quality;
            }
            return null;
        }

        private WaterQuality sample(Level level, BlockPos pos, BlockState state) {
            long time = level.getGameTime();
            if (lastQuality != null && level.dimension() == lastDimension && pos.asLong() == lastPos
                    && state == lastState && time >= lastSampleTime && time - lastSampleTime < RESAMPLE_TICKS) {
                return lastQuality;
            }
            lastDimension = level.dimension();
            lastPos = pos.asLong();
            lastState = state;
            lastSampleTime = time;
            lastQuality = WaterPurity.sampleAt(level, pos);
            return lastQuality;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
