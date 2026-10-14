/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import com.cobblemon.mod.common.api.gui.renderSprite
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.util.math.toEulerXYZDegrees
import com.cobblemon.mod.common.util.toHex
import com.cobblemon.mod.common.client.render.gui.CobblemonModelPipState
import com.cobblemon.mod.common.client.render.gui.GuiModelLighting
import com.cobblemon.mod.common.client.render.gui.graphics
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.LightCoordsUtil
import org.joml.Matrix3x2fStack
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.atan

enum class ProfileTransformType {
    PROFILE,
    NONE,
    POKEDEX,
    SUMMARY,
}

fun drawProfilePokemon(
    renderablePokemon: RenderablePokemon,
    matrixStack: Matrix3x2fStack,
    rotation: Quaternionf,
    poseType: PoseType = PoseType.PROFILE,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F,
    profileTransformType: ProfileTransformType = ProfileTransformType.PROFILE,
    applyBaseScale: Boolean = false,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F,
    headYaw: Float = 0f,
    headPitch: Float = 0f,
    blockLight: Int = 13
) = drawProfilePokemon(
    species = renderablePokemon.species.resourceIdentifier,
    matrixStack = matrixStack,
    rotation = rotation,
    poseType = poseType,
    state = state.also { it.currentAspects = renderablePokemon.aspects },
    partialTicks = partialTicks,
    scale = scale,
    profileTransformType = profileTransformType,
    applyBaseScale = applyBaseScale,
    r = r,
    g = g,
    b = b,
    a = a,
    headYaw = headYaw,
    headPitch = headPitch,
    blockLight = blockLight
)

fun drawProfilePokemon(
    species: Identifier,
    matrixStack: Matrix3x2fStack,
    rotation: Quaternionf,
    poseType: PoseType = PoseType.PROFILE,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F,
    profileTransformType: ProfileTransformType = ProfileTransformType.PROFILE,
    applyBaseScale: Boolean = false,
    doQuirks: Boolean = false,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F,
    headYaw: Float = 0f,
    headPitch: Float = 0f,
    blockLight: Int = 13
) {
    // 3D content inside a GUI has to be drawn as a picture-in-picture element; the body below is the original
    // immediate-mode rendering, now run against the PIP's pose stack and buffers.
    // Rendering happens later in the frame, so don't share the caller's (mutable) rotation.
    val rotation = Quaternionf(rotation)
    CobblemonModelPipState.submit(matrixStack.graphics, radius = scale * 4F) { poseStack, bufferSource ->
        poseStack.scale(scale, scale, -scale)

        val sprite = VaryingModelRepository.getSprite(species, state, SpriteType.PROFILE)

        if (sprite == null) {

            val model = VaryingModelRepository.getPoser(species, state)
            val texture = VaryingModelRepository.getTexture(species, state)

            val context = RenderContext()
            model.context = context
            VaryingModelRepository.getTextureNoSubstitute(species, state).let { context.put(RenderContext.TEXTURE, it) }
            val baseScale = PokemonSpecies.getByIdentifier(species)!!.getForm(state.currentAspects).baseScale
            context.put(RenderContext.SCALE, baseScale)
            context.put(RenderContext.SPECIES, species)
            context.put(RenderContext.ASPECTS, state.currentAspects)
            context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
            context.put(RenderContext.POSABLE_STATE, state)
            context.put(RenderContext.DO_QUIRKS, doQuirks)

            state.currentModel = model

            val renderType = RenderTypes.entityCutoutCull(texture)

            state.setPoseToFirstSuitable(poseType)
            state.updatePartialTicks(partialTicks)

            if (profileTransformType == ProfileTransformType.NONE) {
                poseStack.translate(0F, 0F, -4.0F)
                if (applyBaseScale) {
                    poseStack.scale(baseScale, baseScale, 1 / baseScale)
                }
            } else {
                var profileScale: Float
                var profileTranslation: Vec3

                when (profileTransformType) {
                    ProfileTransformType.POKEDEX -> {
                        profileScale = model.profilePokedexScale ?: model.profileScale
                        profileTranslation = model.profilePokedexTranslation ?: model.profileTranslation
                    }
                    ProfileTransformType.SUMMARY -> {
                        profileScale = model.profileSummaryScale ?: model.profileScale
                        profileTranslation = model.profileSummaryTranslation ?: model.profileTranslation
                    }
                    else -> {
                        profileScale = model.profileScale
                        profileTranslation = model.profileTranslation
                    }
                }

                poseStack.translate(
                    profileTranslation.x,
                    profileTranslation.y + 1.5 * profileScale,
                    profileTranslation.z - 4.0
                )
                poseStack.scale(profileScale, profileScale, 1 / profileScale)
            }

            poseStack.mulPose(rotation)

            model.applyAnimations(null, state, 0F, 0F, 0F, headYaw, headPitch)

            // The camera orientation override and shadow toggle only affected name tags/shadows, which profile
            // renders never draw; lighting is set up per PIP render below.
            rotation.conjugate()

            val buffer = bufferSource.getBuffer(renderType)
            val light1 = Vector3f(-1F, 1F, 1.0F)
            val light2 = Vector3f(1.3F, -1F, 1.0F)
            GuiModelLighting.setup(light1, light2)
            val packedLight = LightCoordsUtil.pack(blockLight, 0)

            val colour = toHex(r, g, b, a)
            model.withLayerContext(bufferSource, state, VaryingModelRepository.getLayers(species, state)) {
                model.render(context, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, colour)
                bufferSource.endBatch()
            }
            model.setDefault()
        } else {
            renderSprite(poseStack, bufferSource, sprite)
        }
    }
}

const val HEAD_YAW_FACTOR = 40f
const val HEAD_PITCH_FACTOR = 20f
const val SMOOTHING_FACTOR = 0.05f

fun calculateHeadYawAndPitch(
    xCoordinate: Float,
    yCoordinate: Float,
    rotation: Quaternionf,
    mouseX: Int,
    mouseY: Int,
    currentYaw: Float,
    currentPitch: Float,
    resetToCenter: Boolean,
): Pair<Float, Float> {
    val horizontalAngle = atan(((xCoordinate - mouseX) / 40.0f).toDouble()).toFloat()
    val verticalAngle = atan(((yCoordinate - mouseY) / 40.0f).toDouble()).toFloat()

    val eulerXYZRotationDegrees = rotation.toEulerXYZDegrees()
    val targetYaw = if (!resetToCenter) horizontalAngle * HEAD_YAW_FACTOR - eulerXYZRotationDegrees.y else 0f
    val targetPitch = if (!resetToCenter) -verticalAngle * HEAD_PITCH_FACTOR - eulerXYZRotationDegrees.x else 0f

    val newYaw = Mth.rotLerp(SMOOTHING_FACTOR, currentYaw, targetYaw)
    val newPitch = Mth.rotLerp(SMOOTHING_FACTOR, currentPitch, targetPitch)

    return Pair(newYaw, newPitch)
}