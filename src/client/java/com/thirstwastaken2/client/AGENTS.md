# src/client/java — client-only code

The split source set (`loom.splitEnvironmentSourceSets()`). Everything touching
`net.minecraft.client` lives here and **nowhere else**; common code in `src/main/java` may not
reference this package. The reverse is fine — this code reads `ThirstConfig`, `ThirstManager` and
`ThirstData` directly.

Nothing here is authoritative. The client renders the `ThirstData` the server synced to its owner
alone (`ThirstData.STORAGE`), and only the config screen's AppleSkin settings are client-side.

| File | Owns |
|---|---|
| `ThirstWasTaken2Client` | `initialize`, called by the loader's client entrypoint: registers the HUD row |
| `ThirstHud` | drawing the bar |
| `HandDrinking` | hand drinking from water the crosshair misses: picks again with fluids and sends vanilla's use-on-block packet on the water |
| `config/ThirstConfigScreen` | the whole options screen: header with search, a sidebar tab per page, the scrolling rows, Reset, Cancel and Done |
| `config/ConfigCategory` | every page: its icon, its `ConfigSection` tabs and any rows above them (preview, note) |
| `config/ConfigSection` | one tab of a page: its `ConfigEntry` list and any rows after it (open-file button, the item list) |
| `config/ConfigEntry` | one setting: getter/setter on the live config, its default, its control (`toggle`, `choice`, `grade`, `percent`, `number`) and its lang keys |
| `config/ConfigRow` | one row of the list: heading, tab strip, setting, note, preview or action button |
| `config/ItemValueRows` | the per-item thirst values on the Item Values page: one row per listed item, and the row that adds one |
| `config/SicknessRows` | the sickness tables on the Sickness page, one tab per difficulty: a heading per grade with reset, a row per effect (chance, seconds, level, remove), and the row that adds one |
| `config/ConfigTheme` | the screen's colours and small drawing helpers |
| `config/ConfigPreview` | the live thirst bar, food bar and tooltip on the AppleSkin page |
| `platform/ClientVanilla` | client vanilla calls whose shape differs between Minecraft versions |
| `platform/MachineScreen`, `screen/DistillerScreen` | a container screen drawn the same way on every version, and the copper distiller's GUI on it, from `textures/gui/container/copper_distiller.png` (written by `tools/distiller/generate_distiller_gui.py`) |
| `platform/StatusBarRenderer` | the shape `ClientLoader` draws a HUD row through |
| `compat/AppleSkinIntegration` | reads AppleSkin's own settings, only after `AppleSkin.isLoaded()` |
| `compat/JadeIntegration` | the `jade` entrypoint: the grade of the water under the crosshair, see `compat/AGENTS.md` |
| `mixin/MinecraftMixin` | every version and loader: calls `HandDrinking` at the start of a right click. Listed in `src/client/resources/thirstwastaken2.client.mixins.json` |
| `mixin/LocalPlayerMixin` | 1.21.1 only, on both loaders: the thirst sprint gate, on the client player's own food check. Same config |

Loader code for the client lives in `src/client/<loader>/java`, never here, and `checkLoaderSeam`
fails the build on a loader import in this directory:

| File | Owns |
|---|---|
| `client/fabric/ThirstWasTaken2FabricClient` | the Fabric `client` entrypoint |
| `client/platform/ClientLoader` | HUD layer and status bar height registration, per loader |
| `client/compat/ModMenuIntegration` | the `modmenu` entrypoint; Mod Menu is a Fabric-only mod |
| `fabric/mixin/GuiMixin` | 1.21.1 only: the status bar registry Fabric API gained in 1.21.6 |
| `client/neoforge/ThirstWasTaken2NeoForgeClient` | the NeoForge `@Mod(dist = CLIENT)` class; also registers the config screen as the mods list's `IConfigScreenFactory` |

`ClientVanilla` is the client half of `com.thirstwastaken2.platform.Vanilla` and follows the same
rules — plumbing only, one signature on every version. A Stonecutter `//?` branch anywhere else in
this source set means a seam is missing from it. The HUD draw target is the exception that needs no
branch: 26.1 renamed `GuiGraphics` to `GuiGraphicsExtractor` and left every drawing method this mod
uses untouched, so `stonecutter.gradle.kts` renames the type back for older versions instead.

