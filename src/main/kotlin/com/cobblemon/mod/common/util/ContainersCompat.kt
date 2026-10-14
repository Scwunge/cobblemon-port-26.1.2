/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.Containers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/** Containers#dropContentsOnDestroy from 1.21.1 (removed along with BlockBehaviour#onRemove). */
fun dropContentsOnDestroy(state: BlockState, newState: BlockState, level: Level, pos: BlockPos) {
    if (!state.`is`(newState.block)) {
        val container = level.getBlockEntity(pos) as? Container ?: return
        Containers.dropContents(level, pos, container)
        level.updateNeighbourForOutputSignal(pos, state.block)
    }
}
