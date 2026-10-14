/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

/**
 * GroundPathNavigation's createPath overrides lost their @Nullable in 1.21.11 even though paths can still fail to be
 * found, which Kotlin then refuses to override with a nullable result. This restores the nullable signatures.
 */
public abstract class NullableGroundPathNavigation extends GroundPathNavigation {
    public NullableGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public @Nullable Path createPath(BlockPos pos, int reachRange) {
        return createPathOrNull(pos, reachRange);
    }

    @Override
    public @Nullable Path createPath(Entity target, int reachRange) {
        return createPathOrNull(target, reachRange);
    }

    protected abstract @Nullable Path createPathOrNull(BlockPos pos, int reachRange);

    protected abstract @Nullable Path createPathOrNull(Entity target, int reachRange);
}