## HUD

`ClientLoader.addRightStatusBar` places the bar after the food bar and reserves 10px of the right-hand
stack while `ThirstHud.shouldRender` holds, so vanilla stacks around it. On Fabric that is
`HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, …)` plus
`HudStatusBarHeightRegistry.addRight`, under the one `thirstwastaken2:thirst_bar` id. The loader
reads the stack height back and hands `ThirstHud.render` the row's `top`.

Fabric API has neither registry before 1.21.6. On 1.21.1 `ClientLoader` keeps the rows itself and
`GuiMixin` (in `src/client/fabric`) draws them where vanilla is about to draw the air bubbles, 49px up
from the bottom for the one row, then translates the bubbles up by the height it used. That also
means the bar is only drawn when vanilla draws the health and food bars, as on later versions.

Every blit goes through `ClientVanilla.blit`, because 1.21.1 has no render pipelines: the tint there
is shader colour state, set before the draw and reset after it.

Sprite geometry, which is easy to break:

- `thirst_icons.png` is 41x9 — five 9px frames (empty, quarter, half, three quarter, full) on an
  **8px stride**, because neighbouring frames share their transparent edge column. Hence
  `U_EMPTY = 0` and `FILL_FRAMES = {8, 16, 24, 32}`.
- Each droplet holds two thirst points, so `FILL_THRESHOLDS` is `{0.5, 1.0, 1.5, 2.0}` against
  `level - i * 2`.
- `thirst_icons_parched.png` is the same sheet in dry sand, drawn instead while the player has
  Parched, the way vanilla swaps to `food_*_hunger` for Hunger. Same size and frames, so every blit
  argument is shared. `tools/generate_parched_icons.py` draws it from `thirst_icons.png`; rerun it
  after touching the droplets rather than editing the parched sheet. The config preview always
  draws the plain sheet.
- `thirst_icons_upset_stomach.png` is the same sheet in a venom green, drawn while the player has
  Upset Stomach. It wins over Parched when both are on. `tools/generate_upset_stomach_bar.py` draws
  it from `thirst_icons.png` by swapping its nine blues for a hand-drawn ramp; rerun it rather than editing the sheet.
- The HUD draws whole thirst points only, as vanilla draws food: exhaustion toward the next point is
  never shown on the droplets, so the quarter and three-quarter frames go unused there. A bar that
  showed the drain looked short of full at 20 while drinking was still refused, which read as a sync bug.
- The server syncs exhaustion in quarter-point steps (`ThirstManager.SYNC_STEP`), not every tick. The
  AppleSkin strip moves about 5 px a step.
  Do not add a client feature that needs finer exhaustion without revisiting that step.
- When AppleSkin is loaded, its exhaustion-underlay option is enabled and the quenched outline is not
  `OFF`, `AppleSkinIntegration` says so and `ThirstHud` draws the synced exhaustion as the `v = 18` dither strip of
  `appleskin_icons.png`, blitted with the 256x256 texture size.
- The quenched outline is AppleSkin-only, like the strip: `compat/AppleSkin.quenchedOverlay()` (common
  code) returns `OFF` without AppleSkin, and otherwise the player's `appleskinQuenchedOverlay`. It comes
  from `quenched_overlay.png`, 36x45, at `u = 0/9/18/27` by quarter and `v = ordinal * 9`, so
  `QuenchedOverlay`'s order is the sheet's row order. Every sheet here has its own texture size — do
  not copy blit arguments between them.
- `tools/generate_quenched_overlay.py` draws `quenched_overlay.png` and the matching tooltip glyphs.
  Edit the palettes there and rerun it rather than touching the PNGs.
- The bar shakes when quenched hits zero, mirroring vanilla hunger (`shakePeriod = thirst * 3 + 1`).
- `shouldRender` deliberately does not ask whether the player is alive. Vanilla draws the food bar
  for a dead player, so the hunger bar stays on screen behind the death screen, and a thirst row
  that hid itself there would leave a gap above it. `tools/agent/ui/hud-death-screen.jsonl` is the
  check.

`ThirstTooltip` (common) uses the same two-units-per-droplet rule with its own bitmap font. If the
fill thresholds change here, change them there too.

