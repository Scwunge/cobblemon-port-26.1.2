/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.duck;

/**
 * An entity whose scale is not driven by the scale attribute. LivingEntity#getScale was overridable until 1.21.5;
 * LivingEntityMixin returns this value from it.
 */
public interface ScaleOverridingEntity {
    float cobblemon$getScale();
}
