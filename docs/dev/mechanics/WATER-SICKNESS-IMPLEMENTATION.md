# Water sickness: implementation

How to build [WATER-SICKNESS.md](WATER-SICKNESS.md). That page owns every number and behaviour;
this one owns where the code goes. When they disagree, the design page wins and this one is fixed.

## Rules for every step

- **Effects are markers.** Never override `applyEffectTick`: its signature differs between 1.21.1
  and later. Behaviour lives in `ThirstManager.tickPlayer`, from one `getEffect` lookup per effect,
  and the tick's fast path allocates nothing. Watch the benchmark (`src/dev/java/AGENTS.md`).
- **Register** through `Loader.onRegister(Registries.MOB_EFFECT, ...)` in `ThirstEffects`, next to
  `PARCHED`. `MobEffect`'s constructor is protected: a small subclass, no overrides.
  `Registry.registerForHolder` gives the `Holder<MobEffect>`. Version forks go in `platform/Vanilla`.
- **Drains** are a rate per tick per level added to `raw` in `tickPlayer`, like `PARCHED_EXHAUSTION`,
  never a multiplier on the whole drain.
- **Lang** in all nine files: `en_us`, `vi_vn`, `fr_fr`, `ja_jp`, `ko_kr`, `pl_pl`, `ru_ru`, `zh_cn`,
  `zh_tw`.
- **Config** fields in `ThirstConfig` with clamping in its validation, sliders in
  `client/config/ConfigCategory`. The config has no migration: dropped keys fall back to defaults,
  and the CHANGELOG says so.
- **Divergence from the original mod** gets a comment where it happens, as `../../../AGENTS.md` asks.
- **Verify** every step with `runGametest` on every node, and `runClient` once per loader for anything
  visible. Update `../MANUAL-TESTING.md` for anything only a person can check.
- **Docs** for players are written once per release with the `write-docs` skill:
  `docs/features/water-purity.md`, the grade tables in `MODRINTH.md` and `CURSEFORGE.md`, and
  `CHANGELOG.md`.

## Where things are today

| What | Where |
|---|---|
| The drink roll | `effect/WaterSickness`, called by `WaterPurity.applyEffects` |
| Thirst drain per tick | `ThirstManager.tickPlayer`: `NAUSEA_EXHAUSTION = 0.06`, `PARCHED_EXHAUSTION = 0.01`, Hunger refund |
| Dehydration damage | `ThirstManager.tickPlayer`, every `DAMAGE_INTERVAL = 40` ticks at zero thirst |
| Effects | `ThirstEffects.PARCHED`, `ThirstEffects.UPSET_STOMACH` |
| Chance tables | `ThirstConfig.sicknessEffects`, `config/SicknessEffect` lines by difficulty and grade, each rolling on its own in `WaterSickness.drink`, edited on the Sickness page by `client/config/SicknessRows`. `SicknessTable` and `sicknessPreset` were removed for it; see the note under step 4 |
| Effect tests | `WaterEffectsGameTest`, `UpsetStomachGameTest`, `WaterSicknessGameTest` in `src/gametest/java/com/thirstwastaken2/gametest/` |
| HUD | `client/ThirstHud`, registered per loader in `client/platform/ClientLoader` |
| Icons | `textures/mob_effect/parched.png`; `upset_stomach.png` from `tools/generate_upset_stomach_icon.py` |

**Check before step 5:** dehydration damage on Normal tests `health > 0.0F`, where vanilla
starvation tests `health > 1.0F`, so an empty bar can kill on Normal. Dysentery empties the bar on
Normal, where the design says it stops at half a heart. Decide whether the existing check is a bug.

## Done: 1a-1d

- **1a** `applyEffects` no longer applies Hunger. The Hunger refund in `tickPlayer` stays: Hunger from
  food must not dehydrate.
- **1b** `ThirstEffects.PARCHED`, 0.01 exhaustion per tick per level; the thirst bar swaps to
  `thirst_icons_parched.png` while it lasts (`client/AGENTS.md`). Agent-client script
  `tools/agent/gameplay/parched.jsonl`.
- **1c** Salt water adds Parched II for 30 s, without particles.
- **1d** `nauseaSeconds` by grade (12 / 8 / 5 / 5); drinking by hand restores 2 thirst and 2 quenched.

