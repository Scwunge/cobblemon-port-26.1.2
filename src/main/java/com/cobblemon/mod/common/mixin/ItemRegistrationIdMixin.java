/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.platform.PendingRegistration;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Gives a item constructed by Cobblemon's registration its registry ID, which its properties must carry since
 * 1.21.2 (see {@link PendingRegistration}). The ID is applied right before the constructor reads it; properties shared
 * between several Cobblemon items therefore carry the right ID for each of them.
 */
@Mixin(Item.class)
public class ItemRegistrationIdMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Item.Properties cobblemon$applyPendingId(Item.Properties properties) {
        var id = PendingRegistration.pendingItem();
        return id != null ? properties.setId(id) : properties;
    }
}
