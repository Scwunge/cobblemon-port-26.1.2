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
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * ItemRenderer#renderStatic for render code written against the 1.21.1 API.
 *
 * Items are resolved to an item render state and submitted since 1.21.4. When the buffer source is a
 * [DeferredBufferSource] the item is submitted with the rest of the frame; otherwise (immediate rendering, e.g. into a
 * picture-in-picture texture) it is submitted to the feature dispatcher and drawn right away, as GuiEntityRenderer does.
 */
object LegacyItemRendering {
    fun resolve(stack: ItemStack, displayContext: ItemDisplayContext, level: Level?, owner: ItemOwner?, seed: Int): ItemStackRenderState {
        val state = ItemStackRenderState()
        Minecraft.getInstance().itemModelResolver.updateForTopItem(state, stack, displayContext, level, owner, seed)
        return state
    }

    fun renderStatic(
        owner: ItemOwner?,
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        level: Level?,
        light: Int,
        overlay: Int,
        seed: Int
    ) {
        if (stack.isEmpty) return
        submit(resolve(stack, displayContext, level, owner, seed), poseStack, bufferSource, light, overlay)
    }

    fun renderStatic(stack: ItemStack, displayContext: ItemDisplayContext, light: Int, overlay: Int, poseStack: PoseStack, bufferSource: MultiBufferSource, level: Level?, seed: Int) =
        renderStatic(null, stack, displayContext, poseStack, bufferSource, level, light, overlay, seed)

    fun submit(state: ItemStackRenderState, poseStack: PoseStack, bufferSource: MultiBufferSource, light: Int, overlay: Int) {
        if (state.isEmpty) return
        withCollector(bufferSource) { collector -> state.submit(poseStack, collector, light, overlay, 0) }
    }

    /** Runs [submit] with the collector for [bufferSource], drawing immediately when rendering isn't deferred. */
    fun withCollector(bufferSource: MultiBufferSource, submit: (SubmitNodeCollector) -> Unit) {
        if (bufferSource is DeferredBufferSource) {
            submit(bufferSource.submitNodeCollector)
        } else {
            val dispatcher = Minecraft.getInstance().gameRenderer.featureRenderDispatcher
            submit(dispatcher.submitNodeStorage)
            dispatcher.renderAllFeatures()
        }
    }
}
