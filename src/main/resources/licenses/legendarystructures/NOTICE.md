# LegendaryStructures

- Original: LegendaryStructures [Cobblemon] 1.1.0 by bonanca, https://modrinth.com/mod/legendarystructures
- Licence: MIT (see `LICENSE` in this folder), as declared by bonanca on the Modrinth project page.
- bonanca's page describes it as an unofficial port of an earlier pack by LastGreenSeer (author of
  CobbleTowns), who is credited here as the original author. The original pack and its licence could
  not be found, so the MIT licence above is the one bonanca declared.
- Bundled in this port as built-in data under `data/legendarystructures/` and
  `data/minecraft/tags/function/tick.json`.

## Changes made for this port

The files were modified to load on Minecraft 26.1.2:

- The tick function moved from the generic `gamerules` namespace to `legendarystructures:tick`.
  The `load` function, which only set the world-wide `commandBlockOutput` game rule, was dropped.
- The `tree` marker check used player NBT fields that no longer exist (`SelectedItem`, offhand
  `Slot:-106b`, `Count`); it now uses `if items` on `weapon.mainhand` and `weapon.offhand`. It no
  longer requires the stack sizes to be exactly 1.
- The four `*_spawns` template pools listed 32 structure files that were never included in the pack.
  Those pools were removed, and the 59 jigsaw blocks that pointed at them (in `ice_cave`,
  `mesa_peak`, `power_plant_upper` and `sky_pillar`) now point at `minecraft:empty`.
- Forge-only entity attributes (`forge:entity_gravity`, `forge:step_height_addition`) removed from the
  entities saved in `team_rocket_hq.nbt`. Nothing else in the structure `.nbt` files was changed;
  Minecraft upgrades them when they load.
