package com.thirstwastaken2.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An item that is drunk: the filled bowl and the waterskin.
 *
 * <p>From 1.21.2 drinking is a data component, so on those versions this is a plain item with that
 * component added. Before it an item had to drink by overriding its use, its animation, its duration
 * and what finishing does, which is what the older branch below does. Callers and subclasses see the
 * same class on every version.
 */
public class DrinkItem extends Item {
    /** Left in the hand once the drink is finished, or {@code null} when the item itself stays. */
    private final Item emptyContainer;

    public DrinkItem(Properties properties, Item emptyContainer) {
        super(drinkable(properties, emptyContainer));
        this.emptyContainer = emptyContainer;
    }

    /** Whether there is anything in this stack to drink. Nothing starts drinking while it is false. */
    protected boolean canDrink(ItemStack stack) {
        return true;
    }

    /**
     * What vanilla does when a drink is finished, other than using the stack up: the statistic, the
     * advancement trigger, the game event, and from 1.21.2 the sound. For items that decide for
     * themselves how much drinking removes.
     */
    protected static void drinkEffects(ItemStack stack, Level level, LivingEntity entity) {
        //? if >=1.21.2 {
        var consumable = stack.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
        // Consumable#onConsume is hard-wired to shrink its input, so it runs against a disposable copy.
        if (consumable != null) consumable.onConsume(level, entity, stack.copy());
        //?} else {
        /*if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            net.minecraft.advancements.triggers.CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));
        }
        entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.DRINK);
        *///?}
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return canDrink(player.getItemInHand(hand)) ? super.use(level, player, hand) : InteractionResult.PASS;
    }

    private static Properties drinkable(Properties properties, Item emptyContainer) {
        properties.component(net.minecraft.core.component.DataComponents.CONSUMABLE,
                net.minecraft.world.item.component.Consumables.DEFAULT_DRINK);
        return emptyContainer == null ? properties : properties.usingConvertsTo(emptyContainer);
    }
    //?} else {
    /*private static Properties drinkable(Properties properties, Item emptyContainer) {
        return properties;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return canDrink(stack)
                ? net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand)
                : net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    // The drinking animation is also what makes LivingEntity play the drinking sound while it runs.
    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        drinkEffects(stack, level, entity);
        if (emptyContainer != null && entity instanceof Player player) {
            // Shrinks the stack, and hands the empty container back or into the inventory.
            return net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(emptyContainer));
        }
        if (!(entity instanceof Player player) || !Vanilla.hasInfiniteMaterials(player)) stack.shrink(1);
        return stack.isEmpty() && emptyContainer != null ? new ItemStack(emptyContainer) : stack;
    }
    *///?}

    // How long a drink takes, the config's plainWaterDrinkTicks, 32 like a potion unless changed. From
    // 1.21.2 the consumable component holds a duration too, which this overrides; 1.20.5 handed the use
    // duration the entity using the item.
    //? if >=1.20.5 {
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return com.thirstwastaken2.config.ThirstConfig.get().plainWaterDrinkTicks;
    }
    //?} else {
    /*@Override
    public int getUseDuration(ItemStack stack) {
        return com.thirstwastaken2.config.ThirstConfig.get().plainWaterDrinkTicks;
    }
    *///?}
}
