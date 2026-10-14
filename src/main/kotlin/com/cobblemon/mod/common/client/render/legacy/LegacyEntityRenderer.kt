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
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.scores.Team
import org.joml.Quaternionf

/*
 * Cobblemon's entity renderers were written against the 1.21.1 renderer API: render(entity, yaw, partialTicks,
 * poseStack, buffers, light), with models posed from the entity during rendering. Since 1.21.9 renderers extract a
 * render state and submit draw commands that are executed later in the frame.
 *
 * These classes reproduce the 1.21.1 EntityRenderer / LivingEntityRenderer / MobRenderer render pipeline on top of the
 * new one: the render state carries the entity, and submit() runs the original render code against a
 * DeferredBufferSource. The rendering logic below is the 1.21.1 vanilla logic, unchanged.
 */

/** Render state carrying what the 1.21.1 render method received. */
open class LegacyEntityRenderState : EntityRenderState() {
    var entity: Entity? = null
    var partialTicks = 0F
    var yaw = 0F
}

abstract class LegacyEntityRenderer<T : Entity>(context: EntityRendererProvider.Context) : EntityRenderer<T, LegacyEntityRenderState>(context) {
    private var camera: CameraRenderState? = null

    override fun createRenderState() = LegacyEntityRenderState()

    /** Called before the render state is extracted, e.g. to update the shadow radius for this entity. */
    open fun beforeExtract(entity: T, partialTicks: Float) {}

    override fun extractRenderState(entity: T, state: LegacyEntityRenderState, partialTicks: Float) {
        beforeExtract(entity, partialTicks)
        state.entity = entity
        super.extractRenderState(entity, state, partialTicks)
        state.partialTicks = partialTicks
        // EntityRenderDispatcher passed the interpolated yaw to render.
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.yRot)
        // Name tags are drawn by render (renderNameTag), as they were.
        state.nameTag = null
    }

    @Suppress("UNCHECKED_CAST")
    override fun submit(state: LegacyEntityRenderState, poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, camera: CameraRenderState) {
        val entity = state.entity as? T ?: return
        state.leashStates?.forEach { submitNodeCollector.submitLeash(poseStack, it) }
        this.camera = camera
        try {
            render(entity, state.yaw, state.partialTicks, poseStack, DeferredBufferSource(submitNodeCollector), state.lightCoords)
        } finally {
            this.camera = null
        }
    }

    abstract fun getTextureLocation(entity: T): Identifier

    /** EntityRenderer#render from 1.21.1 (the leash is submitted from the render state). */
    open fun render(entity: T, entityYaw: Float, partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        if (this.shouldShowName(entity)) {
            this.renderNameTag(entity, entity.displayName ?: entity.name, poseStack, buffer, packedLight, partialTicks)
        }
    }

    /** EntityRenderDispatcher#cameraOrientation from 1.21.1. */
    fun cameraOrientation(): Quaternionf = camera?.orientation ?: entityRenderDispatcher.camera?.rotation()?.let(::Quaternionf) ?: Quaternionf()

    protected open fun shouldShowName(entity: T): Boolean {
        return entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity
    }

    protected open fun renderNameTag(entity: T, component: Component, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, partialTicks: Float) {
        val d = this.entityRenderDispatcher.distanceToSqr(entity)
        if (d > 4096.0) {
            return
        }
        val vec3 = entity.attachments.getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTicks)) ?: return
        val notDiscrete = !entity.isDiscrete
        val j = if ("deadmau5" == component.string) -10 else 0
        poseStack.pushPose()
        poseStack.translate(vec3.x, vec3.y + 0.5, vec3.z)
        poseStack.mulPose(this.cameraOrientation())
        poseStack.scale(0.025F, -0.025F, 0.025F)
        val matrix4f = poseStack.last().pose()
        val g = Minecraft.getInstance().options.getBackgroundOpacity(0.25F)
        val k = (g * 255.0F).toInt() shl 24
        val font = this.font
        val h = (-font.width(component) / 2).toFloat()
        font.drawInBatch(component, h, j.toFloat(), 553648127, false, matrix4f, buffer, if (notDiscrete) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL, k, packedLight)
        if (notDiscrete) {
            font.drawInBatch(component, h, j.toFloat(), -1, false, matrix4f, buffer, Font.DisplayMode.NORMAL, 0, packedLight)
        }
        poseStack.popPose()
    }
}

/**
 * A model as LivingEntityRenderer used it in 1.21.1: posed from the entity (setupAnim) and drawn into a buffer
 * (renderToBuffer). Vanilla's EntityModel can no longer be used this way (models are posed from render states and
 * always draw their root part).
 */
