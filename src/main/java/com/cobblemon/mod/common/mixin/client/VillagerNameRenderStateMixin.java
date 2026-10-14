/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.duck.VillagerNameHolder;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** See {@link VillagerNameHolder}. */
@Mixin({VillagerRenderState.class, ZombieVillagerRenderState.class})
public class VillagerNameRenderStateMixin implements VillagerNameHolder {
    @Unique
    private String cobblemon$entityName = "";

    @Override
    public String cobblemon$getEntityName() {
        return cobblemon$entityName;
    }

    @Override
    public void cobblemon$setEntityName(String name) {
        cobblemon$entityName = name;
    }
}
