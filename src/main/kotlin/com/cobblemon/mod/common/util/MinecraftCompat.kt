/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

/*
 * Small bridges for vanilla members that were renamed or removed between 1.21.1 and 26.1, keeping the old semantics so
 * call sites can stay as they were.
 */

/** The item holder of this stack (ItemStack#getItemHolder before 1.21.2). */
val ItemStack.itemHolder: Holder<Item>
    get() = typeHolder()

/** The block holder of this state. */
val BlockState.blockHolder: Holder<Block>
    get() = typeHolder()

/** Item#getDescription from before 1.21.2. */
val Item.description: Component
    get() = Component.translatable(descriptionId)

/** The server this entity is in, if any (Entity#getServer before 1.21.9). */
val Entity.server: MinecraftServer?
    get() = level().server

/** ServerPlayer#server, which became private in 1.21.9. */
val net.minecraft.server.level.ServerPlayer.server: MinecraftServer
    get() = level().server

/** The exclusive upper build limit (Level#getMaxBuildHeight before 1.21.2). */
val LevelHeightAccessor.maxBuildHeight: Int
    get() = maxY + 1

/** The inclusive lower build limit (Level#getMinBuildHeight before 1.21.2). */
val LevelHeightAccessor.minBuildHeight: Int
    get() = minY

/** Level#isNight before 1.21.11. */
val Level.isNight: Boolean
    get() = isDarkOutside

/** Level#blockUpdated before 1.21.2. */
fun net.minecraft.world.level.LevelAccessor.blockUpdated(pos: BlockPos, block: Block) = updateNeighborsAt(pos, block)

/** EntityGetter#getNearbyPlayers before 1.21.2. */
fun Level.getNearbyPlayers(conditions: TargetingConditions, source: LivingEntity?, area: AABB): List<Player> {
    return players().filter { player ->
        area.contains(player.x, player.y, player.z) && (this !is ServerLevel || conditions.test(this, source, player))
    }
}

val UniformInt.minValue: Int
    get() = minInclusive()

val UniformInt.maxValue: Int
    get() = maxInclusive()

private fun commandLevel(level: Int): Permission = Permission.HasCommandLevel(PermissionLevel.byId(level))

/** CommandSourceStack#hasPermission(int) before 1.21.11. */
fun CommandSourceStack.hasPermission(level: Int): Boolean = permissions().hasPermission(commandLevel(level))

/** Player#hasPermissions(int) before 1.21.11. */
fun Player.hasPermissions(level: Int): Boolean = permissions().hasPermission(commandLevel(level))

/** Player#playNotifySound before 1.21.2: plays a sound only this player hears. */
fun Player.playNotifySound(sound: SoundEvent, source: SoundSource, volume: Float, pitch: Float) {
    if (this is ServerPlayer) {
        connection.send(ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, x, y, z, volume, pitch, random.nextLong()))
    }
}

/** Player#displayClientMessage before 1.21.11. */
fun Player.displayClientMessage(message: Component, overlay: Boolean) {
    if (overlay) sendOverlayMessage(message) else sendSystemMessage(message)
}

/** Style#withFont(ResourceLocation) before 1.21.9: a font from a font definition. */
fun net.minecraft.network.chat.Style.withFont(font: net.minecraft.resources.Identifier): net.minecraft.network.chat.Style =
    withFont(net.minecraft.network.chat.FontDescription.Resource(font))

/**
 * ItemStack#getEatingSound before 1.21.2: the item's eating sound. Eating sounds belong to consumables since then, so
 * this is the consumable's sound when it is eaten (rather than drunk), Cobblemon's own eating sound for items that
 * declare one, and the generic eating sound otherwise, as Item#getEatingSound defaulted to.
 */
val ItemStack.eatingSound: net.minecraft.sounds.SoundEvent
    get() {
        val item = item
        if (item is com.cobblemon.mod.common.item.LegacyUseEffectsItem) return item.getEatingSound()
        val consumable = get(net.minecraft.core.component.DataComponents.CONSUMABLE)
        if (consumable != null && consumable.animation() == net.minecraft.world.item.ItemUseAnimation.EAT) return consumable.sound().value()
        return net.minecraft.sounds.SoundEvents.GENERIC_EAT.value()
    }

/** EntityType#is(TagKey) before 1.21.2. */
fun net.minecraft.world.entity.EntityType<*>.`is`(tag: net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<*>>): Boolean =
    builtInRegistryHolder().`is`(tag)

/** Entity#sendSystemMessage before 1.21.9: a no-op for everything but players. */
fun Entity.sendSystemMessage(message: Component) {
    if (this is Player) this.sendSystemMessage(message)
}

/** Level#getSharedSpawnPos before 1.21.9. */
val Level.sharedSpawnPos: BlockPos
    get() = respawnData.globalPos().pos()

/** Entity#getCommandSenderWorld before 1.21.9. */
val Entity.commandSenderWorld: Level
    get() = level()

/** FriendlyByteBuf#writeVec3 / readVec3 before 1.21.2: three doubles. */
fun net.minecraft.network.FriendlyByteBuf.writeVec3(vec: net.minecraft.world.phys.Vec3) {
    writeDouble(vec.x)
    writeDouble(vec.y)
    writeDouble(vec.z)
}

fun net.minecraft.network.FriendlyByteBuf.readVec3() = net.minecraft.world.phys.Vec3(readDouble(), readDouble(), readDouble())

/** Component.Serializer#toJson / fromJson before 1.21.5: a component as a JSON string, with registry access. */
object ComponentJson {
    fun toJson(component: Component, registries: net.minecraft.core.HolderLookup.Provider): String =
        com.google.gson.Gson().toJson(
            net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), component).getOrThrow()
        )

    fun fromJson(json: String, registries: net.minecraft.core.HolderLookup.Provider): Component? {
        val element = com.google.gson.JsonParser.parseString(json) ?: return null
        return net.minecraft.network.chat.ComponentSerialization.CODEC.parse(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), element).getOrThrow()
    }
}

/**
 * Level#getGameRules before 1.21.11. Only server levels have game rules since then, and every caller of this runs on the
 * server.
 */
val Level.gameRules: net.minecraft.world.level.gamerules.GameRules
    get() = (this as net.minecraft.server.level.ServerLevel).gameRules

/**
 * BeehiveBlockEntity#addOccupant(Entity) from 1.21.1, which took any entity; it only takes bees since 1.21.2. This is
 * the same, including skipping the hive's fire check when marking it changed.
 */
fun net.minecraft.world.level.block.entity.BeehiveBlockEntity.addOccupant(entity: Entity) {
    if (isFull) return
    entity.stopRiding()
    entity.ejectPassengers()
    storeBee(net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant.of(entity))
    val level = level
    if (level != null) {
        val blockPos = blockPos
        level.playSound(null, blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), net.minecraft.sounds.SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F)
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, blockPos, net.minecraft.world.level.gameevent.GameEvent.Context.of(entity, blockState))
    }
    entity.discard()
    if (level != null) {
        level.blockEntityChanged(blockPos)
        if (!blockState.isAir) level.updateNeighbourForOutputSignal(blockPos, blockState.block)
    }
}
