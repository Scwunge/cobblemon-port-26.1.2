/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tmmachine;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Reports every slot click to {@link #afterSlotClicked()}. AbstractContainerScreen declares slotClicked's slot as
 * non-null but passes null for clicks outside the container (dropping the carried stack), which a Kotlin override
 * rejects with a NullPointerException before its body runs, so the override lives here instead.
 */
public abstract class SlotClickAwareContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected SlotClickAwareContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput input) {
        super.slotClicked(slot, slotId, mouseButton, input);
        afterSlotClicked();
    }

    protected abstract void afterSlotClicked();
}
