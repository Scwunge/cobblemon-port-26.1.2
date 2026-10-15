# Cobblemon — Fabric port to Minecraft 26.1.2

Port of [Cobblemon](https://gitlab.com/cable-mc/cobblemon) **1.8.2** (upstream commit
`643bf9068b51c829316429916373e5aa9864b8b9`, originally Minecraft 1.21.1) to **Minecraft 26.1.2 on Fabric**.

The first commit in this repository is the upstream 1.8.2 source, unmodified, flattened from the
Architectury `common` + `fabric` modules into a single Fabric project (the NeoForge module is dropped).
Every later commit is port work, so `git diff <first-commit>` shows exactly what changed.

## Licence

- **Code** is licensed under the Mozilla Public License 2.0, the same as upstream (see `LICENSE`).
  This is a modified version of Cobblemon by the Cobblemon Contributors; the port changes are in the
  commit history. Every source file keeps its MPL header, and anyone who gets a build of this port
  can get its source here, under the same licence.
- **Pokémon models and animations** (`src/main/resources/assets/cobblemon/bedrock/pokemon/`) are
  licensed by their authors under Creative Commons Attribution-NonCommercial 3.0; each model folder
  has its own `license` file. They may not be used for commercial purposes, including paid or
  paywalled builds of this port.
- **Bundled datapacks** are included as built-in data, modified to load on 26.1.2 (each folder in
  `src/main/resources/licenses/` has the licence and lists the changes):
  - [CobbleTowns](https://modrinth.com/mod/cobbletowns) by lastgreenseer, MIT
    (`src/main/resources/licenses/cobbletowns/`).
- This is an unofficial fan project, not affiliated with or endorsed by the Cobblemon team, Nintendo,
  Game Freak, The Pokémon Company or Mojang.
