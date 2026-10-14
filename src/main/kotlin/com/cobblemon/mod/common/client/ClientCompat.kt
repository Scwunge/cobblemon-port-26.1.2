/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client

import com.cobblemon.mod.common.duck.FilterableEditBox
import java.util.function.Predicate
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.toasts.ToastManager

/*
 * Client side bridges for vanilla members that were renamed or removed between 1.21.1 and 26.1.
 */

/** The partial tick the camera was set up with (Camera#getPartialTickTime before 1.21.2). */
val Camera.partialTickTime: Float
    get() = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(true)

val Minecraft.toasts: ToastManager
    get() = toastManager

val Minecraft.timer: DeltaTracker
    get() = deltaTracker

/** EditBox#setFilter, removed in 26.1 and restored by EditBoxMixin. */
fun EditBox.setFilter(filter: Predicate<String>) {
    (this as FilterableEditBox).`cobblemon$setFilter`(filter)
}
