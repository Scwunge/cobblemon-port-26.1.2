/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai;

import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.jspecify.annotations.Nullable;

/**
 * NodeEvaluator#getNode(int, int, int) is null-marked since 1.21.11, but Cobblemon's evaluator reports unusable
 * positions as no node, as it did before. Kotlin can't override with a nullable result, so this restores it;
 * {@link #baseNode} is the vanilla (always present) node.
 */
public abstract class NullableNodeEvaluator extends NodeEvaluator {
    @Override
    protected @Nullable Node getNode(int x, int y, int z) {
        return getNodeOrNull(x, y, z);
    }

    protected abstract @Nullable Node getNodeOrNull(int x, int y, int z);

    protected final Node baseNode(int x, int y, int z) {
        return super.getNode(x, y, z);
    }
}
