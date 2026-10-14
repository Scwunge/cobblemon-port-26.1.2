/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.layer

import com.cobblemon.mod.common.client.render.CobblemonRenderPipelines
import com.cobblemon.mod.common.util.cobblemonResource
import java.util.function.BiFunction
import java.util.function.Function
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier

object CobblemonRenderLayers {
    /** Berry models: the block cutout shader on the berry atlas, lit, not culled, affecting block breaking overlays. */
    val BERRY_LAYER: RenderType = RenderType.create(
        "berries",
        RenderSetup.builder(CobblemonRenderPipelines.BERRY)
            .withTexture("Sampler0", cobblemonResource("textures/atlas/berries.png"))
            .useLightmap()
            .affectsCrumbling()
            .createRenderSetup()
    )

    /** The same render type as vanilla's entity translucent one, which is what this was in 1.21.1. */
    val ENTITY_TRANSLUCENT: BiFunction<Identifier, Boolean, RenderType> = BiFunction { texture, affectsOutline ->
        RenderTypes.entityTranslucent(texture, affectsOutline)
    }

    /** Vanilla's culled entity cutout render type (entityCutout in 1.21.1, entityCutoutCull since 1.21.9). */
    val ENTITY_CUTOUT: Function<Identifier, RenderType> = Function { texture -> RenderTypes.entityCutoutCull(texture) }
}
