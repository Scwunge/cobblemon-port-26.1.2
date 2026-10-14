/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.util.Mth
import kotlin.math.max

/**
 * Cobblemon's scrolling list. It lays out its rows itself (see [getRowTop]) rather than using vanilla's layout.
 *
 * Since 1.21.9 vanilla lists store each entry's position on the entry and entries render through
 * Entry#extractContent without position arguments. To keep every list pixel-identical, [renderEntry] places an entry
 * so that its content area (getX, getContentY, getWidth, getContentHeight) equals the row position, width and height
 * this widget used to pass to Entry#render. The old list API used by subclasses ([scrollAmount], [maxScroll],
 * [maxPosition], [scrollbarPosition], [itemHeight], [getEntry]) is kept here on top of the new one.
 */
abstract class ScrollingWidget<T : ObjectSelectionList.Entry<T>>(
    top: Int = 0,
    val left: Int = 0,
    width: Int = 10,
    height: Int = 10,
    slotHeight: Int = 10,
    val scrollBarWidth: Int = 5
) : ObjectSelectionList<T>(
    Minecraft.getInstance(),
    width, // Width
    height, // Height
    top, // Top
    slotHeight // Slot Height
), CobblemonRenderable {
    /** The height of a row (AbstractSelectionList#itemHeight before 1.21.9). */
    val itemHeight: Int = slotHeight

    override fun extractListBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractSelection(graphics: GuiGraphicsExtractor, entry: T, outlineColor: Int) {}
    open fun renderDecorations(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {}

    /** AbstractSelectionList#getScrollAmount/setScrollAmount before 1.21.9. */
    var scrollAmount: Double
        @JvmName("getCobblemonScrollAmount") get() = scrollAmount()
        @JvmName("setCobblemonScrollAmount") set(value) { setScrollAmount(value) }

    /** The total height of the rows (AbstractSelectionList#getMaxPosition before 1.21.9). */
    open val maxPosition: Int
        get() = this.itemCount * this.itemHeight

    /** AbstractSelectionList#getMaxScroll before 1.21.9. */
    open val maxScroll: Int
        get() = max(0, this.maxPosition - (this.height - 4))

    override fun maxScrollAmount(): Int = this.maxScroll

    override fun contentHeight(): Int = this.maxPosition + 4

    /** The x position of the scrollbar (AbstractSelectionList#getScrollbarPosition before 1.21.9). */
    open val scrollbarPosition: Int
        get() = this.left + this.width - this.scrollBarWidth

    override fun scrollBarX(): Int = this.scrollbarPosition

    open fun getEntry(index: Int): T = this.children()[index]

    init {
        setRectangle(width, height, top, top + height)
        setLeft(left)
    }

    final override fun setRectangle(width: Int, height: Int, top: Int, bottom: Int) {
        this.width = width
        this.height = height
        this.y = bottom
        this.x = left + width
    }

    fun setLeft(left: Int){
        this.x = left
    }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        this.focused = if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) this.getEntryAtPosition(
            mouseX.toDouble(),
            mouseY.toDouble()
        ) else null
        // The menu background and header that used to be optionally drawn here were always disabled for these lists.

        this.enableScissor(context)
        this.renderListItems(context, mouseX, mouseY, delta)
        context.disableScissor()

        val maxScroll = this.maxScroll
        if (maxScroll > 0) {
            renderScrollbar(context, mouseX, mouseY, delta)
        }

        this.renderDecorations(context, mouseX, mouseY)
    }

    open fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float){
        val xLeft = this.scrollbarPosition
        val xRight = xLeft + scrollBarWidth

        val barHeight = this.bottom - this.y

        var j2 = ((barHeight * barHeight).toFloat() / this.maxPosition.toFloat()).toInt()
        j2 = Mth.clamp(j2, 32, barHeight - 8)
        var k1 = scrollAmount.toInt() * (barHeight - j2) / this.maxScroll + this.y
        if (k1 < this.y) {
            k1 = this.y
        }

        context.fill(xLeft, this.y, xRight, this.bottom, -16777216)
        context.fill(xLeft, k1, xRight, k1 + j2, -8355712)
        context.fill(xLeft ,k1, xRight - 1, k1 + j2 - 1, -4144960)
    }

    override fun extractListItems(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        renderListItems(graphics, mouseX, mouseY, a)
    }

    open fun renderListItems(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val i = this.rowLeft
        val j = this.rowWidth
        val k = this.itemHeight
        val l = this.itemCount

        for (m in 0 until l) {
            val n = this.getRowTop(m)
            val o = this.getRowBottom(m)
            if (o >= this.y && n <= this.bottom) {
                this.renderItem(context, mouseX, mouseY, delta, m, i, n, j, k)
            }
        }
    }

    open fun renderItem(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        index: Int,
        x: Int,
        y: Int,
        entryWidth: Int,
        entryHeight: Int
    ) {
        val entry =  this.getEntry(index)
        renderEntry(entry, context, y, x, entryWidth, entryHeight, mouseX, mouseY, focused == entry, delta)
    }

    /** Renders [entry] with the row geometry Entry#render used to receive (top, left, width, height). */
    fun renderEntry(entry: T, context: GuiGraphicsExtractor, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, delta: Float) {
        // Entry content is inset by 2px on each side of the entry's bounds.
        entry.x = left
        entry.y = top - 2
        entry.setWidth(width)
        entry.setHeight(height + 4)
        entry.extractContent(context, mouseX, mouseY, hovered, delta)
    }

    override fun getRowLeft(): Int {
        return this.left
    }

    override fun getRowRight(): Int {
        return this.rowLeft + this.rowWidth
    }

    override fun getRowWidth(): Int {
        return this.width
    }

    override fun getRowTop(index: Int): Int {
        return this.y - scrollAmount.toInt() + (index * this.itemHeight)
    }

    override fun getRowBottom(index: Int): Int {
        return this.getRowTop(index) + this.itemHeight
    }

    abstract class Slot<T : Slot<T>>(): Entry<T>() {
        // Override render to show each individual element
    }
}
