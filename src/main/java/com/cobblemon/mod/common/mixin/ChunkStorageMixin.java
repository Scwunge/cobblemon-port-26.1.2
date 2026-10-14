/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static com.cobblemon.mod.common.datafixer.CobblemonSchemas.VERSION_KEY;

import com.cobblemon.mod.common.datafixer.CobblemonSchemas;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.SavedDataStorage;

/*
 * Chunk upgrading moved out of ChunkStorage in 1.21.9: ChunkMap#upgradeChunkTag upgrades a chunk to the current
 * version through SimpleRegionStorage (which leaves current chunks untouched). Cobblemon's fixes run on the result,
 * which covers both the outdated chunks this used to fix after vanilla and current chunks it fixed on their own.
 */
@Mixin(ChunkMap.class)
public class ChunkStorageMixin {
    @ModifyReturnValue(method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    public CompoundTag cobblemon$doChunkFix(CompoundTag vanillaFixed) {
        int curVersion = vanillaFixed.getIntOr(VERSION_KEY, 0);
        return DataFixTypes.CHUNK.update(CobblemonSchemas.getDATA_FIXER(), vanillaFixed, curVersion, CobblemonSchemas.DATA_VERSION);
    }
}
