/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.net.serializers

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer

/** Replacement for vanilla's EntityDataSerializers.COMPOUND_TAG, removed in 1.21.5. */
object CompoundTagDataSerializer : EntityDataSerializer<CompoundTag> {
    override fun copy(value: CompoundTag): CompoundTag = value.copy()
    override fun codec(): StreamCodec<RegistryFriendlyByteBuf, CompoundTag> = ByteBufCodecs.COMPOUND_TAG.cast()
}
