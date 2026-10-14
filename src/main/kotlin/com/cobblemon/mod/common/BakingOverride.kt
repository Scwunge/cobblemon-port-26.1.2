/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.Identifier

/**
 * Contains information for forcing a model to be baked
 *
 * @param modelLocation The location of the model
 * @param modelIdentifier The identifier that the BlockStateModel will be registered to
 */
data class BakingOverride(
    val modelLocation: Identifier,
    val modelIdentifier: ModelResourceLocation
) {
    fun getModel(): BlockStateModel {
        return Minecraft.getInstance().modelManager.getModel(modelIdentifier)
    }
}
