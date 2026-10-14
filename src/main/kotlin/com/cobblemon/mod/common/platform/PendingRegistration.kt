/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.platform

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

/**
 * Holds the registry key of the entry currently being constructed by [PlatformRegistry.create].
 *
 * Item and block properties need their ID at construction time since 1.21.2. Rather than threading the ID through every
 * Cobblemon item/block constructor, ItemRegistrationIdMixin and BlockRegistrationIdMixin apply the pending ID to the
 * properties of items and blocks constructed while an entry is being registered.
 */
object PendingRegistration {
    private val pending = ThreadLocal<ResourceKey<*>?>()

    fun <E> with(key: ResourceKey<*>, entry: () -> E): E {
        val previous = pending.get()
        pending.set(key)
        try {
            return entry()
        } finally {
            pending.set(previous)
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun pendingItem(): ResourceKey<Item>? = pending.get()?.takeIf { it.isFor(Registries.ITEM) } as ResourceKey<Item>?

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun pendingBlock(): ResourceKey<Block>? = pending.get()?.takeIf { it.isFor(Registries.BLOCK) } as ResourceKey<Block>?
}
