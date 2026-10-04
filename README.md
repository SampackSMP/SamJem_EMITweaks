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
  `c:dyes` and so on) are not loaded. The pack's groups, which InvIndexLedger writes to
  `config/remi/stack_groups/`, and any group another mod or resource pack ships, still load.
  REMI's `disabledStackGroups` list in `remi.json` no longer needs to switch the defaults off.
- **BCLib**: anvil recipes stream the hammer tag instead of every item.

The javadoc on each mixin covers the details.

## Inventory item groups (`itemgroups`, `mixin/itemgroups`)

Collapsible item groups in the creative inventory, in the style of Bedrock Edition. Click the plus
icon on a group to expand it and the minus icon to collapse it. This is SamPack's fork of
[Inventory Item Groups](https://modrinth.com/mod/inventory-item-groups) by Bizarre Cube, cut down to
NeoForge 1.21.1. It keeps the fork's performance work: per-tab caching of group matching, and index
lookup tables instead of per-slot linear scans. The code is MIT-licensed. Its notice is in
`LICENSE-InventoryItemGroups` and ships inside the jar.

The feature is client-only. Its sprites, translations and config files keep the original
`inventory_item_groups` namespace, so these carry over unchanged:
- existing `config/inventory_item_groups.json` and `config/inventory_item_groups_scl` files
- resource-pack group names (`group_name.inventory_item_groups.<name>`)

The groups use their built-in defaults unless [Simple Config Lib](https://modrinth.com/mod/simple-config-lib)
or [Cloth Config](https://modrinth.com/mod/cloth-config) is installed. Either one makes the groups
editable from this mod's config button in the mod list.

## Creative tabs (`tabs`, `mixin/tabs`)

Replaces Recreative for the pack's creative tabs. InvIndexLedger's `build` writes the rules to
`config/sampack_emitweaks/creative_tabs.json`. The mod reads that file at startup. After a build,
run `/sampack_emitweaks reload_tabs` in game, then reopen the creative inventory. It re-reads the file,
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

EMI's index and the `/icondump` dumps still see the game's own tabs: `PristineTabs` and
`PristineEmiIndex` leave the custom tabs out and ignore the hiding and ordering.

## Creative inventory layout (`creative`, `mixin/creative`)

The creative inventory can have more item columns and rows than vanilla's 9x5. A wider inventory
also fits more tabs per row, and so more tabs per page. Set the size in
`config/sampack_emitweaks-client.toml`, or in the "Creative Inventory Layout" tab of the config
screen when Cloth Config is installed:

| Option | Default | Range |
| --- | --- | --- |
| `columns` | 9 | 9 to 32 |
| `rows` | 5 | 5 to 20 |
| `fit_to_screen` | true | Shrinks the size, never below vanilla, when the window is too small for it |

These are the `creative_inventory` section. The same file's `icon_export` section holds the
[icon export](#icon-and-data-dumps-icondump) settings.

The default is the vanilla size, and at that size every hook returns vanilla's own values. A
changed size applies the next time the creative inventory opens.

The extra columns and rows are inserted into the vanilla panel by repeating a slot column and a
slot row of the tab's own background texture. Resource packs and modded tab backgrounds still
apply. The inventory tab keeps its vanilla contents in the bottom-left corner, so its hotbar lines
up with the item tabs' hotbar. Saved hotbars are padded to full rows.

Every hook changes one constant or argument, so the vanilla methods still run along with other
mods' hooks on them. The hooks were checked against the creative screen mixins of the mods in the
SamPack pack:
- **owo-lib**: custom tab textures still apply. owo reads each tab's `row()`/`column()`, which
  are reset from the actual page layout before each frame.
- **Sounds**, **Polytone**, **REMI**: unaffected.

## Icon and data dumps (`icondump`)

Everything [InvIndexLedger](https://github.com/SampackSMP/InvIndexLedger) reads from the game,
merged in from **SamJem: IconDump**. Client only. The commands, output folders and file formats are
unchanged from the standalone mod.

```
/icondump export [size] [mod <id> | modRegex <regex> | match <regex>]
/icondump update [size] <regex>
/icondump data [emi | chipped | tabs]
/icondump pack
```

### Icons (`export`, `update`)

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
`modRegex` narrow it by namespace, `match` by stack id. `update` redraws only the stacks whose id
matches `<regex>`, in place in an existing export: a stack it has is redrawn over its own tile, a
new matching stack is appended, and one that no longer exists is dropped from `meta.json`. Regexes
are Java regexes found anywhere in the id; anchor with `^`/`$` for a whole match.

```
/icondump export match item:minecraft:.*_log   # a standalone dump of just these
/icondump update ^item:chipped:                # redraw just these inside the full dump
```

Esc on the progress screen cancels. A cancelled `export` leaves no `meta.json`, which readers take
to mean "incomplete". A cancelled `update` leaves the export as it was. `meta.json` is always
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

### Data (`data`)

Writes into `<minecraft>/icondump/`, all three files or the one named. Run it in singleplayer: the
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

### Pack dump (`pack`)

Writes `<minecraft>/icondump/pack/<section>.json`, each `{"format": 1, "data": ...}`, then
`manifest.json` with every section's count and every entry that failed. Singleplayer only.

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

The `icon_export` section of `config/sampack_emitweaks-client.toml`, also on the config screen.
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
