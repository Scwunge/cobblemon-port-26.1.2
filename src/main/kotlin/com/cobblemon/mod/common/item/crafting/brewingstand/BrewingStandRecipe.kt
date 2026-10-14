/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting.brewingstand

import com.cobblemon.mod.common.CobblemonRecipeSerializers
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes
import net.minecraft.world.item.crafting.RecipeBookCategories
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class BrewingStandRecipe(
    val groupName: String,
    val input: Ingredient,
    val bottle: Ingredient,
    val result: ItemStack
) : Recipe<BrewingStandInput> {

    override fun getType() = CobblemonRecipeTypes.BREWING_STAND
    override fun getSerializer(): RecipeSerializer<BrewingStandRecipe> = SERIALIZER
    override fun assemble(input: BrewingStandInput): ItemStack = result.copy()
    fun getResultItem(): ItemStack = result.copy()
    override fun group() = groupName
    override fun showNotification() = true
    // Brewing stand recipes are not part of the recipe book (as before 1.21.2, where they had no book category).
    override fun isSpecial() = true
    override fun placementInfo(): PlacementInfo = PlacementInfo.NOT_PLACEABLE
    override fun recipeBookCategory(): RecipeBookCategory = RecipeBookCategories.CRAFTING_MISC

    override fun matches(input: BrewingStandInput, level: Level): Boolean {
        val ingredientMatches = this.input.test(input.getIngredient())
        val validBottles = input.getBottles()
            .filter { !it.isEmpty }
            .all { this.bottle.test(it) }

        return ingredientMatches && validBottles
    }

    companion object {
        val SERIALIZER by lazy { RecipeSerializer(Serializer.CODEC, Serializer.STREAM_CODEC) }

        fun isBottle(itemStack: ItemStack, recipeManager: SynchronizedRecipes): Boolean {
            val recipes = recipeManager.getAllOfType(CobblemonRecipeTypes.BREWING_STAND)
            return recipes.any { recipe ->
                recipe.value.bottle.test(itemStack)
            }
        }

        fun isInput(itemStack: ItemStack, recipeManager: SynchronizedRecipes): Boolean {
            val recipes = recipeManager.getAllOfType(CobblemonRecipeTypes.BREWING_STAND)
            return recipes.any { recipe ->
                recipe.value.input.test(itemStack)
            }
        }
    }

    object Serializer {
            val CODEC: MapCodec<BrewingStandRecipe> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter { recipe -> recipe.groupName },
                    Ingredient.CODEC.fieldOf("input").forGetter { recipe -> recipe.input },
                    Ingredient.CODEC.fieldOf("bottle").forGetter { recipe -> recipe.bottle },
                    ItemStack.CODEC.fieldOf("result").forGetter { recipe -> recipe.result }
                ).apply(instance, ::BrewingStandRecipe)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BrewingStandRecipe> =
                StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buffer: RegistryFriendlyByteBuf): BrewingStandRecipe {
                val group = buffer.readUtf(32767)
                val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val bottle = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val result = ItemStack.STREAM_CODEC.decode(buffer)
                return BrewingStandRecipe(group, input, bottle, result)
            }

            private fun toNetwork(buffer: RegistryFriendlyByteBuf, recipe: BrewingStandRecipe) {
                buffer.writeUtf(recipe.groupName)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.bottle)
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result)
            }
    }
}