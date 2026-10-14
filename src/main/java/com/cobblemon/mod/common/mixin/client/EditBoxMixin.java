/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.duck.FilterableEditBox;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the value filter EditBox had up to 1.21.10: a value the filter rejects is never set.
 */
@Mixin(EditBox.class)
public abstract class EditBoxMixin implements FilterableEditBox {

    @Shadow
    private String value;

    @Unique
    private Predicate<String> cobblemon$filter = Objects::nonNull;

    @Override
    public void cobblemon$setFilter(Predicate<String> filter) {
        this.cobblemon$filter = filter;
    }

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void cobblemon$filterSetValue(String value, CallbackInfo ci) {
        if (!this.cobblemon$filter.test(value)) {
            ci.cancel();
        }
    }

    @Inject(method = "insertText", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;value:Ljava/lang/String;", opcode = org.objectweb.asm.Opcodes.PUTFIELD), cancellable = true)
    private void cobblemon$filterInsertText(String input, CallbackInfo ci, @Local(name = "start") int start, @Local(name = "end") int end, @Local(name = "text") String text) {
        String newValue = new StringBuilder(this.value).replace(start, end, text).toString();
        if (!this.cobblemon$filter.test(newValue)) {
            ci.cancel();
        }
    }

    @Inject(method = "deleteCharsToPos", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;value:Ljava/lang/String;", opcode = org.objectweb.asm.Opcodes.PUTFIELD), cancellable = true)
    private void cobblemon$filterDeleteChars(int pos, CallbackInfo ci, @Local(name = "start") int start, @Local(name = "end") int end) {
        String newValue = new StringBuilder(this.value).delete(start, end).toString();
        if (!this.cobblemon$filter.test(newValue)) {
            ci.cancel();
        }
    }
}
