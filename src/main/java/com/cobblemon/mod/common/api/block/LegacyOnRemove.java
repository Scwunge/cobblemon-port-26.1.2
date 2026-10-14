/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockBehaviour#onRemove as it existed until 1.21.4: called on the server whenever the block's state is replaced,
 * before its block entity is removed. Vanilla split it into BlockEntity#preRemoveSideEffects and
 * BlockBehaviour#affectNeighborsAfterRemoval; Cobblemon's blocks keep their original removal logic through this
 * interface, invoked from LevelChunkMixin at the point the old method was called.
 */
public interface LegacyOnRemove {
    void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston);

    /** What BlockBehaviour#onRemove itself did: drop the block entity when the block changes. */
    static void removeBlockEntity(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            level.removeBlockEntity(pos);
        }
    }
}
