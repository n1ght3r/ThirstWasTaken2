package com.thirstwastaken2.gametest;

import com.thirstwastaken2.damage.ThirstDamageTypes;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * The datapack entries and version-specific vanilla calls the mod depends on.
 *
 * <p>These break silently rather than loudly. A damage type that fails to load is quietly replaced
 * by vanilla starvation, and a tag file that does not apply leaves dehydration soaking into armour.
 * Both would look like a balance complaint rather than a bug.
 *
 * <p>These tests check that the entries loaded, not that vanilla then applies them. Mock players in
 * a game test report creative mode and vanilla refuses to damage them, so actually losing health to
 * dehydration stays a manual check. See docs/dev/MANUAL-TESTING.md.
 */
public final class EnvironmentGameTest {
    @GameTest
    public void dehydrationLoadsItsDamageTypeAndTags(GameTestHelper helper) {
        DamageSource source = ThirstDamageTypes.dehydration(helper.getLevel());

        TestFixtures.check(helper, source.is(ThirstDamageTypes.DEHYDRATE),
                "the dehydrate damage type should load from the datapack rather than falling back "
                        + "to starvation, got " + source.getMsgId());
        TestFixtures.check(helper, source.is(DamageTypeTags.BYPASSES_ARMOR),
                "the mod's bypasses_armor tag entry should have loaded, so armour cannot soften "
                        + "dehydration");
        // The no_knockback tag arrived with 1.20.2; before it no damage type knocks back without an attacker.
        //? if >=1.20.5 {
        TestFixtures.check(helper, source.is(DamageTypeTags.NO_KNOCKBACK),
                "the mod's no_knockback tag entry should have loaded, so dehydration does not "
                        + "knock the player back");
        //?}
        TestFixtures.check(helper, source.is(DamageTypeTags.NO_IMPACT),
                "the mod's no_impact tag entry should have loaded, so dehydration hurts the way "
                        + "drowning does");
        helper.succeed();
    }

    @GameTest
    public void overworldWaterDoesNotEvaporate(GameTestHelper helper) {
        BlockPos here = helper.absolutePos(new BlockPos(2, 2, 2));

        TestFixtures.check(helper, !Vanilla.waterEvaporates(helper.getLevel(), here),
                "the overworld is not Nether-like, so thirst should not drain at the Nether rate");
        helper.succeed();
    }
}
