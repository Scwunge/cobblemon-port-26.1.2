/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.gui

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Quaternionfc
import org.joml.Vector3f

/*
 * GUI pose stacks became 2D (Matrix3x2fStack) in 1.21.6. These mirror the PoseStack calls Cobblemon's GUI code was
 * written with so that code can stay as it was. Z components are dropped: in the deferred GUI, draw order (not depth)
 * decides what is on top, and later elements overlapping earlier ones are always drawn above them.
 */

fun Matrix3x2fStack.pushPose() {
    pushMatrix()
}

fun Matrix3x2fStack.popPose() {
    popMatrix()
}

fun Matrix3x2fStack.translate(x: Number, y: Number, @Suppress("UNUSED_PARAMETER") z: Number) {
    translate(x.toFloat(), y.toFloat())
}

fun Matrix3x2fStack.translate(x: Double, y: Double) {
    translate(x.toFloat(), y.toFloat())
}

fun Matrix3x2fStack.scale(x: Float, y: Float, @Suppress("UNUSED_PARAMETER") z: Float) {
    scale(x, y)
}

/** Applies the on-screen (orthographic) effect of a 3D rotation. For Z-axis rotations this is an exact 2D rotation. */
fun Matrix3x2fStack.mulPose(rotation: Quaternionfc) {
    val xAxis = rotation.transform(Vector3f(1F, 0F, 0F))
    val yAxis = rotation.transform(Vector3f(0F, 1F, 0F))
    mul(Matrix3x2f(xAxis.x, xAxis.y, yAxis.x, yAxis.y, 0F, 0F))
}
