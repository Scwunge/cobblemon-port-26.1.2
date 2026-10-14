/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.color

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.color.item.ItemTintSources
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState

/**
 * An item colour provider: the colour for a tint index of an item stack, -1 for no tint.
 *
 * Minecraft's ItemColor was replaced by item model tint sources in 1.21.4. Cobblemon's providers keep this shape and
 * are exposed to item models as tint sources through [ItemColorTintSource].
 */
fun interface ItemColor {
    fun getColor(stack: ItemStack, layer: Int): Int
}

/** A block colour provider (Minecraft's BlockColor before 1.21.11). */
fun interface BlockColor {
    fun getColor(state: BlockState, level: BlockAndTintGetter?, pos: BlockPos?, tintIndex: Int): Int
}

/**
 * The tint source for one tint index of an [ItemColor]. Item models refer to it as {"type": <id>, "tint_index": <n>}.
 *
 * Tints are ARGB since 1.21.4 (ItemColor ignored the alpha), so colours are made opaque; the colour mixes can lose
 * alpha to rounding.
 */
class ItemColorTintSource(val provider: ItemColor, val tintIndex: Int, private val codec: MapCodec<ItemColorTintSource>) : ItemTintSource {
    override fun calculate(itemStack: ItemStack, level: ClientLevel?, owner: LivingEntity?) = ARGB.opaque(provider.getColor(itemStack, tintIndex))

    override fun type() = codec

    companion object {
        /** Makes [provider] available to item models as the tint source type [id]. */
        fun register(id: Identifier, provider: ItemColor) {
            lateinit var codec: MapCodec<ItemColorTintSource>
            codec = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.INT.optionalFieldOf("tint_index", 0).forGetter(ItemColorTintSource::tintIndex)
                ).apply(instance) { index -> ItemColorTintSource(provider, index, codec) }
            }
            ItemTintSources.ID_MAPPER.put(id, codec)
        }
    }
}
