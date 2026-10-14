/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotRecipeBookComponent;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {

    @Shadow
    private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab;

    /**
     * Adds the cooking pot's search tab, which aggregates every cooking pot category, like the vanilla search tabs.
     */
    @Inject(method = "rebuildCollections", at = @At("RETURN"))
    private void cobblemon$addCookingPotSearchCategory(CallbackInfo ci) {
        Map<ExtendedRecipeBookCategory, List<RecipeCollection>> byCategory = new HashMap<>(this.collectionsByTab);
        byCategory.put(
            CookingPotRecipeBookComponent.CookingPotSearchCategory.INSTANCE,
            CookingPotRecipeBookComponent.CookingPotSearchCategory.INSTANCE.getIncludedCategories()
                .stream()
                .flatMap(subCategory -> byCategory.getOrDefault(subCategory, List.of()).stream())
                .collect(ImmutableList.toImmutableList())
        );
        this.collectionsByTab = Map.copyOf(byCategory);
    }

    /**
     * The cooking pot's book settings were reset whenever the recipe book was (re)sent by the server.
     */
    @Inject(method = "clear", at = @At("HEAD"))
    private void cobblemon$resetCookingPotSettings(CallbackInfo ci) {
        CookingPotRecipeBookComponent.resetSettings();
    }
}
