/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokenav

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphicsExtractor
import com.mojang.blaze3d.vertex.PoseStack
import org.joml.Matrix3x2fStack
import com.cobblemon.mod.common.client.render.gui.pushPose
import com.cobblemon.mod.common.client.render.gui.popPose
import com.cobblemon.mod.common.client.render.gui.translate
import com.cobblemon.mod.common.client.render.gui.scale
import com.cobblemon.mod.common.client.render.gui.mulPose
class PokeNavFillerButton(
    posX: Int, posY: Int,
    pX: Int, pY: Int,
    pWidth: Int, pHeight: Int,
    pXTexStart: Int, pYTexStart: Int, pYDiffText: Int,
    pTextureWidth: Int, pTextureHeight: Int
): PokeNavImageButton(posX, posY, pX, pY, pWidth, pHeight, pXTexStart, pYTexStart, pYDiffText, FILLER, pTextureWidth, pTextureHeight, {}, "".text()) {

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        this.applyBlitk(context.pose(), pMouseX, pMouseY, pPartialTicks)
        context.pose().pushPose()
    }

    override fun applyBlitk(matrices: Matrix3x2fStack, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        blitk(
            matrixStack = matrices,
            texture = FILLER,
            x = x, y = y + 0.25,
            width = width, height = height,
            red = RED,
            green = GREEN,
            blue = BLUE,
            alpha = ALPHA
        )
    }

    companion object {

        const val RED = .28235
        const val GREEN = .29412
        const val BLUE = .30980
        const val ALPHA = .9

        private val FILLER = cobblemonResource("textures/gui/pokenav/pokenav_filler.png")

    }

}