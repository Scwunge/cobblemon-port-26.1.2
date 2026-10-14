/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.keybind

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.KeyMapping

/**
 * Key mapping categories are registered identifiers since 1.21.9. Their labels are
 * key.category.cobblemon.cobblemon and key.category.cobblemon.cobblemon.debug, which carry the text of the old
 * key.cobblemon.categories.* keys.
 */
object KeybindCategories {
    val COBBLEMON_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(cobblemonResource("cobblemon"))
    val COBBLEMON_DEBUG_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(cobblemonResource("cobblemon.debug"))
}
