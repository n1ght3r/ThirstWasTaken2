package com.thirstwastaken2.dev.benchmark;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The patch of the overworld a run works in: force-loaded chunks for the simulated players to stand in, and
 * a small water fixture a few blocks below the build limit for the interactions. Every block the benchmark
 * places goes through {@link #place}, which records what was there, and {@link #close} puts it all back.
 * Every simulated player is created here too, so {@link #releasePlayers} can make vanilla forget them.
 */
final class BenchmarkWorld {
    /** Chunks either side of the area's centre chunk kept loaded, so no read ever loads a chunk mid-measurement. */
    private static final int CHUNK_RADIUS = 2;
    /** Players stand within this many blocks of the centre, all inside the forced chunks. */
    private static final int SPREAD = 24;
    /** Centre chunks to try, in order; the first one that suits the benchmark wins. */
    private static final int[][] AREA_CHUNKS = areaChunks();
    /** Candidate centres are this many chunks apart, and this many rings of them are tried around (0, 0). */
    private static final int AREA_STEP = 32;
    private static final int AREA_RINGS = 4;
    /** Block update plus client notification, the same flags the mod's cauldron transfer uses. */
    private static final int BLOCK_UPDATE_FLAGS = 3;

    final MinecraftServer server;
    final ServerLevel level;
    private final List<int[]> forcedChunks = new ArrayList<>();
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private final List<BenchmarkPlayer> players = new ArrayList<>();
    private final Set<UUID> createdPlayers = new LinkedHashSet<>();
    private int chunkX;
    private int chunkZ;
    private BlockPos water;
    private BlockPos coveredWater;
    private BlockPos cauldron;
    private String description = "not set up";

    BenchmarkWorld(MinecraftServer server) {
        this.server = server;
        this.level = server.overworld();
    }

    /** Picks an area outside spawn protection, force-loads it and builds the water fixture. */
    void open() {
        chooseArea();
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                // Only chunks this run forced are released again; one the world already forces stays forced.
                if (level.setChunkForced(x, z, true)) forcedChunks.add(new int[] {x, z});
                level.getChunk(x, z);
            }
        }
        buildFixture();
        BlockPos field = new BlockPos(centerX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, centerX(), centerZ()), centerZ());
        description = "chunks " + (chunkX - CHUNK_RADIUS) + ".." + (chunkX + CHUNK_RADIUS)
                + " x " + (chunkZ - CHUNK_RADIUS) + ".." + (chunkZ + CHUNK_RADIUS)
                + ", players in " + biomeAt(field) + ", water fixture at " + water.toShortString()
                + " in " + biomeAt(water);
    }

    BlockPos water() {
        return water;
    }

    /** A water source with a block over it, so no sky above. */
    BlockPos coveredWater() {
        return coveredWater;
    }

    BlockPos cauldron() {
        return cauldron;
    }

    String describe() {
        return description;
    }

    /** Puts the water source back, for example after a bucket scooped it up. */
    void refillWater() {
        if (!level.getBlockState(water).is(Blocks.WATER)) {
            level.setBlock(water, Blocks.WATER.defaultBlockState(), BLOCK_UPDATE_FLAGS);
        }
    }

    /** Empties the cauldron's stored quality, so the next pour changes the block again. */
    void resetCauldron() {
        level.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), BLOCK_UPDATE_FLAGS);
    }

    /** The simulated player with this index, created on first use and reused by later scenarios. */
    BenchmarkPlayer player(int index) {
        while (players.size() <= index) players.add(extraPlayer(players.size()));
        return players.get(index);
    }

    /** A new simulated player that is not shared between scenarios. Still forgotten by {@link #releasePlayers}. */
    BenchmarkPlayer extraPlayer(int index) {
        BenchmarkPlayer player = new BenchmarkPlayer(level, index);
        createdPlayers.add(player.getUUID());
        return player;
    }

    /** Stands a player on the surface, spread across the forced chunks so they sample different columns. */
    void standInField(Player player, int index) {
        int x = centerX() + Math.floorMod(index * 7, SPREAD * 2) - SPREAD;
        int z = centerZ() + Math.floorMod(index * 13, SPREAD * 2) - SPREAD;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        player.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
    }

    /** Above the water source, looking straight down at it, within reach of bottles and buckets. */
    void standAboveWater(Player player) {
        player.snapTo(water.getX() + 0.5, water.getY() + 2.0, water.getZ() + 0.5, 0.0F, 90.0F);
    }

    Holder<Enchantment> enchantment(ResourceKey<Enchantment> key) {
        return server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /**
     * Forgets the simulated players, including the stats and advancement progress vanilla created for each
     * of them in the player constructor. {@code PlayerList#remove} does this for a player who disconnects, but
     * it also saves their data and announces the departure, and nothing public does only the forgetting.
     * Advancement progress costs hundreds of kilobytes a player, so without this a stress run would hold on to
     * most of a gigabyte until the server stops.
     *
     * <p>The field and method names are the Mojang names the dev environment runs with on every supported
     * version. Vanilla unregisters the advancement listeners with {@code clearTriggers} from 26.2 on and
     * {@code stopListening} before, so whichever exists is used.
     */
    void releasePlayers() {
        players.clear();
        if (createdPlayers.isEmpty()) return;
        try {
            PlayerList list = server.getPlayerList();
            Map<?, ?> stats = privateMap(list, "stats");
            Map<?, ?> advancements = privateMap(list, "advancements");
            Method unregister = advancementCleanup();
            if (unregister == null) {
                BenchmarkRunner.LOGGER.warn("[ThirstBenchmark] no advancement cleanup method found; listeners may stay registered");
            }
            for (UUID id : createdPlayers) {
                if (list.getPlayer(id) != null) continue;
                Object progress = advancements.remove(id);
                if (progress != null && unregister != null) unregister.invoke(progress);
                stats.remove(id);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            BenchmarkRunner.LOGGER.warn("[ThirstBenchmark] could not drop vanilla's caches for the simulated players", e);
        }
        createdPlayers.clear();
    }

    /** Restores every block and releases every chunk and player this run touched. Safe to call more than once. */
    void close() {
        releasePlayers();
        List<Map.Entry<BlockPos, BlockState>> placed = new ArrayList<>(originals.entrySet());
        for (int i = placed.size() - 1; i >= 0; i--) {
            level.setBlock(placed.get(i).getKey(), placed.get(i).getValue(), BLOCK_UPDATE_FLAGS);
        }
        originals.clear();
        for (int[] chunk : forcedChunks) level.setChunkForced(chunk[0], chunk[1], false);
        forcedChunks.clear();
    }

    private void chooseArea() {
        BenchmarkPlayer probe = player(0);
        boolean anyUnprotected = false;
        for (int[] candidate : AREA_CHUNKS) {
            chunkX = candidate[0];
            chunkZ = candidate[1];
            // Spawn protection would silently refuse the bottle and bucket fills.
            if (server.isUnderSpawnProtection(level, new BlockPos(centerX(), 0, centerZ()), probe)) continue;
            anyUnprotected = true;
            // Sea water carries no grade and never hydrates, so over an ocean or a beach the fills have
            // nothing to stamp and the drinks restore nothing. Reading the biome does not generate a chunk.
            if (!salty(new BlockPos(centerX(), topY(), centerZ()))) return;
        }
        throw new IllegalStateException(anyUnprotected
                ? "Every candidate benchmark area is an ocean or a beach; the interactions need fresh water"
                : "Every candidate benchmark area is inside spawn protection; set spawn-protection=0 in server.properties");
    }

    private boolean salty(BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
    }

    /** (0, 0) first, then rings of candidates {@link #AREA_STEP} chunks apart, nearest ring first. */
    private static int[][] areaChunks() {
        List<int[]> chunks = new ArrayList<>();
        chunks.add(new int[] {0, 0});
        for (int ring = 1; ring <= AREA_RINGS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) chunks.add(new int[] {dx * AREA_STEP, dz * AREA_STEP});
                }
            }
        }
        return chunks.toArray(int[][]::new);
    }

    private void buildFixture() {
        int x = centerX();
        int z = centerZ();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos base = null;
        for (int y = topY() - 4; y > surface + 2; y--) {
            if (isClear(x, y, z)) {
                base = new BlockPos(x, y, z);
                break;
            }
        }
        if (base == null) throw new IllegalStateException("No clear space above " + x + ", " + z + " for the water fixture");

        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int dx = -2; dx <= 4; dx++) {
            for (int dz = -2; dz <= 2; dz++) place(base.offset(dx, -1, dz), stone);
        }
        // The source sits in a one-block ring, so it has nowhere to flow.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) place(base.offset(dx, 0, dz), stone);
            }
        }
        // Mud and a composter inside the 5x3x5 sampling neighbourhood take both pollution branches.
        place(base.offset(-1, 0, -1), Blocks.MUD.defaultBlockState());
        place(base.offset(1, 0, 1), Blocks.COMPOSTER.defaultBlockState());
        water = base;
        place(water, Blocks.WATER.defaultBlockState());
        // A second source under a roof, for sampling water the sky does not reach. Walled in on the sides
        // the floor and the cauldron leave open, so it has nowhere to flow either.
        coveredWater = base.offset(3, 0, -1);
        place(base.offset(2, 0, -1), stone);
        place(base.offset(4, 0, -1), stone);
        place(base.offset(3, 0, -2), stone);
        place(coveredWater.above(), stone);
        place(coveredWater, Blocks.WATER.defaultBlockState());
        cauldron = base.offset(3, 0, 0);
        place(cauldron, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL));
    }

    private boolean isClear(int x, int y, int z) {
        for (int dx = -2; dx <= 4; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (!level.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).isAir()) return false;
                }
            }
        }
        return true;
    }

    private void place(BlockPos pos, BlockState state) {
        originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
        level.setBlock(pos, state, BLOCK_UPDATE_FLAGS);
    }

    /** The highest buildable y. 1.21.1 only exposes the exclusive build limit, one above it. */
    private int topY() {
        //? if >1.21.1 {
        return level.getMaxY();
        //?} else
        //return level.getMaxBuildHeight() - 1;
    }

    private String biomeAt(BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
    }

    private int centerX() {
        return (chunkX << 4) + 8;
    }

    private int centerZ() {
        return (chunkZ << 4) + 8;
    }

    private static Map<?, ?> privateMap(PlayerList list, String name) throws ReflectiveOperationException {
        Field field = PlayerList.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<?, ?>) field.get(list);
    }

    private static Method advancementCleanup() {
        for (String name : new String[] {"clearTriggers", "stopListening"}) {
            try {
                return PlayerAdvancements.class.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Try the name the other Minecraft versions use.
            }
        }
        return null;
    }
}
