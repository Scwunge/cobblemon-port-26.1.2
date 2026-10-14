/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.item.crafting.CookingPotBookCategory
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.crafting.RecipeBookCategory

/**
 * The recipe book categories of the cooking pot.
 *
 * Recipe book categories are registry entries since 1.21.2, the search tab that aggregates them lives on the client
 * (see CookingPotRecipeBookComponent).
 */
object CobblemonRecipeCategories {
    val COOKING_POT_FOODS: RecipeBookCategory = register("cooking_pot_foods")
    val COOKING_POT_MEDICINES: RecipeBookCategory = register("cooking_pot_medicines")
    val COOKING_POT_COMPLEX_DISHES: RecipeBookCategory = register("cooking_pot_complex_dishes")
    val COOKING_POT_MISC: RecipeBookCategory = register("cooking_pot_misc")

    val COOKING_POT_CATEGORIES = listOf(COOKING_POT_FOODS, COOKING_POT_MEDICINES, COOKING_POT_COMPLEX_DISHES, COOKING_POT_MISC)

    fun forCookingPotCategory(category: CookingPotBookCategory): RecipeBookCategory = when (category) {
        CookingPotBookCategory.FOODS -> COOKING_POT_FOODS
        CookingPotBookCategory.MEDICINES -> COOKING_POT_MEDICINES
        CookingPotBookCategory.COMPLEX_DISHES -> COOKING_POT_COMPLEX_DISHES
        CookingPotBookCategory.MISC -> COOKING_POT_MISC
    }

    /** Forces the categories to be registered. */
    fun register() {}

    private fun register(name: String): RecipeBookCategory {
        return Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, cobblemonResource(name), RecipeBookCategory())
    }
}
