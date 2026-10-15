# CobbleTowns

- Original: CobbleTowns 1.0.2 by lastgreenseer, https://modrinth.com/mod/cobbletowns
- Licence: MIT (see `LICENSE` in this folder), as declared on the Modrinth project page.
- Bundled in this port as built-in data under `data/cobbletowns/` and `data/minecraft/tags/function/`.

## Changes made for this port

The files were modified to load on Minecraft 26.1.2:

- Folders renamed to the current layout (`structures` to `structure`, `loot_tables` to
  `loot_table`, `functions` to `function`, `tags/functions` to `tags/function`).
- The pack's helper loot tables moved from `minecraft:cobblemon/*` to `cobbletowns:cobblemon/*`, and
  their invalid table type `loot_table` changed to `minecraft:chest`.
- Nested loot table entries use `value` instead of `name`.
- `set_nbt` loot functions replaced with `set_custom_data`, and the matching item selectors in the
  spawn functions now check the `minecraft:custom_data` component.
- `minecraft:grass` in loot tables renamed to `minecraft:short_grass`.
- The `load` function no longer sets the world-wide `commandBlockOutput` game rule.
- Structure `.nbt` files are unchanged; Minecraft and Cobblemon upgrade them when they load.
