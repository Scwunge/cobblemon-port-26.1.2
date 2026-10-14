/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.DepthStencilState
import net.minecraft.client.renderer.RenderPipelines

/**
 * Pipelines for Cobblemon's model layers (see PosableModel#makeLayer).
 *
 * In 1.21.1 these layers combined a vanilla entity shader with their own render state: back-face culling and depth
 * writes always on, blending only for translucent layers and no lightmap for emissive ones. Pipelines now bundle the
 * shader with that state, so each combination that has no vanilla equivalent is defined here, using the shader defines
 * vanilla uses for the corresponding 1.21.1 shader.
 */
object CobblemonRenderPipelines {
    /** rendertype_entity_translucent with culling and depth writes. */
    @JvmField
    val LAYER_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(cobblemonResource("pipeline/layer_translucent"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .build()

    /** rendertype_entity_translucent_emissive with culling and depth writes. */
    @JvmField
    val LAYER_EMISSIVE_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
        .withLocation(cobblemonResource("pipeline/layer_emissive_translucent"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .build()

    /** rendertype_entity_translucent_emissive without blending, with culling and depth writes. */
    @JvmField
    val LAYER_EMISSIVE: RenderPipeline = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
        .withLocation(cobblemonResource("pipeline/layer_emissive"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withCull(true)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .build()
}
