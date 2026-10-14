/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.cookingpot

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.gui.legacyTextColour
import com.cobblemon.mod.common.CobblemonNetwork.sendToServer
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.block.campfirepot.CampfirePotColor
import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu
import com.cobblemon.mod.common.block.campfirepot.SeasoningSlot
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.COOKING_POT_COLOR_INDEX
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.IS_LID_OPEN_INDEX
import com.cobblemon.mod.common.integration.jei.CobblemonJeiPlugin
import com.cobblemon.mod.common.integration.jei.cooking.CampfirePotRecipeCategory
import com.cobblemon.mod.common.net.messages.client.cooking.ToggleCookingPotLidPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenPosition
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import kotlin.math.ceil

@Environment(EnvType.CLIENT)
class CookingPotScreen(
    menu: CookingPotMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractRecipeBookScreen<CookingPotMenu>(
    menu,
    CookingPotRecipeBookComponent(menu),
    playerInventory,
    Component.translatable("cobblemon.container.campfire_pot")
) {

    companion object {
        private const val BACKGROUND_HEIGHT = 166
        private const val BACKGROUND_WIDTH = 176

        const val COOK_PROGRESS_HEIGHT = 12
        const val COOK_PROGRESS_WIDTH = 22

        private val BACKGROUND = cobblemonResource("textures/gui/campfirepot/campfire_pot.png")
        val COOK_PROGRESS_SPRITE: Identifier = cobblemonResource("textures/gui/campfirepot/cook_progress.png")

        /** The colour of the result slot's background, the preview is drawn at half opacity over it. */
        private const val PREVIEW_FADE_COLOUR = 0x808B8B8B.toInt()
    }

    private lateinit var cookButton: CookButton

    // The image size is the default 176x166 (BACKGROUND_WIDTH x BACKGROUND_HEIGHT).

    override fun getRecipeBookButtonPosition(): ScreenPosition {
        return ScreenPosition(this.leftPos + 5, ((height - BACKGROUND_HEIGHT) / 2) + 35)
    }

    override fun init() {
        super.init()
        val topPos = ((height - BACKGROUND_HEIGHT) / 2)

        this.titleLabelX = this.imageWidth - (BACKGROUND_WIDTH / 2) - (this.font.width(this.title) / 2)
        this.titleLabelY = 6

        if (::cookButton.isInitialized) removeWidget(cookButton)
        val color = CampfirePotColor.entries[menu.containerData.get(COOKING_POT_COLOR_INDEX).coerceIn(0, CampfirePotColor.entries.lastIndex)]
        cookButton = CookButton(this.leftPos + 97, topPos + 56, menu.containerData.get(IS_LID_OPEN_INDEX) == 0, color) {
            val isLidClosed = menu.containerData.get(IS_LID_OPEN_INDEX) == 0
            sendToServer(ToggleCookingPotLidPacket(isLidClosed))
        }

        addRenderableWidget(cookButton)
    }

    override fun extractLabels(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, legacyTextColour(4210752), false)
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractBackground(context, mouseX, mouseY, partialTick)
        renderBg(context, partialTick, mouseX, mouseY)
    }

    fun renderBg(
        context: GuiGraphicsExtractor,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int
    ) {
        blitk(
            matrixStack = context.pose(),
            texture = BACKGROUND,
            x = leftPos, y = (height - BACKGROUND_HEIGHT) / 2,
            width = BACKGROUND_WIDTH, height = BACKGROUND_HEIGHT
        )

        val cookProgress = ceil(menu.getBurnProgress() * COOK_PROGRESS_WIDTH).toInt()
        blitk(
            matrixStack = context.pose(),
            texture = COOK_PROGRESS_SPRITE,
            x = leftPos + 96,
            y = topPos + 39,
            width = cookProgress,
            height = COOK_PROGRESS_HEIGHT,
            textureWidth = COOK_PROGRESS_WIDTH
        )
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {
        cookButton.color = CampfirePotColor.entries[menu.containerData.get(COOKING_POT_COLOR_INDEX).coerceIn(0, CampfirePotColor.entries.lastIndex)]
        cookButton.selected = menu.containerData.get(IS_LID_OPEN_INDEX) == 0
        cookButton.setPosition(this.leftPos + 97, topPos + 56)

        super.extractRenderState(context, mouseX, mouseY, partialTicks)
    }

    // Slots are drawn relative to the menu's corner.
    override fun extractSlots(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        super.extractSlots(context, mouseX, mouseY)

        val resultSlot = menu.resultSlot
        val optionalRecipe = menu.currentActiveRecipe
        if (optionalRecipe != null && !optionalRecipe.value.result.isEmpty && !resultSlot.hasItem()) {
            val resultItem = menu.previewItem

            // Drawn at half opacity: the slot background is blended back over the item.
            context.fakeItem(resultItem, resultSlot.x, resultSlot.y)
            context.itemDecorations(font, resultItem, resultSlot.x, resultSlot.y)
            context.fill(resultSlot.x, resultSlot.y, resultSlot.x + 16, resultSlot.y + 16, PREVIEW_FADE_COLOUR)

            if (isHovering(resultSlot.x, resultSlot.y, 16, 16, mouseX.toDouble(), mouseY.toDouble()) && menu.carried.isEmpty) {
                context.setTooltipForNextFrame(font, getTooltipFromContainerItem(resultItem), resultItem.tooltipImage, mouseX, mouseY)
            }
        }
    }

    override fun extractSlot(guiGraphics: GuiGraphicsExtractor, slot: Slot, mouseX: Int, mouseY: Int) {
        if (slot is SeasoningSlot && ((!menu.carried.isEmpty && !slot.mayPlace(menu.carried)) || (slot.hasItem() && !slot.mayPlace(slot.item))) ) {
            val x = slot.x
            val y = slot.y
            guiGraphics.fill(x,y,x+16,y+16,822018048)
        }
        super.extractSlot(guiGraphics, slot, mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean  {
        val mouseX = event.x()
        val mouseY = event.y()
        val progressX = leftPos + 96
        val progressY = topPos + 39
        if (Cobblemon.implementation.isModInstalled("jei") &&
            mouseX >= progressX && mouseX < progressX + COOK_PROGRESS_WIDTH &&
            mouseY >= progressY && mouseY < progressY + COOK_PROGRESS_HEIGHT
        ) {
            CobblemonJeiPlugin.jeiRuntime?.recipesGui?.showTypes(
                listOf(CampfirePotRecipeCategory.RECIPE_TYPE)
            )
            return true
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun extractTooltip(
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int
    ) {
        val progressX = leftPos + 96
        val progressY = topPos + 39
        if (Cobblemon.implementation.isModInstalled("jei") &&
            mouseX >= progressX && mouseX < progressX + COOK_PROGRESS_WIDTH &&
            mouseY >= progressY && mouseY < progressY + COOK_PROGRESS_HEIGHT
        ) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                Component.translatable("jei.tooltip.show.recipes"),
                mouseX,
                mouseY
            )
            return
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY)
    }
}
