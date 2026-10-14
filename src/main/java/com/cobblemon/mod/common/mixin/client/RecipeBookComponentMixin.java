/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotRecipeBookComponent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {

    /**
     * The cooking pot's search tab is always shown, like the vanilla search tabs.
     */
    @ModifyExpressionValue(
        method = "updateTabs",
        at = @At(value = "CONSTANT", args = "classValue=net/minecraft/client/gui/screens/recipebook/SearchRecipeBookCategory")
    )
    private boolean cobblemon$isSearchTab(boolean original, @Local RecipeBookTabButton tabButton) {
        return original || tabButton.getCategory() == CookingPotRecipeBookComponent.CookingPotSearchCategory.INSTANCE;
    }

    // The cooking pot keeps its book settings itself, see CookingPotRecipeBookComponent.

    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void cobblemon$isCookingPotBookOpen(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CookingPotRecipeBookComponent) {
            cir.setReturnValue(CookingPotRecipeBookComponent.getBookOpen());
        }
    }

    @WrapWithCondition(method = "setVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientRecipeBook;setOpen(Lnet/minecraft/world/inventory/RecipeBookType;Z)V"))
    private boolean cobblemon$setCookingPotBookOpen(ClientRecipeBook book, RecipeBookType type, boolean open) {
        if ((Object) this instanceof CookingPotRecipeBookComponent) {
            CookingPotRecipeBookComponent.setBookOpen(open);
            return false;
        }
        return true;
    }

    @Inject(method = "isFiltering", at = @At("HEAD"), cancellable = true)
    private void cobblemon$isCookingPotBookFiltering(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CookingPotRecipeBookComponent) {
            cir.setReturnValue(CookingPotRecipeBookComponent.getBookFiltering());
        }
    }

    @Inject(method = "toggleFiltering", at = @At("HEAD"), cancellable = true)
    private void cobblemon$toggleCookingPotBookFiltering(CallbackInfo ci) {
        if ((Object) this instanceof CookingPotRecipeBookComponent) {
            CookingPotRecipeBookComponent.setBookFiltering(!CookingPotRecipeBookComponent.getBookFiltering());
            ci.cancel();
        }
    }
}
