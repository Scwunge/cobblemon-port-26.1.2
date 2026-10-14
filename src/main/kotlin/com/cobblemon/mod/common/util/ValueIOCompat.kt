/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import java.util.stream.Stream
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/*
 * Since 1.21.6 entities and block entities save through ValueOutput/ValueInput instead of a CompoundTag. Cobblemon's
 * save code builds CompoundTags, so these helpers move a whole compound's entries in and out of a value output/input.
 * Entries end up at the same place they always did, keeping existing worlds loadable.
 */

/** A map codec covering every entry of the enclosing compound. */
object WholeCompoundCodec : MapCodec<CompoundTag>() {
    override fun <T> keys(ops: DynamicOps<T>): Stream<T> = Stream.empty()

    override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<CompoundTag> {
        val tag = CompoundTag()
        input.entries().forEach { entry ->
            ops.getStringValue(entry.first).result().ifPresent { key ->
                tag.put(key, ops.convertTo(NbtOps.INSTANCE, entry.second))
            }
        }
        return DataResult.success(tag)
    }

    override fun <T> encode(input: CompoundTag, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T> {
        input.keySet().forEach { key -> prefix.add(key, NbtOps.INSTANCE.convertTo(ops, input.get(key)!!)) }
        return prefix
    }
}

/** Writes every entry of [tag] into this output. */
fun ValueOutput.putAll(tag: CompoundTag) {
    store(WholeCompoundCodec, tag)
}

/** All entries of this input as a CompoundTag. */
fun ValueInput.asCompoundTag(): CompoundTag = read(WholeCompoundCodec).orElseGet(::CompoundTag)

/**
 * Registries for block entity save code that used to receive them as a parameter of saveAdditional. Block entities
 * are saved while in a level; the server's registries are the fallback.
 */
fun net.minecraft.world.level.block.entity.BlockEntity.blockEntityRegistries(): net.minecraft.core.HolderLookup.Provider =
    level?.registryAccess()
        ?: com.cobblemon.mod.common.Cobblemon.implementation.server()?.registryAccess()
        ?: net.minecraft.core.RegistryAccess.EMPTY
