/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.duck;

/**
 * The plain name of the villager a (zombie) villager render state was extracted from. Render layers don't see the
 * entity since 1.21.2, and the profession texture overrides depend on the villager's name.
 */
public interface VillagerNameHolder {
    String cobblemon$getEntityName();

    void cobblemon$setEntityName(String name);
}
