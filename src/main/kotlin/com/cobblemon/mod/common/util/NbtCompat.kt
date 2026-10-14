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
