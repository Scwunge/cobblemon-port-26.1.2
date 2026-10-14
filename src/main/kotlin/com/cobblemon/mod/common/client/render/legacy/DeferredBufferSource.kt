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
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType

/**
 * A [MultiBufferSource] that lets render code written for immediate-mode rendering run inside the deferred rendering
 * introduced in 1.21.9.
 *
 * Vertices written to it are recorded as they arrive - already transformed by whatever pose they were written with -
 * and replayed into the real buffers when the frame's submitted geometry is drawn. Since everything about a vertex is
 * decided when it is recorded, render code may pose shared models, read entity state, etc. exactly as it used to.
 * Render types are batched together in the same way MultiBufferSource.BufferSource batched them.
 */
class DeferredBufferSource(val submitNodeCollector: SubmitNodeCollector) : MultiBufferSource {
    private val buffers = LinkedHashMap<RenderType, RecordingVertexConsumer>()

    override fun getBuffer(renderType: RenderType): VertexConsumer = buffers.getOrPut(renderType) {
        val recorder = RecordingVertexConsumer()
        submitNodeCollector.submitCustomGeometry(IDENTITY, renderType) { _, consumer -> recorder.replay(consumer) }
        recorder
    }

    companion object {
        // Recorded vertices are already transformed; the geometry is submitted untransformed.
        private val IDENTITY = PoseStack()
    }
}

/** Records vertices for [DeferredBufferSource]. */
class RecordingVertexConsumer : VertexConsumer {
    private var floats = FloatArray(FLOATS * 64)
    private var ints = IntArray(INTS * 64)
    private var count = 0

    private val f get() = (count - 1) * FLOATS
    private val i get() = (count - 1) * INTS

    override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
        if (count * FLOATS >= floats.size) {
            floats = floats.copyOf(floats.size * 2)
            ints = ints.copyOf(ints.size * 2)
        }
        count++
        val f = f
        floats[f] = x
        floats[f + 1] = y
        floats[f + 2] = z
        floats[f + 3] = 0F
        floats[f + 4] = 0F
        floats[f + 5] = 0F
        floats[f + 6] = 1F
        floats[f + 7] = 0F
        floats[f + 8] = 1F
        val i = i
        ints[i] = -1
        ints[i + 1] = 0
        ints[i + 2] = 0
        ints[i + 3] = 0
        return this
    }

    override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
        ints[i] = (a and 255 shl 24) or (r and 255 shl 16) or (g and 255 shl 8) or (b and 255)
        return this
    }

    override fun setColor(color: Int): VertexConsumer {
        ints[i] = color
        return this
    }

    override fun setUv(u: Float, v: Float): VertexConsumer {
        floats[f + 3] = u
        floats[f + 4] = v
        return this
    }

    override fun setUv1(u: Int, v: Int): VertexConsumer {
        ints[i + 1] = (u and 0xFFFF) or (v shl 16)
        return this
    }

    override fun setUv2(u: Int, v: Int): VertexConsumer {
        ints[i + 2] = (u and 0xFFFF) or (v shl 16)
        return this
    }

    override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
        floats[f + 5] = x
        floats[f + 6] = y
        floats[f + 7] = z
        return this
    }

    override fun setLineWidth(width: Float): VertexConsumer {
        floats[f + 8] = width
        ints[i + 3] = 1
        return this
    }

    val isEmpty: Boolean
        get() = count == 0

    fun clear() {
        count = 0
    }

    fun replay(consumer: VertexConsumer) {
        for (index in 0 until count) {
            val f = index * FLOATS
            val i = index * INTS
            consumer.addVertex(floats[f], floats[f + 1], floats[f + 2])
                .setColor(ints[i])
                .setUv(floats[f + 3], floats[f + 4])
                .setUv1(ints[i + 1] and 0xFFFF, ints[i + 1] ushr 16)
                .setUv2(ints[i + 2] and 0xFFFF, ints[i + 2] ushr 16)
                .setNormal(floats[f + 5], floats[f + 6], floats[f + 7])
            if (ints[i + 3] != 0) {
                consumer.setLineWidth(floats[f + 8])
            }
        }
    }

    private companion object {
        const val FLOATS = 9 // x, y, z, u, v, nx, ny, nz, line width
        const val INTS = 4 // colour, uv1 (overlay), uv2 (light), has line width
    }
}
