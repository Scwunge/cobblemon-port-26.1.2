/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.duck;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * The entity (and partial tick) an entity render state was extracted from. Entity renderers only see render states
 * since 1.21.2; Cobblemon's rendering hooks that need the entity itself get it from here.
 */
public interface EntityRenderStateSource {
    @Nullable Entity cobblemon$getEntity();

    float cobblemon$getPartialTick();

    void cobblemon$setSource(Entity entity, float partialTick);
}
