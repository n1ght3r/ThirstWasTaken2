package com.thirstwastaken2.immersiveengineeringforge;

import java.util.function.Supplier;

/**
 * Whether the server thread is inside one of Immersive Engineering's own transfers between a tank and a
 * container. Loaded only from the mixins, which only apply with the mod installed.
 */
public final class ImmersiveEngineeringForgeWater {
    /**
     * How deep the thread is inside {@code FluidUtils}' transfers. Only there does
     * {@code ForgeFluidUtilMixin} wrap a container's handler, so no other mod's use of Forge's
     * {@code FluidUtil} changes.
     */
    private static final ThreadLocal<int[]> TRANSFER = ThreadLocal.withInitial(() -> new int[1]);

    private ImmersiveEngineeringForgeWater() { }

    /** Runs {@code transfer} with containers' handlers carrying the grade; see {@link #TRANSFER}. */
    public static <T> T graded(Supplier<T> transfer) {
        int[] depth = TRANSFER.get();
        depth[0]++;
        try {
            return transfer.get();
        } finally {
            depth[0]--;
        }
    }

    public static boolean inTransfer() {
        return TRANSFER.get()[0] > 0;
    }
}
