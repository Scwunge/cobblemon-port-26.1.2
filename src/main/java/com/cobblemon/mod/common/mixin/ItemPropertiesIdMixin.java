/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.platform.PendingRegistration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the ID of the item Cobblemon is currently registering to properties created during its construction.
 * See {@link PendingRegistration}.
 */
@Mixin(Item.Properties.class)
public class ItemPropertiesIdMixin {
    @Shadow
    private @Nullable ResourceKey<Item> id;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cobblemon$applyPendingId(CallbackInfo ci) {
        if (this.id == null) {
            this.id = PendingRegistration.pendingItem();
        }
    }
}
