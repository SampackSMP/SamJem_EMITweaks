# SamPack EMI Tweaks

Load-time and EMI-reload fixes for the SamPack modpack on NeoForge 1.21.1. Each fix produces the
same result the original mod would have produced. It only removes redundant work, or a crash, along
the way. Every fix is a mixin into one specific mod. If that mod isn't installed, its mixin is
skipped.

This mod merges and replaces **Sampack_CreativeTabFix** (`sampack_tabfix`), **EMI Reclocked**
(`emireclocked`) and **SamJem_InventoryItemGroups** (`inventory_item_groups`). Remove those jars when
you install this one. The mod declares `inventory_item_groups` incompatible, so a leftover copy is
reported at startup instead of applying its mixins twice.

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
