/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import net.minecraft.world.item.crafting.Ingredient

/**
 * Represents the ingredients necessary to craft a [TechnicalMachine] in the [TMBlock]
 */
class TechnicalMachineRecipe(
    ingredient: () -> Ingredient,
    val count: Int
) {
    constructor(ingredient: Ingredient, count: Int) : this({ ingredient }, count)

    /**
     * Resolved on first use: since 1.21.2 a tag ingredient needs the tag's holder set, and item tags are only bound
     * after the data reload that loads TMs.
     */
    val ingredient: Ingredient by lazy(ingredient)
}
