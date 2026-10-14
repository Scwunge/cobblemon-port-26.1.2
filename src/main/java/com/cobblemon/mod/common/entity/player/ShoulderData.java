/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.player;

import com.cobblemon.mod.common.api.net.serializers.CompoundTagDataSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;

/**
 * Synced copies of a player's shoulder entity data.
 *
 * Up to 1.21.4 players synced the full NBT of their shoulder entities to clients, which is what Cobblemon renders
 * shouldered Pokémon from. Vanilla now only syncs parrot variants, so Cobblemon syncs the data itself. Defined from
 * Player's static initializer (PlayerMixin) and kept up to date by ServerPlayerMixin.
 */
public final class ShoulderData {
    public static final EntityDataAccessor<CompoundTag> LEFT = SynchedEntityData.defineId(Player.class, CompoundTagDataSerializer.INSTANCE);
    public static final EntityDataAccessor<CompoundTag> RIGHT = SynchedEntityData.defineId(Player.class, CompoundTagDataSerializer.INSTANCE);

    private ShoulderData() {
    }

    /** Forces class initialization, defining the accessors. */
    public static void init() {
    }
}
