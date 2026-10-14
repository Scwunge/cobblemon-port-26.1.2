/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.gui

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Matrix3x2fc
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.cobblemon.mod.common.client.render.gui.pushPose
import com.cobblemon.mod.common.client.render.gui.popPose
import com.cobblemon.mod.common.client.render.gui.translate
import com.cobblemon.mod.common.client.render.gui.scale
import com.cobblemon.mod.common.client.render.gui.mulPose

/*
 * Since Minecraft 1.21.6 GUI rendering is deferred: screens no longer draw immediately but submit render states to a
 * GuiGraphicsExtractor (formerly GuiGraphics), and its pose stack is 2D (Matrix3x2fStack). Cobblemon's GUI code was
 * written against immediate-mode drawing with a 3D PoseStack, so this file holds the small amount of infrastructure
 * that lets the existing helpers (blitk, drawProfilePokemon, ...) keep their shape:
 *
 *  - [GuiGraphicsTracker] remembers the extractor of the current frame so helpers that only receive the pose stack
 *    can still submit elements.
 *  - [FloatBlitRenderState] is a textured quad with float coordinates (vanilla's blit only accepts ints).
 *  - [CobblemonModelPipState]/[CobblemonModelPipRenderer] render 3D content (Pokémon models, NPCs, items...) into a
 *    picture-in-picture texture, which is how 3D content must be shown inside a GUI now.
 */

/** Tracks the [GuiGraphicsExtractor] of the frame currently being extracted. Set from GuiGraphicsExtractorMixin. */
object GuiGraphicsTracker {
    @JvmStatic
    var current: GuiGraphicsExtractor? = null

    @JvmStatic
    fun get(pose: Matrix3x2fStack): GuiGraphicsExtractor {
        val graphics = current ?: throw IllegalStateException("No GuiGraphicsExtractor is active; GUI helpers must be called while extracting a screen or HUD.")
        if (graphics.pose() !== pose) {
            throw IllegalStateException("The supplied pose stack does not belong to the active GuiGraphicsExtractor.")
        }
        return graphics
    }
}

/** The extractor that owns this pose stack. */
val Matrix3x2fStack.graphics: GuiGraphicsExtractor
    get() = GuiGraphicsTracker.get(this)

/** Converts a 2D GUI pose into the equivalent 3D affine transform (z untouched). */
fun Matrix3x2fc.to4f(): Matrix4f = Matrix4f(
    m00(), m01(), 0F, 0F,
    m10(), m11(), 0F, 0F,
    0F, 0F, 1F, 0F,
    m20(), m21(), 0F, 1F
)

/** A textured quad with float coordinates, submitted with the given 2D pose. */
class FloatBlitRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val x0: Float,
    private val y0: Float,
    private val x1: Float,
    private val y1: Float,
    private val u0: Float,
    private val u1: Float,
    private val v0: Float,
    private val v1: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds: ScreenRectangle? = run {
        val ix0 = floor(min(x0, x1)).toInt()
        val iy0 = floor(min(y0, y1)).toInt()
        val ix1 = ceil(max(x0, x1)).toInt()
        val iy1 = ceil(max(y0, y1)).toInt()
        val rect = ScreenRectangle(ix0, iy0, ix1 - ix0, iy1 - iy0).transformMaxBounds(pose)
        if (scissorArea != null) scissorArea.intersection(rect) else rect
    }

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(color)
    }

    override fun pipeline() = pipeline
    override fun textureSetup() = textureSetup
    override fun scissorArea() = scissorArea
    override fun bounds() = bounds
}

/** A flat coloured triangle (drawn as a quad whose last two corners coincide). */
class ColoredTriangleRenderState(
    private val pose: Matrix3x2fc,
    private val x0: Float, private val y0: Float,
    private val x1: Float, private val y1: Float,
    private val x2: Float, private val y2: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds: ScreenRectangle? = run {
        val ix0 = floor(minOf(x0, x1, x2)).toInt()
        val iy0 = floor(minOf(y0, y1, y2)).toInt()
        val ix1 = ceil(maxOf(x0, x1, x2)).toInt()
        val iy1 = ceil(maxOf(y0, y1, y2)).toInt()
        val rect = ScreenRectangle(ix0, iy0, ix1 - ix0, iy1 - iy0).transformMaxBounds(pose)
        if (scissorArea != null) scissorArea.intersection(rect) else rect
    }

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x2, y2).setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x2, y2).setColor(color)
    }

    override fun pipeline(): RenderPipeline = RenderPipelines.GUI
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
    override fun scissorArea() = scissorArea
    override fun bounds() = bounds
}

/** Submits a flat coloured triangle in screen coordinates (the current pose is not applied). */
fun GuiGraphicsExtractor.fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
    guiRenderState.addGuiElement(ColoredTriangleRenderState(Matrix3x2f(), x0, y0, x1, y1, x2, y2, color, scissorStack.peek()))
}

