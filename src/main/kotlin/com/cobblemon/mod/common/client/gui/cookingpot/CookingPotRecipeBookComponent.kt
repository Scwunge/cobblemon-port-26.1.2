/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.cookingpot

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonRecipeCategories
import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.recipebook.GhostSlots
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
import net.minecraft.client.gui.screens.recipebook.RecipeCollection
import net.minecraft.network.chat.Component
import net.minecraft.recipebook.PlaceRecipeHelper
import net.minecraft.resources.Identifier
import net.minecraft.util.context.ContextMap
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import java.util.Optional

/**
 * The recipe book of the cooking pot: a search tab followed by the foods, medicines, complex dishes and misc tabs.
 *
 * @since 1.21.2 recipe books are built per menu type; this replaces the enum extensions the cooking pot used before.
 */
@Environment(EnvType.CLIENT)
class CookingPotRecipeBookComponent(menu: CookingPotMenu) : RecipeBookComponent<CookingPotMenu>(menu, TABS) {

    /** The tab that shows the recipes of every cooking pot category. */
    object CookingPotSearchCategory : ExtendedRecipeBookCategory {
        val includedCategories = CobblemonRecipeCategories.COOKING_POT_CATEGORIES
    }

    companion object {
        private val FILTER_BUTTON_SPRITES = WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_disabled_highlighted")
        )
        private val FILTER_NAME: Component = Component.translatable("cobblemon.container.campfire_pot.recipe_book.toggle_recipes")

        val TABS = listOf(
            TabInfo(ItemStack(Items.COMPASS), Optional.empty(), CookingPotSearchCategory),
            TabInfo(CobblemonItems.LEEK_AND_POTATO_STEW, CobblemonRecipeCategories.COOKING_POT_FOODS),
            TabInfo(CobblemonItems.POTION, CobblemonRecipeCategories.COOKING_POT_MEDICINES),
            TabInfo(CobblemonItems.APRIJUICE_RED, CobblemonRecipeCategories.COOKING_POT_COMPLEX_DISHES),
            TabInfo(CobblemonItems.PROTEIN, CobblemonRecipeCategories.COOKING_POT_MISC)
        )

        /*
         * The cooking pot's book settings are kept on the client only, like they were when the cooking pot had its own
         * RecipeBookType that was excluded from syncing: the book starts open and filtering every session.
         */
        @JvmStatic
        var bookOpen = true
        @JvmStatic
        var bookFiltering = true

        @JvmStatic
        fun resetSettings() {
            bookOpen = true
            bookFiltering = true
        }
    }

    override fun isCraftingSlot(slot: Slot): Boolean {
        return menu.resultSlot == slot || menu.inputGridSlots.contains(slot)
    }

    private fun canDisplay(display: RecipeDisplay): Boolean {
        return when (display) {
            is ShapedCraftingRecipeDisplay -> menu.gridWidth >= display.width() && menu.gridHeight >= display.height()
            is ShapelessCraftingRecipeDisplay -> menu.gridWidth * menu.gridHeight >= display.ingredients().size
            else -> false
        }
    }

    override fun selectMatchingRecipes(collection: RecipeCollection, stackedContents: StackedItemContents) {
        collection.selectRecipes(stackedContents, ::canDisplay)
    }

    override fun fillGhostRecipe(ghostSlots: GhostSlots, recipe: RecipeDisplay, context: ContextMap) {
        ghostSlots.setResult(menu.resultSlot, context, recipe.result())
        val inputSlots = menu.inputGridSlots
        when (recipe) {
            is ShapedCraftingRecipeDisplay -> PlaceRecipeHelper.placeRecipe(
                menu.gridWidth,
                menu.gridHeight,
                recipe.width(),
                recipe.height(),
                recipe.ingredients()
            ) { ingredient, gridIndex, _, _ -> ghostSlots.setInput(inputSlots[gridIndex], context, ingredient) }
            is ShapelessCraftingRecipeDisplay -> {
                val slotCount = minOf(recipe.ingredients().size, inputSlots.size)
                for (i in 0 until slotCount) {
                    ghostSlots.setInput(inputSlots[i], context, recipe.ingredients()[i])
                }
            }
        }
    }

    override fun getFilterButtonTextures() = FILTER_BUTTON_SPRITES

    override fun getRecipeFilterName() = FILTER_NAME

    // The settings are client side only, see bookOpen and bookFiltering.
    override fun sendUpdateSettings() {}
}