## Config screen

`ThirstConfigScreen` is the one screen Mod Menu (Fabric) and the mods list (NeoForge) open. It is built
from vanilla widgets and plain fills only, no config library: a header with the mod's name and a search
box, a sidebar with a tab per `ConfigCategory`, the page's rows, and Reset / Cancel / Done. Typing in
the search box lists matching settings from every page, grouped by page. The sidebar names its pages
without icons, as vanilla's tabs do, and so do the page's heading and the search results' page headings; below 380 GUI pixels wide it shows the pages' icons only. The HUD position is fixed to vanilla's right-hand status-bar stack; there is
no offset setting because the preview cannot show screen position.

It keeps to vanilla's look (`ConfigTheme`): white marks the selected page and tab, text is grey or
white, the sidebar and list sit on one dark translucent panel between vanilla-style separator lines,
and amber for a changed setting is the only colour of the screen's own. The item and mod icons carry
the rest; no accent colour.

### Grouping settings

**Settings are grouped by topic, not by how many fit on screen.** A page is one subject (Thirst,
Water, Containers). A page that covers several distinct things is split into `ConfigSection`s, shown
as a strip of tabs under its heading, one tab per thing: Water is Drinking, Quenched, Sea Water, and
Rain and Dripstone. Scrolling is fine; the Item List tab is long and stays one tab. So:

- A new setting goes in the section whose topic it is, never the nearest one with room.
- If no section fits, add one, or a page if it is a subject of its own. Do not widen a section's
  meaning to take it in.
- Do not split a page that is one topic just because it is long, and do not merge two topics because
  each is short.
- A page of one section shows no strip. Once a page gains a second topic, split it, and give the
  settings already there a section named for their own topic.

The heading and the strip are pinned, and only the rows below them scroll. The tab shown is kept per
page while the screen is open. Reset in the footer still acts on the whole page. A section's title
is `thirstwastaken2.config.group.<page>.<section>`, in all nine lang files.

A setting that does nothing without another mod is built `.requires("<mod id>")` and left off the
page, the search and Reset while that mod is missing; its value stays in the file. Cold Sweat Climate
is one. A tab left with no settings that way is dropped too (Thirst's Seasons tab without Serene
Seasons), and a page left with one tab shows no strip.

A setting row is its name (amber, with an amber bar, when it differs from the default), its control,
and a reset button beside it; the row's tooltip is the description. Rows scroll a whole row at a time,
so a row is either fully shown or hidden and nothing needs clipping. Tab painting and the reset button
go through `ClientVanilla.button`, other drawing through `ClientVanilla.canvas`, because 1.21.11 and
26.1 renamed the method a widget draws in.

Controls write **straight into the live `ThirstConfig` instance**, so the HUD, the tooltips and the
preview follow every change at once. Done or Escape calls `ThirstConfig.commit()`, which re-sanitises,
bumps the generation and saves; Cancel calls `ThirstConfig.restore` with the snapshot taken when the
screen was constructed. So a control's range must not be wider than the clamp in
`ThirstConfig.sanitize()`, or the value silently snaps back. `ConfigEntry` reads `ThirstConfig.get()`
on every call, so it never holds a stale instance after Cancel.

