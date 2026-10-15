# PokéCenter PCs

- Original: PokéCenter PCs (Cobblemon Addon) 1.2 by Instrafe,
  https://modrinth.com/mod/pokecenter-pc-cobblemon
- Licence of the addon: MIT (see `LICENSE` in this folder), as declared on the Modrinth project page.
- The 19 changed files are Cobblemon's own village Poké Center structures with a PC added, so they
  stay under Cobblemon's Mozilla Public License 2.0 like the rest of this port. Instrafe's changes are
  credited here under MIT.
- Files: `data/cobblemon/structure/villages/*/pokecenter.nbt` and
  `data/cobblemon/structure/village_*/village_*_pokecenter.nbt`.

## Changes made for this port

- `villages/cherry/pokecenter.nbt`: removed a wild Zigzagoon that had been saved into the structure,
  so cherry villages don't all spawn the same Pokémon with the same UUID. Nothing else was changed.
- The other 18 files are unchanged from the addon; Minecraft upgrades them when they load.
