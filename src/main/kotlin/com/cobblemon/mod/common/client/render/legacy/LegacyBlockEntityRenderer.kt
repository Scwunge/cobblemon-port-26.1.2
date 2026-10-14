/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.legacy

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

/** Render state carrying what the 1.21.1 block entity render method received. */
open class LegacyBlockEntityRenderState : BlockEntityRenderState() {
    var blockEntity: BlockEntity? = null
    var partialTicks = 0F
}

/**
 * BlockEntityRenderer as it was in 1.21.1, run on top of the extract/submit renderer API (see [LegacyEntityRenderer]
 * for the approach).
 */
interface LegacyBlockEntityRenderer<T : BlockEntity> : BlockEntityRenderer<T, LegacyBlockEntityRenderState> {
    fun render(blockEntity: T, partialTick: Float, poseStack: PoseStack, multiBufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int)

    override fun createRenderState() = LegacyBlockEntityRenderState()

    override fun extractRenderState(
        blockEntity: T,
        state: LegacyBlockEntityRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.blockEntity = blockEntity
        state.partialTicks = partialTicks
    }

    @Suppress("UNCHECKED_CAST")
    override fun submit(state: LegacyBlockEntityRenderState, poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, camera: CameraRenderState) {
        val blockEntity = state.blockEntity as? T ?: return
        // BlockEntityRenderDispatcher always passed NO_OVERLAY.
        render(blockEntity, state.partialTicks, poseStack, DeferredBufferSource(submitNodeCollector), state.lightCoords, OverlayTexture.NO_OVERLAY)
    }
}
