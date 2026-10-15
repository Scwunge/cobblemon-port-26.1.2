execute as @e[type=marker,tag=pillar] at @s run execute as @p[distance=..10] run setblock ~ ~3 ~ minecraft:redstone_torch
execute as @e[type=marker,tag=power] at @s run execute at @e[distance=..5,nbt={Item:{id:"cobblemon:thunder_stone"}}] run setblock ~ ~-3 ~ minecraft:redstone_torch
execute as @e[type=marker,tag=ice] at @s run execute at @e[distance=..4,nbt={Item:{id:"cobblemon:ice_stone"}}] run setblock ~ ~-5 ~ minecraft:redstone_torch
execute as @e[type=marker,tag=fire] at @s run execute at @e[distance=..4,nbt={Item:{id:"cobblemon:fire_stone"}}] run setblock ~ ~-5 ~ minecraft:redstone_torch
execute as @e[type=marker,tag=netherite] at @s run kill @e[type=item,distance=..100,nbt={Item:{id:"minecraft:netherite_block"}}]
execute as @e[type=marker,tag=beacon] at @s run kill @e[type=item,distance=..50,nbt={Item:{id:"minecraft:beacon"}}]
execute as @e[type=marker,tag=emerald] at @s run kill @e[type=item,distance=..50,nbt={Item:{id:"minecraft:emerald_block"}}]
execute as @e[type=marker,tag=tree] at @s as @a[distance=..20] if items entity @s weapon.mainhand cobblemon:dawn_stone if items entity @s weapon.offhand minecraft:totem_of_undying run setblock ~ ~1 ~ minecraft:redstone_torch
execute as @e[type=marker,tag=hq] at @s run execute as @p[distance=..18] run setblock ~ ~1 ~ minecraft:redstone_torch
