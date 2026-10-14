/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.resources.Identifier

/**
 * A model that is baked on its own (not as part of a block state or item) so renderers can draw it directly.
 *
 * Such models were registered under a ModelResourceLocation before 1.21.4; they are Fabric extra models now.
 */
data class BakingOverride(val modelLocation: Identifier) {
    val key: ExtraModelKey<BlockStateModel> = ExtraModelKey.create { modelLocation.toString() }

    fun getModel(): BlockStateModel {
        return Minecraft.getInstance().modelManager.getModel(key) ?: error("Model $modelLocation was not baked")
    }
}
