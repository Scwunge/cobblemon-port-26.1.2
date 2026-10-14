/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay.Companion.PORTRAIT_DIAMETER
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.gui.CobblemonModelPipState
import com.cobblemon.mod.common.client.render.gui.GuiModelLighting
import com.cobblemon.mod.common.client.render.gui.blitFloat
import com.cobblemon.mod.common.client.render.gui.graphics
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.util.toHex
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.LightCoordsUtil
import org.joml.Matrix3x2fStack
import org.joml.Vector3f
import com.cobblemon.mod.common.client.render.gui.pushPose
import com.cobblemon.mod.common.client.render.gui.popPose
import com.cobblemon.mod.common.client.render.gui.translate
import com.cobblemon.mod.common.client.render.gui.scale
import com.cobblemon.mod.common.client.render.gui.mulPose

@JvmOverloads
fun blitk(
    matrixStack: Matrix3x2fStack,
    texture: Identifier? = null,
    x: Number,
    y: Number,
    height: Number = 0,
    width: Number = 0,
    uOffset: Number = 0,
    vOffset: Number = 0,
    textureWidth: Number = width,
    textureHeight: Number = height,
    blitOffset: Number = 0,
    red: Number = 1,
    green: Number = 1,
    blue: Number = 1,
    alpha: Number = 1F,
    blend: Boolean = true,
    scale: Float = 1F
) {
    // GUI rendering is deferred and always alpha blended now; blitOffset (z) and blend no longer apply.
    texture ?: return
    val graphics = matrixStack.graphics
    val colour = ARGB.colorFromFloat(alpha.toFloat(), red.toFloat(), green.toFloat(), blue.toFloat())
    matrixStack.pushMatrix()
    matrixStack.scale(scale, scale)
    graphics.blitFloat(
        texture,
        x.toFloat(), y.toFloat(), x.toFloat() + width.toFloat(), y.toFloat() + height.toFloat(),
        uOffset.toFloat() / textureWidth.toFloat(), (uOffset.toFloat() + width.toFloat()) / textureWidth.toFloat(),
        vOffset.toFloat() / textureHeight.toFloat(), (vOffset.toFloat() + height.toFloat()) / textureHeight.toFloat(),
        colour
    )
    matrixStack.popMatrix()
}

/**
 * Before 1.21.6, Font treated colours with (almost) no alpha as fully opaque, and Cobblemon relies on that by passing
 * plain RGB values. Text with zero alpha is no longer drawn, so restore the old behaviour.
 */
fun legacyTextColour(colour: Int): Int = if ((colour and 0xFC000000.toInt()) == 0) colour or 0xFF000000.toInt() else colour

@JvmOverloads
fun drawCenteredText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: Component,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = (text as MutableComponent).let { if (font != null) it.font(font) else it }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt() - textRenderer.width(comp) / 2, y.toInt(), legacyTextColour(colour), shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true,
    pMouseX: Number? = null,
    pMouseY: Number? = null
): Boolean {
    val comp = if (font == null) text else text.setStyle(text.style.withFont(font))
    val textRenderer = Minecraft.getInstance().font
    var x = x
    val width = textRenderer.width(comp)
    if (centered) {
        x = x.toDouble() - width / 2
    }
    context.text(textRenderer, comp, x.toInt(), y.toInt(), legacyTextColour(colour), shadow)
    var isHovered = false
    if (pMouseY != null && pMouseX != null) {
        if (pMouseX.toInt() >= x.toInt() && pMouseX.toInt() <= x.toInt() + width &&
            pMouseY.toInt() >= y.toInt() && pMouseY.toInt() <= y.toInt() + textRenderer.lineHeight
        ) {
            isHovered = true
        }
    }
    return isHovered
}

