/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.platform.events

import net.minecraft.client.renderer.SubmitNodeCollector
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.LevelRenderer

/**
 * Event fired during the various [Stage]s of the [LevelRenderer].
 *
 * @author Segfault Guy
 * @since September 11th, 2024
 */
data class RenderEvent(
    val stage: Stage,
    val levelRenderer: LevelRenderer,
    /** Camera relative: the origin is at the camera's position. */
    val poseStack: PoseStack,
    /** World rendering is deferred since 1.21.9: geometry is submitted here and drawn later in the frame. */
    val submitNodeCollector: SubmitNodeCollector,
    val tickCounter: DeltaTracker,
    val camera: Camera
) {

    /** Represents when geometry is collected for rendering by [LevelRenderer.renderLevel]. */
    enum class Stage {
        TRANSLUCENT
    }
}