Note: `tools/generate_parched_icons.py` is referenced in `client/AGENTS.md` and
`src/main/resources/AGENTS.md` but is not in `tools/`.

## Done: 2

- **2** `ThirstEffects.UPSET_STOMACH`, numbers in `effect/UpsetStomach`. Nausea bursts only roll when
  the player is not already nauseous. `FoodDataMixin` reads the saturation multiplier at the head of
  `FoodData#tick`, since `FoodData` has no player, and applies it in `add`. The thirst bar turns green
  (`thirst_icons_upset_stomach.png`, from `tools/generate_upset_stomach_bar.py`), ahead of Parched.
  Tests in `UpsetStomachGameTest`.
- **Nausea has to last to be seen.** Vanilla fades Nausea in over 150 ticks and starts fading it out
  60 ticks before it ends (`setBlendDuration(150, 20, 60)` on 26.2; the same ramp by hand on 1.21.1),
  so 3 s or less shows nothing. The planned 3 s bursts and 2 s taste were invisible in game. Bursts
  are 10 s, the taste 7 s and sea water's Nausea 8 s. Nausea's own drain is not charged while the
  player has Upset Stomach, whose drain went from 0.005 to 0.008 per level to pay for its bursts, so
  level I still costs about 2.4 thirst a minute.

## Done: 3 and 4

- **3** Poisoning is `WaterSickness.poisoning(difficulty)`, plain vanilla effects, given with Upset
  Stomach. Its durations and levels are constants there, not config.
- **4** `WaterPurity.applyEffects` hands fresh water to `effect/WaterSickness` and always quenches.
  `quenchWhenDebuffed`, `nauseaChance`, `poisonChance` and `nauseaSeconds` are gone; the config has
  only `sicknessPreset` (`REALISTIC`, `CLASSIC`). The chances are fixed in `config/SicknessTable`, one
  per difficulty, of `poisoningChance`, `upsetStomachChance` and `upsetStomachLevel` for Dirty, Murky
  and Clean. They were config pages for a while and were fixed to keep the screen short. The chances
  are whole percents: nothing before Dysentery needs a fraction.
  **Later replaced:** the presets, `SicknessTable`, `WaterSickness.poisoning` and the one-roll walk
  are gone. `ThirstConfig.sicknessEffects` lists every effect per difficulty and grade, any effect id,
  each rolling on its own; its defaults are the fixed tables above with Poisoning's chance added to
  Upset Stomach's. The steps below describe the code as it was built.
- **Choices made here:**
  - Dysentery has no range yet, so the walk is Poisoning then Upset Stomach, and a Dirty drink on
    Normal makes the player ill 75% of the time instead of 80%. Step 5 adds `dysenteryChance` in front.
  - The current illness is read from the effects: Upset Stomach with Poison is Poisoning.
  - Poisoning dropped Weakness, Mining Fatigue and Slowness after play testing; Poison lasts longer
    instead (10 / 20 / 30 s).
  - Upset Stomach from a drink shows its particles; Parched from salt water does not.
  - `CLASSIC` is the roll from before the rework with its old defaults, not configurable.
- Tests: `WaterSicknessGameTest` forces the roll into every range of every table.

## Step 5: Dysentery

- `ThirstEffects.DYSENTERY`, harmful, `setBlendDuration(22)` like Darkness (needed by 5b).
- The 10 s start: a hidden short effect that turns into Dysentery when it runs out, or a timer in
  `ThirstData`. Choose here; step 6 reuses it. The hidden effect is cleared by milk and its expiry
  hook differs between versions; the `ThirstData` field needs a codec and sync change.
- Action bar message and sound at the start.
- `tickPlayer`: 0.03 exhaustion per tick. On the slow tick, while thirst ≤ 10, 1 damage every 10 s.
- Death: when `sicknessCanKill` is on (default) and the difficulty is Hard, fever and dehydration
  damage may kill; otherwise both stop at 1 health while Dysentery lasts.
- Damage type `thirstwastaken2:dysentery` next to `ThirstDamageTypes.dehydration`, death message in
  nine lang files.
- Milk must not clear it: NeoForge through `EffectCure`; Fabric a mixin on the milk bucket. The totem
  works as usual.
