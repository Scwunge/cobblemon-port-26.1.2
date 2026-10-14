/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.snowstorm

import com.cobblemon.mod.common.client.render.legacy.RecordingVertexConsumer
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry
import net.minecraft.client.Camera
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.ParticleGroup
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState
import net.minecraft.client.renderer.texture.TextureAtlas

/**
 * The render types of Snowstorm particles, one per Bedrock particle material.
 *
 * In 1.21.1 each material was a ParticleRenderType that set up the particle shader with its own blending and culling.
 * Particles are now drawn from extracted render states, so each material gets a pipeline with that same state and a
 * particle group that collects its particles' quads and submits them.
 */
object ParticleMaterials {
    val ALPHA = Material("alpha", BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA), cull = true)
    val ADD = Material("add", BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE), cull = false)
    val BLEND = Material("blend", BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA), cull = false)
    val OPAQUE = Material("opaque", BlendFunction(SourceFactor.SRC_COLOR, DestFactor.ZERO), cull = true)

    // The order ParticleManagerMixin appended them to vanilla's render order in 1.21.1
    val ALL = listOf(ADD, ALPHA, BLEND, OPAQUE)

    /** Registers the particle groups of the materials, rendered after vanilla's particles in the order of [ALL]. */
    fun register() {
        var previous: ParticleRenderType = ParticleRenderType.SINGLE_QUADS
        for (material in ALL) {
            ParticleGroupRegistry.register(material.renderType) { engine -> Group(engine, material) }
            ParticleGroupRegistry.registerOrdering(previous, material.renderType)
            previous = material.renderType
        }
    }

    class Material(name: String, blendFunction: BlendFunction, cull: Boolean) {
        val renderType = ParticleRenderType(cobblemonResource("snowstorm_$name").toString())

        val pipeline: RenderPipeline = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(cobblemonResource("pipeline/snowstorm_$name"))
            .withColorTargetState(ColorTargetState(blendFunction))
            .withCull(cull)
            .build()

        val geometryType: RenderType = RenderType.create(
            "cobblemon_snowstorm_$name",
            RenderSetup.builder(pipeline)
                .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
                .useLightmap()
                .createRenderSetup()
        )

        override fun toString() = renderType.name
    }

    /** Holds the particles of one material and extracts their quads each frame. */
    class Group(engine: ParticleEngine, val material: Material) : ParticleGroup<com.cobblemon.mod.common.client.render.SnowstormParticle>(engine) {
        private val state = RenderState(material)

        override fun extractRenderState(frustum: Frustum, camera: Camera, partialTickTime: Float): ParticleGroupRenderState {
            state.vertices.clear()
            for (particle in particles) {
                particle.render(state.vertices, frustum, camera, partialTickTime)
            }
            return state
        }
    }

    class RenderState(val material: Material) : ParticleGroupRenderState {
        val vertices = RecordingVertexConsumer()

        override fun submit(submitNodeCollector: SubmitNodeCollector, camera: CameraRenderState) {
            if (!vertices.isEmpty) {
                // Vertices are camera relative already.
                submitNodeCollector.submitCustomGeometry(PoseStack(), material.geometryType) { _, consumer -> vertices.replay(consumer) }
            }
        }

        // The quads are replaced when the group is next extracted.
    }
}
