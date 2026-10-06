# SamPack EMI Tweaks

Load-time and EMI-reload fixes for the SamPack modpack on NeoForge 1.21.1. Each fix produces the
same result the original mod would have produced. It only removes redundant work, or a crash, along
the way. Every fix is a mixin into one specific mod. If that mod isn't installed, its mixin is
skipped.

This mod merges and replaces **Sampack_CreativeTabFix** (`sampack_tabfix`), **EMI Reclocked**
(`emireclocked`), **SamJem_InventoryItemGroups** (`inventory_item_groups`) and **SamJem: IconDump**
(`samjem_icondump`), and takes over from **Recreative** (`recreative`) for the pack's creative tabs.
Remove those jars when you install this one. The mod declares `inventory_item_groups`,
`samjem_icondump` and `recreative` incompatible, so a leftover copy is reported at startup instead
of applying twice.

On the client it requires [EMI](https://modrinth.com/mod/emi) and
[REMI](https://modrinth.com/mod/reliable-emi): the creative inventory searches with EMI and groups
with REMI, and every setting is in EMI's config screen. A dedicated server runs without either.

It is meant to run alongside [EmiAccelerator](https://modrinth.com/mod/emiaccelerator), not to
replace it. EmiAccelerator disk-caches EMI's item list and defers `EmiSearch.bake()`, and this mod
doesn't duplicate either of those.

## EMI fixes (`mixin/emi`)

These were written against EMI 1.1.24 for NeoForge 1.21.1 (Modrinth version `5sIPA1To`).

### Tag sort

`EmiTags.reloadTags(Registry)` sorts tags by member count, using a comparator that calls
`EmiTagKey#stream()`. That call re-derives the tag's members from the live registry on every
comparison. `EmiTagKey#getList()` already holds a fresh cached copy of the same list, because
`EmiTagKey.reload()` runs first. `EmiTagsSortMixin` computes each tag's size once from that list.
The resulting order is the same.

### Crafting remainder shortcut

`EmiShapedRecipe.setRemainders(...)` is shared by shaped and shapeless recipes. For every candidate
item in every slot, it builds a scratch 3x3 crafting grid, calls `recipe.getRemainingItems(input)`,
and reads back one index. `Recipe#getRemainingItems`'s default implementation is a flat per-slot
map through `getCraftingRemainingItem()`, so the rest of the grid never affects the answer.

`EmiRecipeRemainderMixin` checks each recipe class once, by reflection, and caches the result. When
the class still uses `Recipe`'s default `getRemainingItems`, the mixin replaces the grid simulation
with the equivalent per-item check. Any class that overrides it, or that the reflective check
can't verify, falls through to EMI's original code.

### Cheap exception for missing item ids

`ItemEmiStackSerializer.create(...)` calls `getHolder(id).orElseThrow()`. Every stale item id in an
EMI data file throws there, and EMI's generic `catch (Exception)` discards the exception right away.
`EmiItemStackSerializerMixin` throws the same exception type without filling in its stack trace.
The log message and the `EmiStack.EMPTY` fallback don't change.

### Search defaults

EMI's plain search matches substrings, and by default it also searches tooltips, so "light" finds
wooden buttons. If mod names are searched too, "light" also finds all of Twilight Forest.
`EmiConfigSearchDefaultsMixin` turns both defaults (`search-tooltip-by-default`,
`search-mod-name-by-default`) off. Tooltips can still be searched with `$` and mod names with `@`.
These are only defaults: an `emi.css` that already sets them keeps its values.

## Compatibility and load-time fixes (`mixin`, `mixin/compat`)

- **Duplicate creative-tab entries**: NeoForge's `assertNewEntryDoesNotAlreadyExists` no longer
  aborts the whole tab build when an entry is already in the set. That set's `add()` is a no-op
  for duplicates anyway.
- **ComputerCraft**: skips the ~13 s `tryRebuildTabContents` on server start in singleplayer, where
  the client rebuilds the tabs moments later anyway.
- **Enhanced Tooltips**: reuses vanilla's cached creative-tab build instead of building every tab a
  second time.
- **EMI Trades**: rolls trade offers with a per-thread random instead of the client level's random.
  Using the level's random from EMI's reload thread crashes the game.
- **Sable**: applies block physics properties once per reload instead of once per level. In
  singleplayer it also skips the redundant client-side packet application.
- **Puzzles Lib**: builds the top-level model location set once per model bake, in parallel.
- **owo-lib**: skips `DerivedComponentMap.derive()` when it can't change anything.
- **Farmer's Delight**: `ItemAbilityIngredient` shares one registry scan per ability instead of
  running one per recipe.
- **EMIffect**: per-effect scans only walk flower blocks and food items.
- **REMI**: stack groups are indexed by item id before stacks are matched against them.
- **REMI default stack groups**: the groups REMI ships in its own jar (`minecraft:planks`,
  `c:dyes` and so on) are not loaded (configurable). The pack's groups, which InvIndexLedger writes to
  `config/remi/stack_groups/`, and any group another mod or resource pack ships, still load.
  REMI's `disabledStackGroups` list in `remi.json` no longer needs to switch the defaults off.
- **BCLib**: anvil recipes stream the hammer tag instead of every item.

The javadoc on each mixin covers the details.

## Creative inventory groups (`creative`, `mixin/itemgroups`)

Collapsible groups in the creative inventory, in the style of Bedrock Edition: REMI's stack groups,
laid out the way REMI lays out EMI's index. Click the plus icon on a group to expand it and the
minus icon to collapse it. A group stays open across tabs, searches and screens for the session.
The groups can be turned off in the [config](#config-screen).

This replaces SamPack's fork of [Inventory Item Groups](https://modrinth.com/mod/inventory-item-groups)
by Bizarre Cube, which this mod used to carry as a second grouping system for when REMI wasn't
installed. REMI is now required, so its config, its Simple Config Lib / Cloth Config screens and
its `config/inventory_item_groups*` files are no longer used. Its sprites (the plus, minus and slot
highlights) are still drawn, under the `inventory_item_groups` namespace so resource packs made for
it still apply; their MIT notice is in `LICENSE-InventoryItemGroups` and ships inside the jar.

### Group icons

A collapsed group, in the creative inventory or in REMI's EMI panels, can show an icon other than
its items. The `icon` key of a REMI stack group json (any type: `remi:group`, `remi:tag`, ...)
takes:

| `icon` | Shows |
| --- | --- |
| `"first"` | the group's first item |
| `"stacked"` | its first three items, fanned out the way REMI draws groups |
| an item id, e.g. `"minecraft:oak_log"` (or `"item:minecraft:oak_log"`) | that item |
| a texture path ending in `.png`, e.g. `"sampack:textures/gui/groups/logs.png"` | that texture over the whole slot (any square size; resource packs can supply it) |

Without one, each place uses its configured default: the first item in the creative inventory and
the stacked three in REMI, unless changed in the config. An unknown item or a malformed value is
logged and ignored. REMI ignores the key itself, so stack group files with it still load without
this mod. REMI groups already take a display name from their `name` key.

```json
{"type": "remi:tag", "id": "chipped:acacia_log", "tag": "chipped:acacia_log", "icon": "minecraft:acacia_log"}
```

### Nested groups

A stack group can hold other stack groups, in the creative inventory and in REMI's EMI panels
alike. The `subgroups` key of a stack group json (any type) lists the ids of the groups inside it:

```json
{"id": "sampack:wooden_doors", "type": "remi:group", "priority": 1,
 "contents": ["item:minecraft:pale_oak_door", "item:quark:azalea_door"],
 "subgroups": ["chipped:oak_door", "chipped:spruce_door"]}
```

Expanding the doors shows the oak and spruce door sets as groups of their own, each led by its
own door as it is without nesting, among the doors that have no set; each expands in turn. The outer group's icon, item count and search by name cover its subgroups'
items too. In REMI's panels an expanded subgroup is drawn as its own region inside the outer one.

- Each item still belongs to the one group REMI matches it to: the highest `priority`, then the
  lowest id. So an item should be listed by one of the two groups only; InvIndexLedger leaves a
  chipped set's parent block out of the section, so the set keeps it as its first item, and
  writes `"priority": 1` on the section so no other group that names one of its items takes it.
- A group with fewer than two items in the list, its subgroups' included, is dissolved into the
  group around it, as REMI already shows a one-item group as a plain item.
- A disabled outer group leaves its subgroups standing on their own. A group named by two outer
  groups stays in the first, and a cycle is cut where it closes; both are logged.
- The key goes on the outer group because REMI's tag-page toggle rewrites a `remi:tag` file from
  scratch and would drop it from the inner one. REMI ignores the key, and while no group has
  subgroups REMI lays its panels out with its own code.

InvIndexLedger's `build` writes `subgroups` for every collapsible grouping section that has
chipped sets spliced in behind its items.

## EMI search bar (`search`, `mixin/emi`)

Each of these can be turned off in [EMI's config screen](#config-screen); all are on by default.
- **Clear on tab switch**: switching the creative inventory to another tab clears the search.
- **Clear on close**: leaving any inventory screen (creative, survival, a chest, any container)
  clears the search. Going to one of EMI's own screens (a recipe, the recipe tree) and back doesn't
  count, nor does the same kind of screen opening in its place (the creative screen reopening
  itself at a new size) or a switch between the survival and creative inventories.
- **Clear button**: an x near the right end of the search bar clears it while it has text.
- **Search history**: EMI already adds a search to its history when the bar loses focus with text
  that isn't already the newest entry, and the up and down arrows step through it. That history is
  now kept across restarts (`config/sampack_emitweaks/search_history.json`, newest first, 20
  entries by default). An arrow at the bar's right end opens it as a list (8 rows by default,
  scrollable). Left-click an entry to search it again, right-click to forget it; up, down and Enter
  pick one from the keyboard, and Escape or a click elsewhere closes the list. Switches at its foot
  turn clearing on tab switch, clearing on close and the clear button on and off without closing
  it. While the list is open it takes every click, release, scroll and those keys over it, a click
  outside it only closes it (except on the search bar, which it still focuses), and the screen
  under it is drawn as if the mouse were nowhere, so nothing under it highlights or shows a
  tooltip. It can
  optionally be narrowed to the entries containing what is typed. A search cleared by any of the
  above still goes into the history first.

The bar's text stops short of the buttons. A press the list takes has its release taken too, so
nothing lands on the screen once the list closes. EMI routes raw mouse and key input to its own widgets
before the screen sees it; the buttons and the list sit at the front of that
(`EmiScreenManagerSearchMixin`), and `GameRendererSearchMixin` hides the mouse from the screen the
way NeoForge hides it from the layers under the top screen.

## Config screen

Every setting of this mod, and every setting of REMI, is in EMI's own config screen, merged into
EMI's groups by topic rather than listed per mod. REMI's search options join EMI's own General →
Search; everything else is a subgroup at the end of EMI's General, UI or Dev group, with a button
in EMI's jump bar drawn in the style of EMI's own icons (`assets/sampack_emitweaks/textures/gui/config.png`).
When the jump bar runs out of room, these buttons are dropped before any of EMI's. The mod list's
config buttons open the screen at Search Bar (this mod) and Creative Tabs (REMI). The rows search,
collapse, count toward EMI's revert button and revert with it like EMI's own, and they are saved
when the screen closes, each file only if something in it changed.

| Where | Holds |
| --- | --- |
| General, after Cheat Mode | REMI's Better Cheat Mode |
| General → Search | EMI's own search options, then REMI's search by id and prefix options |
| General → Search Bar | clear button, clear on tab switch / on closing an inventory; REMI's search bar width, offset, padding, colors and texture |
| General → Search History | history button, rows shown / searches kept, filtering, keeping the history, clearing it |
| General → Tags | REMI's tag page options |
| UI → Creative Inventory | rows, fit to screen, following EMI's search, focusing search on the search tab, collapsible groups, default group icon |
| UI → Creative Tabs | REMI's creative tab sidebar options (tab sizes, icons, tab sync), its disabled tabs list |
| UI → Stack Groups | REMI's stack group options, REMI's default group icon, skipping REMI's built-in groups, its disabled groups list |
| UI → Sidebar Pages | REMI's pagination and other miscellaneous options, and a button to REMI's own screen |
| Dev → Icon Export | the [icon export](#config) options |

Values that come in pairs share one row, in the two-number widget EMI uses for its sidebar sizes
(hover a number for its name): rows shown and searches kept, REMI's vertical and horizontal tab
sizes, its tab icon size and tab count, its search bar offset and padding, the icon and sheet
size; REMI's two search bar colors share a row too.

This mod's settings are stored in `config/sampack_emitweaks-client.toml` (sections `search`,
`creative`, `remi` and `icon_export`) and REMI's in its own `remi.json`. Keeping REMI's creative
tab sidebar on the creative inventory's tab, which the grid's search relies on, is REMI's own
"Sync Selected Creative Mode Tab" setting (on by default).

## Creative tabs (`tabs`, `mixin/tabs`)

Replaces Recreative for the pack's creative tabs. InvIndexLedger's `build` writes the rules to
`config/sampack_emitweaks/creative_tabs.json`. The mod reads that file at startup. After a build,
run `/emitweaks reload_tabs` in game, then reopen the creative inventory. It re-reads the file,
rebuilds the tabs and reloads EMI. F3+T re-reads the file and rebuilds the tabs too. A tab id that
is new since startup still needs a restart, and the command says so. The file is a json array of
rules, in the format Recreative read:

| Rule | Effect |
| --- | --- |
| `{"action": "custom_tab", "tabs": [id], "name": .., "icon": item id, "items": [..]}` | Adds a tab. Each item is an id, or `{"item": id, "components": patch}`, where `patch` is a data component patch as json or as a json string |
| `{"action": "remove_tab", "tabs": [ids]}` | Hides tabs |
| `{"action": "tab_order", "order": [ids]}` | Puts the named tabs first, in that order. The rest follow in NeoForge's order |

Recreative's `modify_tab`, `#tag` entries, `.png` icons and item placement anchors are not
supported. The ledger never writes them. Without the file, the tabs stay as the game made them.

The custom tabs are registered as real creative tabs on the client only (the creative tab
registry is not synced to clients). An id another mod already registers is skipped with a warning.
Hidden and reordered tabs apply to the creative screen's pages, to `CreativeModeTabs.tabs()` (which
REMI's tab sidebar reads), and to the tab the screen opens on.

EMI's index and the `/emitweaks export` dumps still see the game's own tabs: `PristineTabs` and
`PristineEmiIndex` leave the custom tabs out and ignore the hiding and ordering.

## Creative inventory layout (`creative`, `mixin/creative`)

The creative inventory has as many item rows as the window has room for (5 to 20), with
vanilla's 9 columns. The rows (5 to 20) and fitting to the window are
[configurable](#config-screen) and apply the next time the inventory opens. Resizing the window or
changing the GUI scale reopens an open creative inventory at the new size, on the same tab and
scrolled to the same items.

All tabs are the same size and closer to square: 24px wide, showing 23px out from the panel (21px
for the bottom row), instead of 26x28, with their full vanilla borders and 1px between icon and
border. Tab sprites are drawn with a strip of their plain middle fill left out, so borders and
corners stay intact.
- The top and bottom rows hold only item tabs: as many as fit, 8 per row at vanilla width
  (vanilla: 5). A full row runs from the panel's left edge to its right edge, the width left
  over shared out as gaps between tabs, with vanilla's corner sprites at both ends. When the tabs
  would have to touch, the panel is widened just enough for 1px gaps (4px at vanilla width), by
  stretching plain columns either side of the scrollbar (the inventory tab's right margin).
- The search, inventory, saved hotbars and op tabs, which vanilla aligns right in those rows, are
  side tabs: search above inventory near the bottom of the left side, saved hotbars above op near
  the bottom of the right side, 4px above the panel's bottom edge (a side's only tab goes in the
  lower spot). A side tab is a tab mirrored across its diagonal: a top tab on the left, opening
  onto the lit left border, and a bottom tab on the right, opening onto the shaded right border.
  With EMI, `CreativeEmiPlugin` reports the screen's bounds as reaching out to the side tabs, so
  EMI's side panels, and REMI's creative tab bars (laid out from EMI's panels), stay clear of them.

Selecting a tab on another page (from REMI's sidebar, for example) turns to its page.

Going back from EMI's recipe screen keeps the creative inventory's tab, page and scroll position.
Refilling the grid (when EMI's search results come back unchanged, for example) keeps the scroll
position when the items are the same. Opening the search tab focuses EMI's search bar.

The extra columns and rows are inserted into the vanilla panel by repeating a slot column and a
slot row of the tab's own background texture. Resource packs and modded tab backgrounds still
apply. The inventory tab keeps its vanilla contents in the bottom-left corner, so its hotbar lines
up with the item tabs' hotbar. Saved hotbars are padded to full rows.

Most hooks change one constant or argument, so the vanilla methods still run along with other
mods' hooks on them. The side tabs are the exception: for the tabs vanilla aligns right, the
drawing, click and hover methods are skipped and done by this mod. The hooks were checked against the creative screen mixins of the mods in the
SamPack pack:
- **owo-lib**: custom tab textures still apply. owo reads each tab's `row()`/`column()`, which
  are reset from the actual page layout before each frame.
- **Sounds**, **Polytone**, **REMI**: unaffected.

## Icon and data exports (`icondump`)

Everything [InvIndexLedger](https://github.com/SampackSMP/InvIndexLedger) reads from the game,
merged in from **SamJem: IconDump**. Client only. The output folders and file formats are unchanged
from the standalone mod. The commands moved under `/emitweaks`:

```
/emitweaks export [size] [mod <id> | modRegex <regex> | match <regex>]
/emitweaks export data [emi | chipped | tabs]
/emitweaks export gamedata
/emitweaks update_icons [size] <regex>
```

| SamJem: IconDump | Now |
| --- | --- |
| `/icondump export …` | `/emitweaks export …` (without a filter, also writes the data files) |
| `/icondump data …` | `/emitweaks export data …` |
| `/icondump pack` | `/emitweaks export gamedata` (only when asked for) |
| `/icondump update …` | `/emitweaks update_icons …` |

`/emitweaks export` or `/emitweaks export <size>` with no filter writes the [data files](#data-export-data)
first, then every icon, so one command takes everything InvIndexLedger reads. A data file that
can't be written is reported and doesn't stop the icons. With a filter, it writes only icons.

### Icons (`export`, `update_icons`)

Every item, fluid and EMI stack, rendered into a few PNG spritesheets plus a `meta.json` index in
`<minecraft>/icon-sheets-x<size>/`. It does the job of
[IconExporter](https://github.com/CyclopsMC/IconExporter) (MIT), with a different output:

- **Sheets:** `sheet_000.png`, `sheet_001.png`, … (2048×2048 by default: 4,096 icons each at
  32px), instead of one file per stack.
- **Exact ids:** every tile is keyed by the id `EmiIngredientSerializers` gives it, the same string
  `emi_dump.json` lists the stack under (`item:minecraft:oak_log`).
- **Unlisted items:** registered items that neither EMI nor any creative tab shows are rendered
  too, from their default stack, and listed under `unlisted`.
- **True transparency:** each icon is drawn over black and over white, and alpha is recovered
  from the difference.
- **Speed:** icons render straight into offscreen sheets, 64 per frame. The GUI scale and window
  size make no difference.

`export` writes a fresh folder (size defaults to 32), replacing whatever was there. `mod` and
`modRegex` narrow it by namespace, `match` by stack id. `update_icons` redraws only the stacks whose id
matches `<regex>`, in place in an existing export: a stack it has is redrawn over its own tile, a
new matching stack is appended, and one that no longer exists is dropped from `meta.json`. Regexes
are Java regexes found anywhere in the id; anchor with `^`/`$` for a whole match.

```
/emitweaks export match item:minecraft:.*_log   # a standalone dump of just these
/emitweaks update_icons ^item:chipped:          # redraw just these inside the full dump
```

Esc on the progress screen cancels. A cancelled `export` leaves no `meta.json`, which readers take
to mean "incomplete". A cancelled `update_icons` leaves the export as it was. `meta.json` is always
written last and replaced by a rename:

```jsonc
{
  "format": 1,
  "generator": "sampack_emitweaks 2.1.0",
  "minecraft": "1.21.1",
  "source": "emi",            // or "creative" without EMI
  "size": 32,
  "columns": 64,              // tiles per sheet row
  "created": "2026-10-01T15:46:00Z",
  "sheets": [{"file": "sheet_000.png", "width": 2048, "height": 2048, "count": 4096}],
  "icons": {"item:minecraft:stone": [0, 0, 0]},   // id -> [sheet, x, y] in pixels
  "names": {"item:minecraft:stone": "Stone"},
  "unlisted": ["item:minecraft:debug_stick"],
  "failed": []                // ids whose render threw (see the log)
}
```

Without EMI, the stacks come from the creative tabs as their mods built them (`PristineTabs`) and
the source fluids, keyed in EMI's id shape.

### Data (`export data`)

Writes into `<minecraft>/icondump/`, all three files or the one named. A bare `/emitweaks export`
writes all three too. Run it in singleplayer: the
Chipped recipes come from the integrated server. Each file is written through a temp file and a
rename, and a part that cannot run is reported and skipped without stopping the others.

- **`emi_dump.json`** (format 3, needs EMI):
  - `added`: every stack EMI shows, in its order. With a pack deployed, this is the pack's order.
  - `index`: the same, for EMI's list before any resource pack's index data (InvIndexLedger's
    output included) removes or reorders stacks (`PristineEmiIndex`). Since EMI's index is built
    from the pristine creative tabs, this is the game's own order even with the pack deployed.
  - `components`: stack id → its component patch as a json string, for every stack in `added` or
    `index`, in the encoding the creative tab rules' `components` field reads.
  - `registry`: every registered item.
  - `tags`: item tag → members.
- **`chipped_recipes.json`** (format 2): `{"recipes": {id: recipe json}}` for every
  `chipped:workbench` recipe, generated ones included. Singleplayer only.
- **`creative_tabs.json`**: a flat array of every registered creative tab id, in NeoForge's own
  order rather than the pack's `tab_order`, without the pack's custom tabs.

### Game data (`export gamedata`)

A dump of the whole pack, for auditing it from outside the game. Nothing else runs it. Writes
`<minecraft>/icondump/pack/<section>.json`, each `{"format": 1, "data": ...}`, then `manifest.json`
with every section's count and every entry that failed. Singleplayer only.

| Section | |
|---|---|
| `mods` | mod id → name, version |
| `registries` | every id of every registry, built-in and datapack, sorted |
| `tags` | registry → tag → member ids |
| `items` | name, stack size, durability, food |
| `blocks` | name, hardness, blast resistance, its item |
| `entities`, `effects` | name, category |
| `enchantments` | name, max level, mutually exclusive enchantments |
| `creative_tabs` | name, item ids as built, and as displayed where they differ |
| `recipes` | type, result and count, ingredients (a tag kept by name; past 24 options, only the count) |
| `biomes` | placed features per step, and spawns per category, after biome modifiers |
| `dimensions` | generator, biome source, the biomes it can place |
| `structures` | generation step, biomes |
| `loot_tables` | every loot table id |

### Config

The `icon_export` section of `config/sampack_emitweaks-client.toml`, also in [EMI's config screen](#config-screen).
Settings from the old `config/samjem_icondump-client.toml` are not carried over.

| Option | Default | |
| --- | --- | --- |
| `default_size` | 32 | Icon size when the command gives none |
| `max_sheet_size` | 2048 | Sheet width/height cap (also capped by the GPU) |
| `icons_per_frame` | 64 | Icons rendered per frame |
| `include_names` | true | Write display names into `meta.json` |

The code is MIT-licensed (portions from IconExporter by rubensworks). Its notice is in
`LICENSE-IconDump` and ships inside the jar.

## Investigated, not shipped

- **REMI reload-step parallelization**: `StackManager.reload()` depends on the group definitions
  that `StackGroupManager.reload()` loads, so running the two in parallel would be a race.
- **Plugin `register()` loop parallelization**: `EmiRegistry`'s own methods would be safe to run
  concurrently. Third-party plugins can also write to EMI's public static collections directly,
  though, and those writes can't be audited across ~30 plugins.

## Building

```
./gradlew build
```

Output lands in `build/libs/`. Requires JDK 21.