abstract class LegacyEntityModel<T : Entity>(val renderType: (Identifier) -> RenderType = RenderTypes::entityCutoutCull) {
    var attackTime = 0F
    var riding = false
    var young = false

    fun renderType(texture: Identifier): RenderType = renderType.invoke(texture)

    open fun prepareMobModel(entity: T, limbSwing: Float, limbSwingAmount: Float, partialTick: Float) {}

    abstract fun setupAnim(entity: T, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float)

    abstract fun renderToBuffer(poseStack: PoseStack, buffer: VertexConsumer, packedLight: Int, packedOverlay: Int, color: Int)
}

/** RenderLayer as LivingEntityRenderer used it in 1.21.1. */
abstract class LegacyRenderLayer<T : LivingEntity, M : LegacyEntityModel<T>>(val renderer: LegacyLivingEntityRenderer<T, M>) {
    abstract fun render(poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, entity: T, limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float)
}

/** LivingEntityRenderer from 1.21.1. */
abstract class LegacyLivingEntityRenderer<T : LivingEntity, M : LegacyEntityModel<T>>(
    context: EntityRendererProvider.Context,
    var model: M,
    shadowRadius: Float
) : LegacyEntityRenderer<T>(context) {
    protected val layers = mutableListOf<LegacyRenderLayer<T, M>>()

    init {
        this.shadowRadius = shadowRadius
    }

    fun addLayer(layer: LegacyRenderLayer<T, M>): Boolean = layers.add(layer)

    override fun getShadowRadius(state: LegacyEntityRenderState): Float =
        super.getShadowRadius(state) * ((state.entity as? LivingEntity)?.scale ?: 1F)

    override fun render(entity: T, entityYaw: Float, partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        poseStack.pushPose()
        this.model.attackTime = this.getAttackAnim(entity, partialTicks)
        this.model.riding = entity.isPassenger
        this.model.young = entity.isBaby
        var h = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot)
        val j = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot)
        var k = j - h
        val vehicle = entity.vehicle
        if (entity.isPassenger && vehicle is LivingEntity) {
            h = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot)
            k = j - h
            var l = Mth.wrapDegrees(k)
            if (l < -85.0F) {
                l = -85.0F
            }
            if (l >= 85.0F) {
                l = 85.0F
            }
            h = j - l
            if (l * l > 2500.0F) {
                h += l * 0.2F
            }
            k = j - h
        }

        var m = Mth.lerp(partialTicks, entity.xRotO, entity.xRot)
        if (isEntityUpsideDown(entity)) {
            m *= -1.0F
            k *= -1.0F
        }

        k = Mth.wrapDegrees(k)
        if (entity.hasPose(Pose.SLEEPING)) {
            val direction = entity.bedOrientation
            if (direction != null) {
                val n = entity.getEyeHeight(Pose.STANDING) - 0.1F
                poseStack.translate(-direction.stepX * n, 0.0F, -direction.stepZ * n)
            }
        }

        val l = entity.scale
        poseStack.scale(l, l, l)
        val n = this.getBob(entity, partialTicks)
        this.setupRotations(entity, poseStack, n, h, partialTicks, l)
        poseStack.scale(-1.0F, -1.0F, 1.0F)
        this.scale(entity, poseStack, partialTicks)
        poseStack.translate(0.0F, -1.501F, 0.0F)
        var o = 0.0F
        var p = 0.0F
        if (!entity.isPassenger && entity.isAlive) {
            o = entity.walkAnimation.speed(partialTicks)
            p = entity.walkAnimation.position(partialTicks)
            if (entity.isBaby) {
                p *= 3.0F
            }
            if (o > 1.0F) {
                o = 1.0F
            }
        }

        this.model.prepareMobModel(entity, p, o, partialTicks)
        this.model.setupAnim(entity, p, o, n, k, m)
        val minecraft = Minecraft.getInstance()
        val bodyVisible = this.isBodyVisible(entity)
        val translucent = !bodyVisible && !entity.isInvisibleTo(minecraft.player!!)
        val glowing = minecraft.shouldEntityAppearGlowing(entity)
        val renderType = this.getRenderType(entity, bodyVisible, translucent, glowing)
        if (renderType != null) {
            val vertexConsumer = buffer.getBuffer(renderType)
            val q = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTicks))
            this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, q, if (translucent) 654311423 else -1)
        }

        if (!entity.isSpectator) {
            for (renderLayer in this.layers) {
                renderLayer.render(poseStack, buffer, packedLight, entity, p, o, partialTicks, n, k, m)
            }
        }

        poseStack.popPose()
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight)
    }

    protected open fun getRenderType(entity: T, bodyVisible: Boolean, translucent: Boolean, glowing: Boolean): RenderType? {
        val texture = this.getTextureLocation(entity)
        return if (translucent) {
            RenderTypes.entityTranslucentCullItemTarget(texture)
        } else if (bodyVisible) {
            this.model.renderType(texture)
        } else {
            if (glowing) RenderTypes.outline(texture) else null
        }
    }

    protected open fun isBodyVisible(entity: T): Boolean = !entity.isInvisible

    protected open fun isShaking(entity: T): Boolean = entity.isFullyFrozen

    protected open fun setupRotations(entity: T, poseStack: PoseStack, bob: Float, yBodyRot: Float, partialTick: Float, scale: Float) {
        var g = yBodyRot
        if (this.isShaking(entity)) {
            g += (Math.cos(entity.tickCount * 3.25) * Math.PI * 0.4F).toFloat()
        }

        if (!entity.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - g))
        }

        if (entity.deathTime > 0) {
            var j = (entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F
            j = Mth.sqrt(j)
            if (j > 1.0F) {
                j = 1.0F
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(j * this.getFlipDegrees(entity)))
        } else if (entity.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - entity.xRot))
            poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * -75.0F))
        } else if (entity.hasPose(Pose.SLEEPING)) {
            val direction = entity.bedOrientation
            val k = if (direction != null) sleepDirectionToRotation(direction) else g
            poseStack.mulPose(Axis.YP.rotationDegrees(k))
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(entity)))
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0F))
        } else if (isEntityUpsideDown(entity)) {
            poseStack.translate(0.0F, (entity.bbHeight + 0.1F) / scale, 0.0F)
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F))
        }
    }

    protected open fun getAttackAnim(entity: T, partialTick: Float): Float = entity.getAttackAnim(partialTick)

    protected open fun getBob(entity: T, partialTick: Float): Float = entity.tickCount + partialTick

    protected open fun getFlipDegrees(entity: T): Float = 90.0F

    protected open fun getWhiteOverlayProgress(entity: T, partialTick: Float): Float = 0.0F

    protected open fun scale(entity: T, poseStack: PoseStack, partialTick: Float) {}

    override fun shouldShowName(entity: T): Boolean {
        val d = this.entityRenderDispatcher.distanceToSqr(entity)
        val f = if (entity.isDiscrete) 32.0F else 64.0F
        if (d >= f * f) {
            return false
        }

        val minecraft = Minecraft.getInstance()
        val localPlayer = minecraft.player ?: return false
        val visible = !entity.isInvisibleTo(localPlayer)
        if (entity !== localPlayer) {
            val team = entity.team
            val team2 = localPlayer.team
            if (team != null) {
                return when (team.nameTagVisibility) {
                    Team.Visibility.ALWAYS -> visible
                    Team.Visibility.NEVER -> false
                    Team.Visibility.HIDE_FOR_OTHER_TEAMS -> if (team2 == null) visible else team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || visible)
                    Team.Visibility.HIDE_FOR_OWN_TEAM -> if (team2 == null) visible else !team.isAlliedTo(team2) && visible
                    else -> true
                }
            }
        }

        return Minecraft.renderNames() && entity !== minecraft.getCameraEntity() && visible && !entity.isVehicle
    }

    companion object {
        @JvmStatic
        fun getOverlayCoords(entity: LivingEntity, whiteOverlayProgress: Float): Int =
            OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0))

        @JvmStatic
        fun isEntityUpsideDown(entity: LivingEntity): Boolean {
            if (entity is Player || entity.hasCustomName()) {
                val name = ChatFormatting.stripFormatting(entity.name.string)
                if ("Dinnerbone" == name || "Grumm" == name) {
                    return entity !is Player || entity.isModelPartShown(PlayerModelPart.CAPE)
                }
            }
            return false
        }

        private fun sleepDirectionToRotation(direction: Direction): Float = when (direction) {
            Direction.SOUTH -> 90.0F
            Direction.WEST -> 0.0F
            Direction.NORTH -> 270.0F
            Direction.EAST -> 180.0F
            else -> 0.0F
        }
    }
}

/** MobRenderer from 1.21.1 (its leash rendering is submitted from the render state). */
abstract class LegacyMobRenderer<T : Mob, M : LegacyEntityModel<T>>(
    context: EntityRendererProvider.Context,
    model: M,
    shadowRadius: Float
) : LegacyLivingEntityRenderer<T, M>(context, model, shadowRadius) {
    override fun getShadowRadius(state: LegacyEntityRenderState): Float =
        super.getShadowRadius(state) * ((state.entity as? Mob)?.ageScale ?: 1F)

    override fun shouldShowName(entity: T): Boolean {
        return super.shouldShowName(entity) && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity)
    }
}
