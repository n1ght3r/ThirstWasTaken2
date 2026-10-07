# Purification and drinking balance

**Status: implemented** (2026-10-06), uncommitted; balance defaults still need the playtests under
[Validation and tuning](#validation-and-tuning). [WATER-REFERENCE.md](WATER-REFERENCE.md) describes the
game as built. Three choices below were taken back during implementation, at the maintainer's call,
and are marked where they stand: **water bottles do not stack**, **drinking by hand stays a sip per
click**, and **no water sprite is recoloured**. The Waterskin's recipe became four leather, a choice
of the maintainer's rather than the cost table's.

## Treatment rules

Progression: collect water, boil to **Clean**, then distil to **Pure**.

| Method | Result | Salt water |
|---|---|---|
| Copper Canteen, Iron Flask, Hanging Pots, Farmer's Delight Cooking Pot | Dirty or Murky to Clean | Never becomes fresh |
| Furnace and smoker | Clean, only with `enableFurnaceBoiling` enabled | Refused |
| Cold Sweat Boiler | +1 grade per pass, capped at Clean | Refused |
| Cold Sweat Waterskin on a campfire | Clean | Refused |
| Copper Distiller | Pure from any input | Pure |
| Create / Create Fly Sand Filter | +1 grade per pass, capped at Pure | Stays salty |
| Tanks, pumps, jars, Spout, Item Drain | Preserve quality | Stays salty |

- Heat never lowers quality or reboils Clean or Pure water.
- `enableFurnaceBoiling` defaults to **true**. Furnaces and smokers give players a basic treatment
  route before metal vessels. Turning it off removes these recipes. No water cooking recipes in
  vanilla campfire slots.
- This mod's Waterskin cannot boil. Cold Sweat's campfire treatment is an integration exception;
  remove its furnace and smoker recipes.
- Keep natural Pure sources: exposed cold mountain water above y 100, Spelunkery Spring Water, loot
  and dripstone cauldrons. The Sand Filter is the processing exception to distillation.
- Ordinary world water without sky exposure at the sampled surface is **at most Murky**; worse
  water remains Dirty. This includes cave pools and covered player-made refill points. Remove the
  deep-aquifer quality bonus. Check salt and explicit `pure_water` fluids first, and preserve quality
  stored in cauldrons/pots/tanks. Finding a tagged spring is a real exception, not every cave pool.

## Names and colours

Use **water quality** in player-facing text. Grades: **Dirty, Murky, Clean, Pure**; salt is separate.
Pure is a game grade, not a claim that distilled water is healthier than other safe water.

| Grade | Tooltip |
|---|---|
| Dirty | `0xB0632E` |
| Murky | `0xBDB878` |
| Clean | `0x74B8E0` |
| Pure | `0x4FD6FF` |
| Salt | `0xE6DFC8` |

Keep Clean's existing tooltip colour (`0x74B8E0`). Check Clean versus Pure in game, including
colour-blind readability and tooltip contrast.

**Taken back:** the bowl and hanging-pot water sprites were recoloured to a new palette and then put
back. They keep their original colours; only the tooltip colours above changed (Murky's).

| Rename | Target |
|---|---|
| `thirst.purity.dirty`, `.slightly_dirty`, `.acceptable`, `.purified` | `thirst.water.dirty`, `.murky`, `.clean`, `.pure` |
| `default_purity`, `rainwater_purity`, `dripstone_purity` lang keys | `*_quality`, labels ending in "Quality" |
| `defaultPurity`, `rainwaterPurity`, `dripstonePurity` config fields / JSON keys | `defaultQuality`, `rainwaterQuality`, `dripstoneQuality` |
| Sophisticated `upgrades.buttons.min_purity` | `min_quality`, "Drinks %s water or better" |
| Jade and boiling message | "Water Quality", "The water has boiled" |

Keep persisted and public identifiers: `water_purity`, `water_salty`, the `purity` blockstate,
`purified_water`, Jade plugin id, `drink_min_purity`, `pure_water` and API names. Keep the grade name
Clean, the sickness key `clean` and the lang key `quenched_percent_clean`.
Migrate the three renamed JSON keys on load: use the legacy value only if the new key is absent,
preserve custom values, and write only the new keys. No sickness grade-key migration is needed.

Player-facing explanations distinguish boiling from removal of dissolved contaminants. Quality
bonuses and sand filtration to Pure are gameplay allowances, not health claims.
Reference: [CDC water treatment](https://www.cdc.gov/drinking-water/about/about-home-water-treatment-systems.html)
and [EPA emergency disinfection](https://www.epa.gov/ground-water-and-drinking-water/emergency-disinfection-drinking-water).

## Design targets

- Thirst is the primary survival constraint on a mining trip. Food alone cannot sustain healing
  without a water reserve, and untreated cave water must not be a profitable substitute for boiling.
- Water preparation should matter before a trip; drinking should not interrupt ordinary activity
  every few seconds. Target roughly 60 to 120 s between drinks during ordinary active Overworld
  play, with longer gaps while building and shorter ones during sprint-jumping or combat.
- Boiling completes the everyday safety task. Pure improves supplies and supports sea-based living;
  it is an optional investment, not a cure for random punishment from correctly boiled water.
- One serving restores the same amount in every drinking container. Materials buy storage and
  treatment. Early items can be replaced; they need not compete equally with iron forever.
- Unlimited world water is part of Minecraft. Balance preparation, carrying and treatment, not
  scarcity of water blocks. Closed-container transfers must conserve water and quality.

## Player mechanics

### Recovery and quality

Every full serving of plain fresh water restores **6 thirst and 4 base quenched** in **32 ticks**.
This applies to bottles, bowls and all three carried vessels. Potions retain their own values.

| Grade | Quenched multiplier | Actual thirst + quenched | Purpose |
|---|---|---|---|
| Dirty | 0% | 6 + 0 | Emergency water |
| Murky | 25% | 6 + 1 | Risky untreated water, not a routine mining supply |
| Clean | 50% | 6 + 2 | Safe everyday water |
| Pure | 100% | 6 + 4 | Safe water with a larger reserve |

Keep integer rounding down, thirst 0 to 20 and quenched capped at current thirst. Plain water normally
requires missing thirst; Clean/Pure can also be drunk at full thirst when quenched is below 20, to
prepare a reserve or pay for healing. Dirty/Murky and hand drinking cannot use that exception.
**Discard thirst overflow from all hydration sources**, including food, milk and flavoured drinks:
otherwise topping up near full creates quenched without paying the source's reserve value.
Tooltips preview the actual gain, including the cap and sickness reductions.
Drinking when six points are missing makes full use of a serving; early top-ups are the player's choice.

Pure gives 25% more total hydration per serving than Clean (10 versus 8), not twice the travel time.
That comparison assumes room for both gains and excludes healing, illness and other food or drink.

**Taken back: drinking by hand stays as it was**, a sip of 3 thirst and 2 base quenched cut by the
grade, one per click, with its sickness roll. The timed drink below was built and then removed.

~~**Drinking by hand:** 6 thirst, zero quenched, 32 uninterrupted ticks, one sickness roll on completion.
Cancel on moving out of reach, losing the water target or releasing use. No immediate gain on click,
no extra offhand sip, and no thirst overflow. This remains a free local fallback without instant
combat recovery or a reason to carry no water. It does not consume the world source.~~

### Health and drain

**Taken back: healing.** The gate and the 25% reserve bonus below were built, then replaced at the
maintainer's call by something close to the original's independent heal: food heals while the thirst
bar is at `foodHealMinThirstPercent` (50%) or more; quenched heals on its own, at `quenchedHealthRegen`
(0.5) of saturation's speed, with a full thirst bar, the food bar at `quenchedHealMinFoodPercent` (50%)
or more and **only once saturation is spent**, so the two take turns; Upset Stomach stops both. The
healing bullets below are the superseded design; the drain ones stand.

- Natural food healing requires **20 thirst, quenched above zero, and no Upset Stomach**. There is
  no slow-heal exception below those thresholds. Apply this gate to both saturation and hunger
  healing. Refund food exhaustion for blocked
  heals; do not charge thirst for refunded healing. Keep `naturalRegeneration` and normal food
  requirements. Disabling dehydration's healing restriction removes its thirst/reserve gate;
  illness has a separate configurable healing restriction.
- Reward a prepared reserve: at **20 thirst and at least 6 quenched, without Upset Stomach**, each
  successful food-based heal adds **25% of the health actually restored by that base heal**. At 1 to
  5 quenched, eligible healing is normal. Check the reserve before the base heal. Clean and Pure
  qualify equally; Pure supplies the reserve with fewer servings.
- `quenchedHealthRegen` now controls this bonus fraction, default **0.25**, with 0 disabling the
  bonus. Replace the independent timed water heal entirely; never run both systems. Update its
  config description and reset value, and document the changed meaning for existing configs.
- Limit the bonus to remaining missing health and charge **6 thirst exhaustion per actual bonus
  HP**, once, outside climate/global multipliers and activity suppression. Carry fractional cost
  through exhaustion. A base heal of 1 HP gives at most 0.25 bonus HP for 1.5 exhaustion (0.375
  quenched points). The base heal keeps its ordinary food cost and mirrored thirst cost. Do not
  charge bonus cost for overhealing or a cancelled bonus; do not charge a second food cost for it.
- No successful base heal means no bonus: hunger, blocked regeneration or a cancelled heal cannot
  be bypassed by water. Apply the bonus only to the two natural food-healing paths, with no extra
  timer or recursive bonus. Its full-thirst, reserve and no-Upset conditions still apply when the
  base healing restrictions are disabled.
- Instant Health, Regeneration effects, absorption and totems remain emergency resources. Do not
  intercept every `heal` call or silently disable other mods' healing; ordinary eating alone is
  what must fail to bypass illness. Explain the blocked-healing condition in the effect tooltip.
- Sprint remains blocked at 6 or below. Keep the existing dehydration damage and Peaceful recovery.
- Add baseline depletion of **one hydration point per minute**, plus activity exhaustion, at four
  exhaustion per point. It applies in survival while enabled, except automatic-refill Peaceful;
  excludes creative/spectator. Mining, building and waiting in a cave all consume water. Successful
  food-based healing still consumes thirst exhaustion, so a full food bar cannot heal indefinitely.
- Make the climate curve monotonic: a slightly warmer biome must not suddenly drain less. Proposed
  Overworld factor: `clamp(0.8 + 0.25 * (temperature - 0.5) + (dry ? 0.15 : 0), 0.65, 1.35)`.
  `temperature` is biome base temperature or the Cold Sweat adapter's equivalent, without the old
  `+0.2` transform; `dry` follows precipitation, including the season integration.
- Apply seasons only without a measured Cold Sweat temperature, then clamp the Overworld factor
  to 0.6 to 1.5. Nether/evaporating dimensions use **3.0** instead. Set the global depletion default
  to **1.2**, multiplying either result. Climate and existing Fire Resistance/Protection relief apply
  to baseline and activity depletion; the baseline is one point/min before these factors.
- Charge illness exhaustion separately from climate, armour, activity integrations and season.
  Keep its EXHAUSTION event visibility, without charging it twice. Nourishment may suppress activity
  depletion, but must not cancel baseline depletion, water illness or illness's healing restriction.
  Explicit pack overrides remain possible.

These drain values are tuning candidates. Validate adapter units and measure actual routes before
release; do not infer seconds of travel from item values alone.

### Sickness

Clean and Pure have **no water sickness on any difficulty**. Peaceful has none from fresh water.
Use one shared illness roll per drink for the default effects: Poison chances below are included
within Upset Stomach chances, not added to them. Poison always comes with Upset Stomach.

| Water | Difficulty | Upset Stomach: chance / duration / level | Poison I: chance / duration |
|---|---|---|---|
| Dirty | Easy | 65% / 45 s / I | 15% / 10 s |
| Dirty | Normal | 90% / 60 s / II | 35% / 20 s |
| Dirty | Hard | 100% / 90 s / II | 50% / 30 s |
| Murky | Easy | 35% / 30 s / I | 5% / 8 s |
| Murky | Normal | 65% / 45 s / I | 15% / 15 s |
| Murky | Hard | 85% / 60 s / II | 25% / 20 s |

- Upset Stomach blocks natural healing even at full hunger, saturation and hydration.
- Level I drains **4 hydration points/min**, level II **8/min**, independent of climate. Food
  saturation and incoming quenched are multiplied by **0.5 at I, 0.25 at II**, rounded down for
  quenched. Thirst recovery is unaffected, so prepared water can still prevent dehydration.
- `extendSicknessEffects = true`: a repeated Upset Stomach proc adds **half the incoming duration**,
  capped at **1.5 times the incoming duration**, without shortening an existing longer effect or
  lowering its level. For remaining duration R and incoming duration D, use
  `max(R, min(R + D / 2, 1.5 * D))`, calculated in ticks; a first proc applies D normally.
  Repeated Normal Dirty procs therefore leave at most 90 s, unless a longer effect was already
  present. Repeated drinking can keep renewing that window; the cap limits time remaining, not
  total time spent ill. Poison and other configured effects keep their existing extension rules.
  Safe water neither extends nor instantly cures illness. No guaranteed taste Nausea or automatic bursts
  are needed for punishment; mechanics and clear effect/blocked-heal feedback carry the consequence.
- Milk and honey can clear Poison according to their normal rules, but **milk does not cure Upset
  Stomach**. Time ends it; commands and explicit modded cures still work. Drinking Clean/Pure keeps
  the player alive while recovering. Do not remove unrelated effects or override every cure API.
- No additional lethal disease in this pass. Poison itself does not kill; low health, continued
  dehydration and cave hazards supply the danger. Keep salt's no-hydration outcome, immediate
  exhaustion, Nausea and Parched, with climate-independent illness drain.

A Normal Dirty proc costs **8 hydration points over 60 s**, plus activity and baseline
depletion, against only six thirst restored by that drink. Normal Murky costs three points over
45 s if illness procs. Three Murky drinks have a **95.7%** chance of at least one illness and a
38.6% chance of at least one Poison proc. Untreated water buys time in an emergency; it is not a
sustainable substitute for treatment during a mining trip. These are proposed, untested defaults.

## Containers and crafting

Servings per slot counts a filled stack. All water drinks use the shared 6 / 4 base above.

| Container | Craft cost | Servings per slot | Clean / Pure total hydration | Role |
|---|---|---|---|---|
| Water bottle | 3 glass for 3 bottles | 1 (does not stack) | 8 / 10 | Vanilla's, sharing, brewing |
| Terracotta Water Bowl | 3 clay balls for 3 clay bowls, each fired into one bowl | 3 (3 bowls) | 24 / 30 | Stackable supply, sharing |
| Waterskin | 4 leather | 4 (one skin) | 32 / 40 | One vessel to fill, no separate empties |
| Copper Canteen | 3 copper ingots + 1 string | 4 (one canteen) | 32 / 40 | Fast field treatment without leather |
| Iron Flask | 3 iron ingots + 1 iron nugget | 6 (one flask) | 48 / 60 | Longer trips, no leather/string requirement |
| Water bucket | 3 iron ingots | 3 for closed transfers | Cannot drink directly | Transport and world placement |

Totals are nominal sums over spaced drinks, not what the player's bars store at once.

- Filled bowls **stack to 3 by default**, only when all their data match, including quality and salt.
  Empty bowls keep their stack size; skins, canteens and flasks remain unstackable. Each drink
  consumes one serving and returns exactly one empty bowl. Fill stacked empties one at a time without
  duplication or item loss.
- **Taken back: water bottles do not stack.** They stacked to 3 through a per-stack
  `max_stack_size`, then it was removed: a stacking potion surprises other mods' brewing, storage and
  automation code. Bottles stay vanilla's. (Hearth and Harvest stacks plain water bottles itself; see
  its integration plan.)
- Default carried capacities are **4/4/6** for skin/canteen/flask. A skin carries one more serving
  than a bowl stack and creates no separate empties. Copper adds field boiling at equal
  capacity; iron carries twice a bowl stack and 50% more than copper. Three crafted clay bowls
  become three reusable fired bowls, exactly one default filled stack. Recipe yield stays fixed.
- Longer autonomy rewards preparation. Do not increase drain to cancel the stack buff: untreated
  water remains dangerous, healing still spends reserves, and every carried serving must be treated.
- No durability, leaking, slower drinking for cheaper vessels or extra recovery for expensive ones.
- Waterskin sprites depend on fill fraction: empty at zero; otherwise choose one of the three filled
  sprites with `ceil(3 * min(servings, capacity) / capacity)`. The bar and numeric tooltip show exact
  contents at any configured capacity. Raise the saved/network serving bound to **64** and audit
  saved/network data, fluid APIs, config bounds and recipe generation. Preserve existing quantities;
  expanding capacity must not grant water. Saved excess above a custom lowered limit stays usable.
- Village house chests can contain 1 to 4 empty bowls, gated on the bowl being enabled.
- Bowl-and-bucket crafting produces Dirty water and returns the empty bucket. Treat this as a lossy
  convenience recipe; it must neither duplicate water nor turn salty input fresh.

### Container configuration

All the following settings accept integers **1 to 64**, clamped in `sanitize()`, with a widget,
reset value and translated description in the container settings. These are independent controls;
do not silently change another setting to preserve the default progression.

| Setting | Default | Controls |
|---|---|---|
| `terracottaWaterBowlStackSize` | 3 | Filled Terracotta Water Bowls per stack |
| `waterskinCapacity` | 4 | Servings in one Waterskin |
| `copperCanteenCapacity` | 4 | Servings in one Copper Canteen |
| `ironFlaskCapacity` | 6 | Servings in one Iron Flask |
| `copperHangingPotCapacity` | 3 | Servings in one Copper Hanging Pot |
| `ironHangingPotCapacity` | 6 | Servings in one Iron Hanging Pot |
| `distillerTankServings` | 9 | Servings in each distiller tank |

- Stack size and servings are different: a bowl still holds one serving; a carried vessel
  still stacks to one. Bucket volume stays three servings, independent of capacity configuration.
- Capacity changes never grant or delete water. An over-capacity saved vessel can drain but takes
  no additional water until it has room. A tank smaller than three cannot accept a whole bucket:
  reject that transfer without consuming it; one-serving containers still work.
- Stack-limit settings require restart and must agree between server and clients. Preserve existing
  items when a limit is lowered: split safely where space exists; retain unsplit excess if no safe
  destination exists and prevent further merging. Never delete overflow or duplicate remainders.
- Preserve custom config values within 1 to 64; missing keys use the defaults above. Audit recipes
  for every fill count up to 64, GUI limits, models, tooltips, save/load and all loader fluid APIs.
  Stack size 64 is an intentional pack option, not the default balance target.

## Treatment stations

| Vessel | Capacity | Seconds per serving / full batch | Commitment |
|---|---|---|---|
| Copper Canteen | 4 | 2 / 8 | Hold use over a lit campfire, or use furnace fuel |
| Iron Flask | 6 | 3 / 18 | Hold use over a lit campfire, or use furnace fuel |
| Copper Hanging Pot | 3 | 3 / 9 | Unattended Clean water |
| Iron Hanging Pot | 6 | 4 / 24 | Unattended two-bucket batch |
| Copper Distiller | 9 per tank | 8 / 72 | Fuel, Pure output, desalination, automation |

Both pot recipes use five matching ingots, two sticks and one chain. Copper serves small frequent
batches; iron needs fewer refills. Pots lose water when broken and collect rain as Clean.
The distiller's boiler centre uses **one iron ingot**. Keep the other component recipes: the machine
costs copper, iron, fired clay and assembly, so a player settling by the sea need not find gold.

The table shows default capacities. Treatment time scales with actual servings, not maximum capacity:
four drinks in a canteen take 8 s even if its configured capacity is 64. A default full flask takes
18 s. Higher-capacity packs can use unattended furnace preparation or treated storage. A default
stack of three bowls is three furnace/smoker operations (24 s / 12 s), never one operation
treating the whole stack. Pots offer fuel-free unattended batches; capacity settings do not change
per-serving throughput or fuel cost.

Pure's reserve and convenient sea-water processing are enough rewards. Clean must not make players
ill to force a distiller purchase. Natural Pure sources, rain and Create filtering remain valid
alternative infrastructure. Multiple dripstone cauldrons scale output at the cost of space and iron;
never justify their balance from one cauldron's speed alone.

With `enableFurnaceBoiling` enabled, all outputs cap at Clean:

| Input | Furnace | Smoker |
|---|---|---|
| Bottle or bowl, 1 serving | 8 s | 4 s |
| Bucket, 3 servings | 24 s | 12 s |
| Copper Canteen | 2 s per serving | No recipe |
| Iron Flask | 3 s per serving | No recipe |

Recipe XP stays zero for water treatment. No water cooking recipes in vanilla campfire slots.

## Integration and transfer boundaries

- Separate plain-water recovery from potion recovery. The stack lookup must distinguish water
  from potions before consulting the cached item-only value; never cache a stack-dependent result
  under `minecraft:potion`. HUD, tooltip, consumption and automation must agree.
- Audit known third-party plain-water drinks, including Cold Sweat's drink action, against 6 / 4.
  Keep temperature effects and pouring actions separate; do not turn them into extra drinks.
- Solid food is incidental hydration: wet fruit restores at most **2 thirst / 0 quenched**, vegetables
  at most **1 / 0**, dry foods and meat **0 / 0**. Apply the no-quenched ceiling to solid food from
  integrations too, with explicit pack overrides. Keep hunger, saturation and special food effects.
  Melon stacks and golden carrots must not substitute for a water reserve through food overflow.
- Plain milk gives **4 thirst / 0 quenched**. It can remove Poison but not Upset Stomach, and cannot
  by itself enable natural healing with an empty water reserve. Milk-based prepared drinks follow
  their recipe category, not an automatic Pure-water bonus.
- Prepared soups and non-alcoholic crafted drinks can supply reserve: target **6 thirst / 4 quenched**
  (also `drinkTagValue` and the keyword drink and soup values; keyword fruit is 2 / 0)
  per completed serving, gated on actual recipes/ingredients. They are valid preparation, not raw
  cave water. Audit known integrations' values and stack sizes before shipping; cheap juices must
  not also inherit 8 / 13 recovery merely because they are tagged as drinks. Do not change foreign
  item stack sizes globally. Exotic effects and modpack overrides cannot be universally balanced.
- Transfers between finite vessels conserve servings and never raise quality by changing container.
  A bucket is three servings regardless of the destination's configured capacity; partial transfers must
  leave leftovers or explicitly discard them, never duplicate them.
- World placement is different: ordinary placed water is sampled from its environment, not a saved
  bottle grade. A bucket can establish a refill point outside evaporating dimensions. Retain this
  vanilla-compatible shortcut; do not describe buckets as only three drinks in open-world use or
  the distiller as the only possible route from an ocean bucket to fresh water in another biome.
- Exposed safe water is a reward for location and travel. Cave refill points still produce at most
  Murky water and need treatment. Closed salt-water tanks require distillation; the Nether rewards
  carried supplies. A campfire and copper vessel provide an affordable treatment route before a
  distiller, including an underground camp.

## Implementation

Step by step, with where the code goes and the performance budget each step must keep:
[PURIFICATION-REWORK-IMPLEMENTATION.md](PURIFICATION-REWORK-IMPLEMENTATION.md).

1. **Names and visuals:** update quality keys, config widgets, Sophisticated UI, the three renamed
   JSON keys and their migration, and all nine lang files. Vietnamese: "Sạch", "Chất lượng nước". The water textures keep their
   colours (taken back); keep Clean's tooltip colour. Preserve persisted/public identifiers listed above.
2. **Recovery:** add configurable plain-water values and a stack-aware lookup, update own vessel
   defaults and required-value insertion, food/milk/prepared-drink defaults and integration categories.
   Remove hydration overflow without breaking existing public signatures or event cancellation;
   allow Clean/Pure reserve top-ups at full thirst, including automation and previews.
   Defaults apply to fresh configs; preserve deliberate custom values. Update all tooltip paths.
3. **Player loop:** implement timed hand drinking, the hydration/illness healing gate, the conditional
   25% bonus per successful food heal and its exhaustion cost; remove the independent water-heal
   timer/path. Add baseline drain, monotonic climate/global scaling and separate illness drain.
   Update config controls/reset/lang and document the bonus setting's changed meaning. Audit
   loader/version seams, actual health gains, starvation and healing refunds.
4. **Sickness and sources:** implement the table, stronger drain/reserve penalties, milk-cure exception
   and no automatic taste/burst Nausea. Add an optional roll-group field to effect entries: entries
   in one group share a random sample per drink; absent groups retain independent rolls for custom
   configs. Default Poison and Upset share a group, with Poison's chance no greater than Upset's.
   Cover serialization, copying, sanitization and config UI; retain duration extension as default,
   with Upset Stomach's half-duration addition and 1.5-times cap, leaving other effects unchanged.
   Implement the covered-water grade cap in the sampling path, with platform seams for sky checks.
   Keep unrelated vanilla Nausea behavior; its drain is illness, not activity. Update sickness docs.
5. **Containers:** configurable capacities and filled stack limits from the table, all 1 to 64,
   with default carried capacities 4/4/6 and bowl stacks of three (bottles: taken back, see above). Update skin models and
   saved/network bounds, config sanitation, widgets/reset/lang and client/server agreement.
   Audit drinking, filling, pouring, dispensers, hoppers and returns for stacked bowls.
   Pot capacities default to three/six and support 64. Do not expand the level-by-boil-progress
   blockstate product to that range: store exact contents/progress in a block entity with bounded
   visual states, migrating existing pot states without water loss. Audit rendering, Jade, transfers
   and partial boiling. Distiller tanks also support the configured 1 to 64 range.
6. **Heat and recipes:** add `WaterPurity.BOILED = 2`; cap core, Farmer's Delight and Cold Sweat heat.
   Add furnace switch defaulting to true and per-loader resource conditions. Existing explicit false
   values stay false. Update both recipe providers, crafting
   costs/times, pot timing and village loot. The three-clay recipe outputs three clay bowls; firing
   remains one-to-one. Remove Cold Sweat furnace/smoker water recipes.
   Do not hand-edit generated output. New recipe patterns must be collision-free on every node.
7. **Advancements:** `boil_water` accepts completing a furnace/smoker water recipe as well as the
   inventory criterion for either metal vessel or Hanging Pot; `purified_water` points to the
   distiller. Update both advancement providers so the default early furnace route counts.
8. **Docs:** update water reference, sickness and distillation plans, integration instructions,
   player/config pages, store pages, screenshots and CHANGELOG. Keep the water page's URL, title it
   "Water quality". Explain resetting affected settings to adopt new defaults.

## Validation and tuning

First validate correctness, then playtest these candidate defaults. No in-game balance trial has
been run for this proposal.

| Synthetic sustained drain | Clean: interval / default six-serving flask | Pure: interval / default six-serving flask |
|---|---|---|
| 4 hydration points/min | 120 s / 12 min | 150 s / 15 min |
| 8 hydration points/min | 60 s / 6 min | 75 s / 7.5 min |
| 16 hydration points/min | 30 s / 3 min | 37.5 s / 3.75 min |

These are arithmetic budgets: `(thirst + quenched) / drain`. They exclude initial player reserves,
refilling, overflow waste, sickness and healing. The rates are test scenarios, not measured movement
rates; validate real runs before claiming travel times.

- Correctness on `26.3.x`, `1.21.1-neoforge`, `1.20.1`, `1.20.1-forge`: config-key migration (legacy
  only, new only, both present, custom values preserved), retained `clean` sickness key, plain
  water versus potions, recovery rounding/caps/overflow, event behavior, sickness and config defaults.
- Hand use: no benefit before 32 ticks, cancel/reach/offhand checks, exactly one completed drink.
  Health: test thirst 19/20, quenched 0/1 and 5/6, Upset present/absent, both healing paths, each option off,
  refunds, starvation and natural regeneration off. Test Clean/Pure top-ups at full thirst and block
  that exception for raw water/hand drinking. At the default, 1 actual base HP gives at most 0.25
  bonus HP and exactly 1.5 bonus exhaustion; check partial heals, remaining-health caps, cancelled
  heals, fractional cost, no duplicate charge and no independent timed healing. No bonus when the
  base heal fails; no bonus on potion/mod healing. Bonus eligibility stays strict with base gates off.
- Climate: no discontinuity at the old temperature threshold; global setting also scales Nether;
  seasons and Cold Sweat do not double-count; illness is neither climate-scaled nor cancelled by
  Nourishment by default. Check baseline drain exclusions and that Nourishment leaves it active.
  Compare bonus enabled/disabled: extra recovery must be bounded by the configured share of actual
  food healing and paid for in water, including when food healing is slow or modified by another mod.
- Sickness: shared roll thresholds, independent custom entries, no Poison without Upset in defaults,
  first-proc duration, half-duration extensions and 1.5-times remaining-duration cap. Cover fractional
  seconds in ticks, mixed grades/levels, longer existing effects, extension disabled, unchanged Poison
  extension, milk/honey cures and recovery on expiry. Normal Dirty must expire within 90 s after the
  last proc when no longer effect was present; continuing to drink may renew that window.
- Anti-bypass: steak/fruit plus repeated cave water cannot restore natural healing during Upset;
  food overflow creates no reserve. Milk plus raw water is not an illness cure loop. Test caves,
  surface water under roofs/trees, placed sources, stored treated water and tagged Pure springs.
- Containers: default stacks of three bowls, bottles not stacking, quality separation, returns, legacy saves, high
  serving counts and partial fills through every loader's fluid API, including inventory full.
  Test brewing with manual/shift-click/hopper insertion, potions retaining their stack limits,
  dispensers and one-item cooking/consumption. Verify the three-serving stack and carried vessels restore
  exactly the same amount per serving. Test zero XP, recipe outputs/times with switch on/off, fresh
  default-on configs, existing explicit false configs, loot and both advancement routes.
- Config: test each stack/capacity setting at 1, its default and 64, plus out-of-range clamping.
  Test mismatched-client protection, restart semantics, safe stack reduction with full inventory,
  lowering filled capacity without loss, one-serving tanks refusing buckets, pot-state migration and
  recipes at high fill counts. Verify three clay produces three clay bowls and three fired bowls.
- Generate resources; run gametests, `checkDatagen`, resource translation checks, `checkLang`,
  `checkDataConditions`, relevant seam checks and the docs build.
- Play the same 20-minute routes with each vessel: starter survival, building, ordinary exploration,
  repeated combat and Nether travel. Record drink count, active treatment time, inventory slots,
  health/food/water spent and refill opportunities. Repeat with prepared Clean and Pure water.
- Include sparse leather/string spawns, ocean starts, multiplayer sharing, bucket refill points,
  dripstone arrays, stackable juices and optional-mod automation. A same-seed comparison isolates
  container differences; use several seeds to test material availability.
- Add a long-trip comparison of a default full stack of three bowls, a skin, a canteen and a flask.
  Record preparation fuel/time and empty-container slots, not just departure capacity. Ensure the
  larger vessels remain convenient without forcing full-batch hand boiling or inflating drain.
- Compare three matched mining runs: food plus untreated cave water; food plus prepared Clean;
  food plus field boiling. Record illness uptime, blocked-heal time, health, water and food spent.
  Raw water must create a material survival disadvantage; prepared water must permit recovery.
  Also test one emergency raw drink followed by shelter and Clean water: a prepared recovery route
  must exist without requiring a distiller or unavoidable death.
- If ordinary travel needs a drink more often than roughly once a minute, tune activity drain before
  adding more capacity. If prepared travel is too easy, test climate and route opportunities before
  adding random sickness to Clean. Keep the bounded, paid healing bonus and conserved-serving rules fixed while
  tuning recovery, so one adjustment is not hiding a different exploit.
