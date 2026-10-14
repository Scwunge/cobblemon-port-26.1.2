/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.util.cobblemonResource
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.trading.TradeSet

/**
 * Cobblemon's villager and wandering trader trades.
 *
 * Since Minecraft 26.1 trades are data driven, so the offers that used to be built here now live in the mod's data:
 *  - data/cobblemon/villager_trade: the individual trades (nurse, fisherman and wandering trader offers)
 *  - data/cobblemon/trade_set/nurse: the nurse's trade set per villager level
 *  - data/cobblemon/tags/villager_trade/nurse: the trades available to each nurse level
 *  - data/minecraft/tags/villager_trade: Cobblemon trades appended to the fisherman and wandering trader pools
 * The offers themselves are unchanged from 1.21.1.
 */
object CobblemonTradeOffers {

    /** The nurse's trade set for each villager level (Novice = 1 .. Master = 5). */
    val NURSE_TRADE_SETS: Int2ObjectMap<ResourceKey<TradeSet>> = Int2ObjectMap.ofEntries(
        *(1..5).map { level -> Int2ObjectMap.entry(level, ResourceKey.create(Registries.TRADE_SET, cobblemonResource("nurse/level_$level"))) }.toTypedArray()
    )
}
