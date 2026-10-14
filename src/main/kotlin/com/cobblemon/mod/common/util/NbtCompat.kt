/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import java.util.UUID
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import java.util.Optional
import net.minecraft.core.BlockPos
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.NbtOps
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack

/*
 * CompoundTag lost its UUID helpers in 1.21.5. These keep Cobblemon's saved data format identical: UUIDs are stored as
 * a 4-int array, exactly as NbtUtils#createUUID/loadUUID did.
 */

fun CompoundTag.putUUID(key: String, uuid: UUID) {
    store(key, UUIDUtil.CODEC, uuid)
}

/** Throws when the key is missing or malformed, as CompoundTag#getUUID did. */
fun CompoundTag.getUUID(key: String): UUID = read(key, UUIDUtil.CODEC).orElseThrow { IllegalArgumentException("Expected UUID-Tag at '$key'") }

fun CompoundTag.hasUUID(key: String): Boolean = (get(key) as? IntArrayTag)?.size == 4

val CompoundTag.allKeys: Set<String>
    get() = keySet()

/** NbtUtils#writeBlockPos from 1.21.1: a 3-int array, the same encoding as BlockPos.CODEC. */
fun writeBlockPos(pos: BlockPos): Tag = IntArrayTag(intArrayOf(pos.x, pos.y, pos.z))

/** NbtUtils#readBlockPos from 1.21.1. */
fun CompoundTag.readBlockPos(key: String): Optional<BlockPos> = read(key, BlockPos.CODEC)

/** ItemStack#saveOptional from 1.21.1: an empty compound for empty stacks, the ItemStack codec's encoding otherwise. */
fun ItemStack.saveOptional(registries: HolderLookup.Provider): Tag =
    if (isEmpty) CompoundTag() else ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow()

/** ItemStack#parseOptional from 1.21.1: an empty stack for an empty or unreadable tag. */
fun parseOptionalItemStack(registries: HolderLookup.Provider, tag: CompoundTag): ItemStack =
    if (tag.isEmpty) ItemStack.EMPTY else ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
        .resultOrPartial { LOGGER.error("Tried to load invalid item: '{}'", it) }
        .orElse(ItemStack.EMPTY)

private val LOGGER = org.slf4j.LoggerFactory.getLogger("Cobblemon/NbtCompat")

/**
 * ContainerHelper#saveAllItems from 1.21.1: an "Items" list of the non-empty stacks, each tagged with its "Slot" byte.
 */
fun saveAllItems(tag: CompoundTag, items: net.minecraft.core.NonNullList<ItemStack>, alwaysPutTag: Boolean, registries: HolderLookup.Provider): CompoundTag {
    val list = net.minecraft.nbt.ListTag()
    for (i in items.indices) {
        val stack = items[i]
        if (!stack.isEmpty) {
            val entry = CompoundTag()
            entry.putByte("Slot", i.toByte())
            entry.merge(stack.saveOptional(registries) as CompoundTag)
            list.add(entry)
        }
    }
    if (!list.isEmpty() || alwaysPutTag) {
        tag.put("Items", list)
    }
    return tag
}

fun saveAllItems(tag: CompoundTag, items: net.minecraft.core.NonNullList<ItemStack>, registries: HolderLookup.Provider) = saveAllItems(tag, items, true, registries)

/** ContainerHelper#loadAllItems from 1.21.1. */
fun loadAllItems(tag: CompoundTag, items: net.minecraft.core.NonNullList<ItemStack>, registries: HolderLookup.Provider) {
    for (entry in tag.getListOrEmpty("Items")) {
        val compound = entry as? CompoundTag ?: continue
        val slot = compound.getByteOr("Slot", 0).toInt() and 255
        if (slot in items.indices) {
            items[slot] = parseOptionalItemStack(registries, compound)
        }
    }
}
