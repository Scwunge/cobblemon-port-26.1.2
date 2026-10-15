# The loot is spawned instead of killed by a villager
loot spawn ~ ~ ~ loot cobbletowns:entities/house
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{raticate32:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon raticate level=32
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{raticate34:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon raticate level=34
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{litwick29:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon litwick level=29
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{litwick30:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon litwick level=30
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{litwick32:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon litwick level=32
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{lampent34:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon lampent level=34
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{sigilyph35:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon sigilyph level=35
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{yamask29:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon yamask level=29
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{yamask31:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon yamask level=31
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{elgyem30:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon elgyem level=30
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{elgyem34:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon elgyem level=34
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{golbat32:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon golbat level=32
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{golbat33:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..20] run spawnpokemon golbat level=33
execute as @e[type=item,nbt={Item:{id:"minecraft:ender_pearl",components:{"minecraft:custom_data":{pokemon_spawn_pearl:1b}}}},limit=1,sort=nearest] at @s if entity @p[distance=..22] run kill @s
# cooldown length is taken directly from the marker entity's data nbt.
execute store result score @s pokemon_respawn_cooldown run data get entity @s data.cooldown_length
data modify entity @s data.last_spawned_uuid set from entity @e[type=cobblemon:pokemon,limit=1,sort=nearest] UUID