/** Submits a float-precision textured quad. */
fun GuiGraphicsExtractor.blitFloat(
    texture: Identifier,
    x0: Float, y0: Float, x1: Float, y1: Float,
    u0: Float, u1: Float, v0: Float, v1: Float,
    color: Int = -1,
    pipeline: RenderPipeline = RenderPipelines.GUI_TEXTURED
) {
    val abstractTexture = Minecraft.getInstance().textureManager.getTexture(texture)
    guiRenderState.addGuiElement(
        FloatBlitRenderState(
            pipeline,
            TextureSetup.singleTexture(abstractTexture.textureView, abstractTexture.sampler),
            Matrix3x2f(pose()),
            x0, y0, x1, y1, u0, u1, v0, v1,
            color,
            scissorStack.peek()
        )
    )
}

/**
 * Picture-in-picture state for arbitrary 3D GUI content. [draw] receives a pose stack that already maps GUI
 * coordinates (as transformed by [guiPose]) to the PIP texture, so callers can keep thinking in GUI space: the origin
 * of the stack is where the GUI pose's origin was when the state was created.
 */
class CobblemonModelPipState(
    val guiPose: Matrix3x2f,
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    private val scissorArea: ScreenRectangle?,
    val draw: (PoseStack, MultiBufferSource.BufferSource) -> Unit
) : PictureInPictureRenderState {
    private val bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)

    override fun x0() = x0
    override fun y0() = y0
    override fun x1() = x1
    override fun y1() = y1
    override fun scale() = 1F
    override fun scissorArea() = scissorArea
    override fun bounds() = bounds

    companion object {
        /**
         * Submits [draw] as a picture-in-picture element. The rendered area is the active scissor rectangle when
         * there is one, otherwise a square of [radius] GUI-pose units around the pose origin (transformed to screen
         * space).
         */
        @JvmStatic
        fun submit(graphics: GuiGraphicsExtractor, radius: Float, draw: (PoseStack, MultiBufferSource.BufferSource) -> Unit) {
            val pose = Matrix3x2f(graphics.pose())
            val origin = pose.transformPosition(Vector2f(0F, 0F))
            val poseScale = sqrt(abs(pose.determinant()))
            val r = radius * poseScale
            var area = ScreenRectangle(
                floor(origin.x - r).toInt(), floor(origin.y - r).toInt(),
                ceil(2 * r).toInt() + 1, ceil(2 * r).toInt() + 1
            )
            val scissor = graphics.scissorStack.peek()
            if (scissor != null) {
                area = scissor.intersection(area) ?: return
            }
            val screen = ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight())
            area = screen.intersection(area) ?: return
            if (area.width() <= 0 || area.height() <= 0) {
                return
            }
            graphics.guiRenderState.addPicturesInPictureState(
                CobblemonModelPipState(pose, area.left(), area.top(), area.right(), area.bottom(), scissor, draw)
            )
        }
    }
}

class CobblemonModelPipRenderer(bufferSource: MultiBufferSource.BufferSource) : PictureInPictureRenderer<CobblemonModelPipState>(bufferSource) {
    private var guiScale = 1

    override fun prepare(renderState: CobblemonModelPipState, guiRenderState: net.minecraft.client.renderer.state.gui.GuiRenderState, guiScale: Int) {
        this.guiScale = guiScale
        super.prepare(renderState, guiRenderState, guiScale)
    }

    override fun getRenderStateClass() = CobblemonModelPipState::class.java

    override fun renderToTexture(renderState: CobblemonModelPipState, poseStack: PoseStack) {
        // Replace the default PIP transform with one mapping GUI space straight onto the texture.
        poseStack.setIdentity()
        poseStack.scale(guiScale.toFloat(), guiScale.toFloat(), 1F)
        poseStack.translate(-renderState.x0().toFloat(), -renderState.y0().toFloat(), 0F)
        poseStack.mulPose(renderState.guiPose.to4f())
        Minecraft.getInstance().gameRenderer.lighting.setupFor(Lighting.Entry.ENTITY_IN_UI)
        renderState.draw(poseStack, bufferSource)
    }

    override fun getTranslateY(height: Int, guiScale: Int) = 0F

    override fun getTextureLabel() = "cobblemon_model"
}

/**
 * Cobblemon lights its GUI models with its own light directions. Lighting directions are a uniform buffer now, so this
 * keeps a small buffer of its own and binds it in place of the vanilla entries.
 */
object GuiModelLighting {
    private val size = Lighting.UBO_SIZE.toLong()
    private var buffer: GpuBuffer? = null

    @JvmStatic
    fun setup(light0: Vector3f, light1: Vector3f) {
        val device = RenderSystem.getDevice()
        val buf = buffer ?: device.createBuffer({ "Cobblemon GUI lighting UBO" }, GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, size).also { buffer = it }
        MemoryStack.stackPush().use { stack ->
            val bytes = Std140Builder.onStack(stack, Lighting.UBO_SIZE)
                .putVec3(Vector3f(light0).normalize())
                .putVec3(Vector3f(light1).normalize())
                .get()
            device.createCommandEncoder().writeToBuffer(buf.slice(0, size), bytes)
        }
        RenderSystem.setShaderLights(buf.slice(0, size))
    }
}
