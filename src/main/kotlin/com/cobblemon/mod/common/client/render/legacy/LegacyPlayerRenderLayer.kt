/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.legacy

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.player.Player

/**
 * A player render layer written against the 1.21.1 RenderLayer contract: it renders with the player entity and a
 * buffer source when the player is drawn. Layers are submitted from render states since 1.21.9; this looks the player
 * up from the state and records what the layer draws, see [DeferredBufferSource].
 */
abstract class LegacyPlayerRenderLayer(parent: RenderLayerParent<AvatarRenderState, PlayerModel>) : RenderLayer<AvatarRenderState, PlayerModel>(parent) {
    /** The render state of the player currently being rendered, for parent model calls that need it. */
    protected lateinit var renderState: AvatarRenderState

    final override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        state: AvatarRenderState,
        yRot: Float,
        xRot: Float
    ) {
        val player = Minecraft.getInstance().level?.getEntity(state.id) as? Player ?: return
        val partialTicks = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(!player.level().tickRateManager().isEntityFrozen(player))
        renderState = state
        render(
            poseStack,
            DeferredBufferSource(submitNodeCollector),
            lightCoords,
            player,
            state.walkAnimationPos,
            state.walkAnimationSpeed,
            partialTicks,
            state.ageInTicks,
            yRot,
            xRot
        )
    }

    abstract fun render(
        matrixStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        player: Player,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    )
}