- Icon to draw: the Upset Stomach stomach, darker.
- Lang: `effect.thirstwastaken2.dysentery` ("Dysentery", vi "Kiết lỵ").
- Gametests: drain; fever only at thirst ≤ 10; no death on Normal; death on Hard; `sicknessCanKill`
  off stops at 1 health on Hard, dehydration included; milk leaves it in place.
- Manual: catch it on Hard, survive by drinking, die of it once, then turn the option off and check
  it stops at half a heart.

## Step 5b: sick vision

Vanilla Darkness, read from the decompiled 1.21.1 and 1.21.11 sources:

- **Light pulse**, `LightTexture`: `max(0, cos(tickCount × π × 0.025) × 0.45 × blend)` times
  `darknessEffectScale`, taken off the lightmap. 4 s cycle.
- **Fog**, 1.21.1 `FogRenderer.DarknessFogFunction`, 1.21.6+ `DarknessFogEnvironment`: fog lerped to
  15 blocks by the blend. Not used for now.
- Both read `MobEffects.DARKNESS` by name. Never copy Mojang code: feed values into it.

**Light pulse for Dysentery.** A client mixin where `LightTexture` reads Darkness's blend returns
`max(darknessBlend, SickVision.lightPulse(player, partialTick))`, which is Dysentery's blend × 0.6
above half thirst and × 1.0 at or below. MixinExtras is already a dependency.

| Versions | Target | Mixin |
|---|---|---|
| 1.21.1 | private `getDarknessGamma(float)` | `@ModifyReturnValue` |
| 1.21.11 | `player.getEffectBlendFactor(MobEffects.DARKNESS, …)` in `updateLightTexture` | `@ModifyExpressionValue` |
| 26.1, 26.2, 26.3 | not checked; the lightmap may have moved | decide at the start |

Split the mixin by version the way `LocalPlayerMixin` is (see `client/AGENTS.md`).

**HUD vignette** for Poisoning, the fever dip and Hard at ≤ 2 hearts:

- `textures/misc/sick_vision.png`, 256x256 radial alpha, from `tools/generate_sick_vision.py`,
  stretched and tinted black. Pulse with the same cosine as Darkness.
- A second HUD layer in `ClientLoader`, under the other HUD elements: Fabric 1.21.6+
  `HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, ...)`, NeoForge
  `RegisterGuiLayersEvent` above `VanillaGuiLayers.CAMERA_OVERLAYS`, Fabric 1.21.1 the existing
  `GuiMixin` next to vanilla's vignette. Record it in `client/AGENTS.md`.
- Not drawn with F1, in spectator, or behind a screen other than chat.
- The fever dip watches the player's hurt time while Dysentery is present. No new packet: effects
  and `ThirstData` are already synced to the owner.

**Shared:**

- `SickVision` next to `ThirstHud`, pure functions from (effects, thirst, health, difficulty, time)
  to (light pulse, vignette alpha), unit tested without a client.
- Both parts scale by `darknessEffectScale`; `sickVision` in the HUD config section turns both off.
- Manual: each state by eye, next to a real Warden's Darkness; slider at 0 turns both off.

**If fog is added later:** 1.21.6+ subclass `DarknessFogEnvironment` overriding `getMobEffect()` and
add it to `FogRenderer`'s list by mixin; 1.21.1 a `MobEffectFogFunction` in `FogRenderer`'s list;
NeoForge `ViewportEvent.RenderFog`.

## Step 6: incubation

Extends step 5's pending-sickness mechanism to Upset Stomach and Poisoning, with a 1-3 minute delay
and an action bar warning shortly before.

## Step 7: salt and oral rehydration salts

- **Salt** item, left when sea water boils away in a hanging pot or a heated cauldron. Design with the
  distillation idea in [ROADMAP.md](ROADMAP.md).
- **Oral Rehydration Salts**, Pure water + sugar + salt: more thirst than a bottle, halves the time
  left on Dysentery and Upset Stomach, pauses the fever for 60 s. One grey tooltip line in
  `ThirstTooltip`.
- Recipes and models through datagen, textures, an advancement for surviving Dysentery.
- Ships with step 5.

## Step 8: toxins

Poisoning likelier from water sampled in the `stagnant_water` tag or hot biomes; boiling does not
clear it, charcoal does. Touches the whole purification system: read
[WATER-REFERENCE.md](WATER-REFERENCE.md) first.
