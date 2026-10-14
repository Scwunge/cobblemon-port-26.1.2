/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.level.Level

/**
 * The moon phase index (0 = full moon .. 7 = waxing gibbous) as Level#getMoonPhase returned it before world clocks and
 * environment attributes replaced it in 26.1.
 */
val Level.moonPhase: Int
    get() = environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE).index()
