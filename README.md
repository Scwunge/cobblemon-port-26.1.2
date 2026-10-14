# Cobblemon — Fabric port to Minecraft 26.1.2

Port of [Cobblemon](https://gitlab.com/cable-mc/cobblemon) **1.8.2** (upstream commit
`643bf9068b51c829316429916373e5aa9864b8b9`, originally Minecraft 1.21.1) to **Minecraft 26.1.2 on Fabric**.

The first commit in this repository is the upstream 1.8.2 source, unmodified, flattened from the
Architectury `common` + `fabric` modules into a single Fabric project (the NeoForge module is dropped).
Every later commit is port work, so `git diff <first-commit>` shows exactly what changed.

Licensed MPL-2.0, same as upstream (see `LICENSE`).