Reset (per row, or the footer's for the page or the search results) copies values from a
`new ThirstConfig()` and rebuilds the rows, because controls show the value they were built with. On
the Item Values page the footer's Reset also puts back every item the page shows, as each row's arrow
would (`ItemValueRows.resetAll`), and lights while any of them differs; ids of mods that are not
installed are left alone.

### Item values

`ItemValueRows` puts the `drinks`, `foods` and `itemBlacklist` entries on the Item Values page's Item List
tab. Its two switches and the Open button, which is now only for the keyword patterns, are on the
Matching tab. One row per id, sorted, so
one mod's items sit together: icon and name, a thirst and a quenched box (0 to `ThirstData.MAX`), a
switch that adds or removes the id from `itemBlacklist`, and reset. Reset puts back the mod's value for
an id `new ThirstConfig()` lists, and takes the line out for any other; a default id is never removed,
since `sanitize()` would merge it straight back. Ids of mods that are not installed are left off the
page, with a note counting them, and stay in the file.

Rows are grouped by namespace under a heading named by `Loader.modName` (the mod's own name, or the
namespace when no mod has that id): vanilla first, then by name. A heading opens and closes its group.
While open it carries the thirst and quenched droplets over those two columns, the same font glyphs
the item tooltip draws (`ThirstTooltip.thirst` and `quenched`, so the outline follows the quenched
overlay setting), and names them on hover. It stays at the top of the list
while its items scroll under it, until the next heading reaches the top: an item row is built
`.under(heading)`, and the screen shows that heading above the first scrolled row it sits under.
Only `minecraft` starts open. The open set is a static field, kept for the session, and adding an item
opens its group. The search box finds item rows by id or name, and every item of a mod whose name or
namespace matches, under their headings, always open.

- **The icon is only drawn in a world** (`minecraft.level != null`), for the `ItemStack` reason below.
  `ConfigTheme.item` calls `fakeItem`, which `stonecutter.gradle.kts` renames to `renderFakeItem`
  before 26.1.
- **26.1 removed `EditBox.setFilter`.** A box's responder puts the last valid text back instead.
- **The integrated server reads the same config from its own thread.** An edit that adds or removes a
  key replaces the map or set with an edited copy; a value edit puts a new array under an existing key.
  Keep that when adding an edit: never restructure a map the server may be reading.
- A new item goes into `drinks` with `drinkTagValue`, the value any tagged drink gets. The add box
  completes an id from the item registry and Add takes the completion.

### Sickness

The Sickness page holds `extendSicknessEffects` on its General tab and the `sicknessEffects` tables,
one tab per difficulty, built by `SicknessRows`. Under each grade, Dirty to Pure, one row per effect: its icon
and name (a faint id when no installed mod registers it), a chance, seconds and level box, and remove;
then a box that completes an effect id from the registry and Add. The grade's heading names the three
columns and resets that grade alone. The footer's Reset, on this page and not from a search, puts back
the whole page: the General tab and every difficulty's table (`SicknessRows.resetAll`).
Every edit swaps `sicknessEffects` for an edited copy, for the same reason as the item maps.
`tools/agent/ui/config-screen.jsonl` opens the Normal tab.

### Mod Items

Six switches, one per item a pack would replace (the three bowls are one). They change nothing on the
spot: the recipe condition is read as data loads, and the creative tab is filled when the client builds
it. So the page opens with a note saying a change needs `/reload` or rejoining, and a restart on a
dedicated server, which reads its own file only on start.

`ConfigPreview` sweeps quenched and saturation over 3.2 seconds, and its exhaustion strip fills once
per sweep, drawn separately from the bar so the droplets never show a drain: the same loop as
`hud-appleskin.gif`, which `tools/generate_docs_images.py` draws. Keep the two in step. The tooltip
and the bar block are each centred in the preview box.

Adding a setting means: field in `ThirstConfig`, clamp in `sanitize()`, a `ConfigEntry` in the section
of its topic (see "Grouping settings"), and `thirstwastaken2.config.<key>` plus `thirstwastaken2.config.<key>.tooltip` in
all nine lang files (`checkLang` fails on a missing one). The key is the Java field name
in snake_case. A new page also needs `section.<key>`, its tooltip and a 16x16 icon texture. Enums use
`choice`, labelled by `<key>.<value in lower case>`, so each value needs its own lang key. Doubles are
edited as integer percentages (`percent`) because the slider steps in whole numbers. Whole numbers
use `number`, labelled by `ConfigEntry.seconds`, `servings` or `wholePercent`; the first two are
`thirstwastaken2.config.unit.*` keys. A value inside an array (`quenchedPercent`) is set in place. The AppleSkin page
is always shown; without AppleSkin it adds a note saying the settings do nothing yet.

The preview never builds an `ItemStack`: Mod Menu opens the screen from the title screen, where 26.1
and later have not bound item components yet and constructing a stack crashes the game. Page icons are
drawn from textures for the same reason. 26.2 moved `setScreen` onto `Minecraft.gui`, hence
`ClientVanilla.setScreen`.

Mod Menu is `clientCompileOnly`. `ModMenuIntegration` (in `src/client/fabric/java`) is only ever
class-loaded when Mod Menu itself resolves the entrypoint, so nothing else may reference it.
