/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import com.cobblemon.mod.common.client.render.gui.mouseClicked
import com.cobblemon.mod.common.client.render.gui.mouseReleased
import com.cobblemon.mod.common.client.render.gui.mouseDragged
import com.cobblemon.mod.common.client.render.gui.keyPressed
import com.cobblemon.mod.common.client.render.gui.keyReleased
import com.cobblemon.mod.common.client.render.gui.charTyped

/**
 * This class adds children-awareness to a Widget similar like a Screen
 * (otherwise the Widgets do not react on click/hover)
 */
abstract class ParentWidget(
    pX: Int, pY: Int,
    pWidth: Int, pHeight: Int,
    component: Component
): CobblemonRenderable, AbstractWidget(pX, pY, pWidth, pHeight, component) {

    val children: MutableList<GuiEventListener> = mutableListOf()

    /**
     * Adds Widget to the children list
     */
    protected fun addWidget(widget: GuiEventListener) {
        children.add(widget)
    }

    /**
     * Removes Widget from the children list
     */
    protected fun removeWidget(widget: GuiEventListener) {
        children.remove(widget)
    }

    override fun mouseMoved(pMouseX: Double, pMouseY: Double) {
        children.forEach {
            it.mouseMoved(pMouseX, pMouseY)
        }
        super.mouseMoved(pMouseX, pMouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return children.any {
            it.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        } || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean  {
        val pMouseX = event.x()
        val pMouseY = event.y()
        val pButton = event.button()
        if (!active) return false

        return children.any {
            it.mouseClicked(pMouseX, pMouseY, pButton)
        }
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean  {
        val pMouseX = event.x()
        val pMouseY = event.y()
        val pButton = event.button()
        return children.any {
            it.mouseReleased(pMouseX, pMouseY, pButton)
        } || super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean  {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()
        val f = dragX
        val g = dragY
        return children.any {
            it.mouseDragged(mouseX, mouseY, button, f, g)
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean  {
        val pKeyCode = event.key()
        val pScanCode = event.scancode()
        val pModifiers = event.modifiers()
        return children.any {
            it.keyPressed(pKeyCode, pScanCode, pModifiers)
        } || super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean  {
        val pKeyCode = event.key()
        val pScanCode = event.scancode()
        val pModifiers = event.modifiers()
        children.forEach {
            it.keyReleased(pKeyCode, pScanCode, pModifiers)
        }
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean  {
        val pCodePoint = event.codepoint().toChar()
        val pModifiers = 0
        children.forEach {
            it.charTyped(pCodePoint, pModifiers)
        }
        return super.charTyped(event)
    }

    override fun defaultButtonNarrationText(pNarrationElementOutput: NarrationElementOutput) {
    }

    /**
     * TODO
     *
     * @param mouseX
     * @param mouseY
     * @return
     *
     * @author Licious
     * @since April 29th, 2022
     */
    fun ishHovered(mouseX: Number, mouseY: Number) = mouseX in this.x..(this.x + this.width) && mouseY in this.y..(this.y + this.height)

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}

}