@JvmOverloads
fun drawTextJustifiedRight(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = text.let { if (font != null) it.font(font) else it }
    val font = Minecraft.getInstance().font
    context.text(font, comp, x.toInt() - font.width(comp), y.toInt(), legacyTextColour(colour), shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    text: FormattedCharSequence,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true
) {
    val textRenderer = Minecraft.getInstance().font
    var tweakedX = x
    if (centered) {
        val width = textRenderer.width(text)
        tweakedX = tweakedX.toDouble() - width / 2
    }
    context.text(textRenderer, text, tweakedX.toInt(), y.toInt(), legacyTextColour(colour), shadow)
}

@JvmOverloads
fun drawString(
    context: GuiGraphicsExtractor,
    text: String,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true,
    font: Identifier? = null
) {
    val comp = Component.literal(text).also {
        font?.run {
            it.toFlatList(it.style.withFont(this))
        }
    }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt(), y.toInt(), legacyTextColour(colour), shadow)
}

@JvmOverloads
fun drawPosablePortrait(
    identifier: Identifier,
    matrixStack: Matrix3x2fStack,
    scale: Float = 13F,
    contextScale: Float = 1F,
    reversed: Boolean = false,
    state: PosableState,
    partialTicks: Float,
    limbSwing: Float = 0F,
    limbSwingAmount: Float = 0F,
    ageInTicks: Float = 0F,
    headYaw: Float = 0F,
    headPitch: Float = 0F,
    doQuirks: Boolean = true,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F
) {
    // 3D content inside a GUI has to be drawn as a picture-in-picture element; the body below is the original
    // immediate-mode rendering, now run against the PIP's pose stack and buffers.
    CobblemonModelPipState.submit(matrixStack.graphics, radius = PORTRAIT_DIAMETER * 4F) { poseStack, immediate ->
        poseStack.pushPose()
        poseStack.translate(0.0, PORTRAIT_DIAMETER.toDouble() + 2.0, 0.0)
        poseStack.scale(scale, scale, -scale)
        poseStack.translate(0.0, -PORTRAIT_DIAMETER / 18.0, 0.0)

        val sprite = VaryingModelRepository.getSprite(identifier, state, SpriteType.PORTRAIT)

        if (sprite == null) {
            val model = VaryingModelRepository.getPoser(identifier, state)
            state.currentModel = model
            val texture = VaryingModelRepository.getTexture(identifier, state)

            val context = RenderContext()
            model.context = context
            VaryingModelRepository.getTextureNoSubstitute(identifier, state).let { context.put(RenderContext.TEXTURE, it) }
            context.put(RenderContext.SCALE, contextScale)
            context.put(RenderContext.SPECIES, identifier)
            context.put(RenderContext.ASPECTS, state.currentAspects)
            context.put(RenderContext.POSABLE_STATE, state)
            context.put(RenderContext.DO_QUIRKS, doQuirks)

            val renderType = RenderTypes.entityCutoutCull(texture)

            val quaternion1 = Axis.YP.rotationDegrees(-32F * if (reversed) -1F else 1F)
            val quaternion2 = Axis.XP.rotationDegrees(5F)

            val originalPose = state.currentPose
            state.setPoseToFirstSuitable(PoseType.PORTRAIT)
            state.updatePartialTicks(partialTicks)
            model.applyAnimations(null, state, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
            originalPose?.let { state.setPose(it) }

            poseStack.translate(
                model.portraitTranslation.x * if (reversed) -1F else 1F,
                model.portraitTranslation.y + 1.5 * model.portraitScale,
                model.portraitTranslation.z - 4
            )
            poseStack.scale(model.portraitScale, model.portraitScale, 1 / model.portraitScale)
            poseStack.mulPose(quaternion1)
            poseStack.mulPose(quaternion2)

            val light1 = Vector3f(0.2F, 1.0F, -1.0F)
            val light2 = Vector3f(0.1F, 0.0F, 8.0F)
            GuiModelLighting.setup(light1, light2)
            quaternion1.conjugate()

            val buffer = immediate.getBuffer(renderType)
            val packedLight = LightCoordsUtil.pack(11, 7)

            val colour = toHex(r, g, b, a)
            model.withLayerContext(immediate, state, VaryingModelRepository.getLayers(identifier, state)) {
                model.render(context, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, colour)
                immediate.endBatch()
            }

            model.setDefault()
        } else {
            renderSprite(poseStack, immediate, sprite)
        }

        poseStack.popPose()
    }
}

fun drawProfile(
    resourceIdentifier: Identifier,
    matrixStack: Matrix3x2fStack,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F
) {
    CobblemonModelPipState.submit(matrixStack.graphics, radius = scale * 4F) { poseStack, bufferSource ->
        poseStack.scale(scale, scale, -scale)

        val sprite = VaryingModelRepository.getSprite(resourceIdentifier, state, SpriteType.PROFILE)

        if (sprite == null) {

            val model = VaryingModelRepository.getPoser(resourceIdentifier, state)
            val texture = VaryingModelRepository.getTexture(resourceIdentifier, state)

            val context = RenderContext()
            model.context = context
            VaryingModelRepository.getTextureNoSubstitute(resourceIdentifier, state).let { context.put(RenderContext.TEXTURE, it) }
            context.put(RenderContext.SCALE, 1F)
            context.put(RenderContext.SPECIES, resourceIdentifier)
            context.put(RenderContext.ASPECTS, state.currentAspects)
            context.put(RenderContext.POSABLE_STATE, state)
            state.currentModel = model

            val renderType = RenderTypes.entityCutoutCull(texture)//model.getLayer(texture)

            state.setPoseToFirstSuitable(PoseType.PORTRAIT)
            state.updatePartialTicks(partialTicks)
            model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)
            poseStack.translate(
                model.profileTranslation.x,
                model.profileTranslation.y + 1.5 * model.profileScale,
                model.profileTranslation.z - 4.0
            )
            poseStack.scale(model.profileScale, model.profileScale, 1 / model.profileScale)
            val quaternion1 = Axis.YP.rotationDegrees(-32F * if (false) -1F else 1F)
            val quaternion2 = Axis.XP.rotationDegrees(5F)
            poseStack.mulPose(quaternion1)
            poseStack.mulPose(quaternion2)

            val buffer = bufferSource.getBuffer(renderType)
            val light1 = Vector3f(-1F, 1F, 1.0F)
            val light2 = Vector3f(1.3F, -1F, 1.0F)
            GuiModelLighting.setup(light1, light2)
            val packedLight = LightCoordsUtil.pack(11, 7)

            model.withLayerContext(bufferSource, state, VaryingModelRepository.getLayers(resourceIdentifier, state)) {
                model.render(context, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
                bufferSource.endBatch()
            }
            model.setDefault()
        } else {
            renderSprite(poseStack, bufferSource, sprite)
        }
    }
}

/** Draws a 2x2 unit, unlit sprite at the origin of [poseStack] (inside a PIP render). */
fun renderSprite(poseStack: PoseStack, bufferSource: MultiBufferSource, sprite: Identifier) {
    val matrix: PoseStack.Pose = poseStack.last()
    matrix.pose().translate(-1f, 0f, 0f)

    val buffer = bufferSource.getBuffer(RenderTypes.text(sprite))
    val light = LightCoordsUtil.FULL_BRIGHT

    buffer.addVertex(matrix, 2f, 0f, 0.0f).setColor(-1).setUv(1f, 0f).setLight(light)
    buffer.addVertex(matrix, 0f, 0f, 0.0f).setColor(-1).setUv(0f, 0f).setLight(light)
    buffer.addVertex(matrix, 0f, 2f, 0.0f).setColor(-1).setUv(0f, 1f).setLight(light)
    buffer.addVertex(matrix, 2f, 2f, 0.0f).setColor(-1).setUv(1f, 1f).setLight(light)
}
