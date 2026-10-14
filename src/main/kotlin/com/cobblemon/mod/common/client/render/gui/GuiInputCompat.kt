/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo

/*
 * Since 1.21.9 input callbacks receive event objects. Cobblemon's screens forward raw coordinates/key codes to their
 * child widgets; these keep those forwarding calls as they were.
 */

fun hasShiftDown() = Minecraft.getInstance().hasShiftDown()
fun hasControlDown() = Minecraft.getInstance().hasControlDown()
fun hasAltDown() = Minecraft.getInstance().hasAltDown()

fun mouseEvent(x: Double, y: Double, button: Int) = MouseButtonEvent(x, y, MouseButtonInfo(button, 0))

fun GuiEventListener.mouseClicked(x: Double, y: Double, button: Int): Boolean = mouseClicked(mouseEvent(x, y, button), false)
fun GuiEventListener.mouseReleased(x: Double, y: Double, button: Int): Boolean = mouseReleased(mouseEvent(x, y, button))
fun GuiEventListener.mouseDragged(x: Double, y: Double, button: Int, dragX: Double, dragY: Double): Boolean =
    mouseDragged(mouseEvent(x, y, button), dragX, dragY)
fun GuiEventListener.keyPressed(key: Int, scancode: Int, modifiers: Int): Boolean = keyPressed(KeyEvent(key, scancode, modifiers))
fun GuiEventListener.keyReleased(key: Int, scancode: Int, modifiers: Int): Boolean = keyReleased(KeyEvent(key, scancode, modifiers))
fun GuiEventListener.charTyped(chr: Char, @Suppress("UNUSED_PARAMETER") modifiers: Int): Boolean = charTyped(CharacterEvent(chr.code))
