/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.legacy

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.core.Direction
import net.minecraft.util.ARGB
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState

/*
 * BakedModel helpers from 1.21.1 for block models, which are BlockStateModels made of parts since 1.21.5.
 */

/** BakedModel#getQuads: the quads of every part of the model facing [side] (null for the faces without a cull face). */
fun BlockStateModel.getQuads(state: BlockState?, side: Direction?, random: RandomSource?): List<BakedQuad> {
    val parts = mutableListOf<BlockStateModelPart>()
    collectParts(random ?: RandomSource.create(42L), parts)
    return parts.flatMap { it.getQuads(side) }
}

/** VertexConsumer#putBulkData: a quad drawn with a uniform colour, light and overlay (no tint, no shading). */
fun VertexConsumer.putBulkData(pose: PoseStack.Pose, quad: BakedQuad, red: Float, green: Float, blue: Float, alpha: Float, light: Int, overlay: Int) {
    val instance = QuadInstance()
    instance.setColor(ARGB.colorFromFloat(alpha, red, green, blue))
    instance.setLightCoords(light)
    instance.setOverlayCoords(overlay)
    putBakedQuad(pose, quad, instance)
}

/** The model of a block state (BlockRenderDispatcher#getBlockModel before 1.21.5). */
fun blockModelOf(state: BlockState): BlockStateModel =
    net.minecraft.client.Minecraft.getInstance().modelManager.blockStateModelSet.get(state)

/**
 * ModelBlockRenderer#renderModel from 1.21.1: every quad of the model at full brightness, tinted quads coloured with
 * [red]/[green]/[blue]. Each quad goes to the moving-block render type of its chunk layer, which is what
 * ItemBlockRenderTypes#getRenderType(state, false) chose per block before layers were per quad.
 */
fun renderModel(
    pose: PoseStack.Pose,
    bufferSource: net.minecraft.client.renderer.MultiBufferSource,
    state: BlockState,
    model: BlockStateModel,
    red: Float,
    green: Float,
    blue: Float,
    light: Int,
    overlay: Int
) {
    val random = RandomSource.create(42L)
    for (side in Direction.entries + listOf<Direction?>(null)) {
        random.setSeed(42L)
        for (quad in model.getQuads(state, side, random)) {
            val tinted = quad.materialInfo().isTinted()
            val r = if (tinted) red.coerceIn(0F, 1F) else 1F
            val g = if (tinted) green.coerceIn(0F, 1F) else 1F
            val b = if (tinted) blue.coerceIn(0F, 1F) else 1F
            val buffer = bufferSource.getBuffer(net.fabricmc.fabric.api.client.renderer.v1.render.ChunkSectionLayerHelper.getMovingBlockRenderType(quad.materialInfo().layer()))
            buffer.putBulkData(pose, quad, r, g, b, 1F, light, overlay)
        }
    }
}
