/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory

/**
 * Cobblemon's game rules. Since 26.1 game rules are registry entries with lowercase identifiers, so the rules that
 * were previously named e.g. "doPokemonSpawning" are now "cobblemon:do_pokemon_spawning".
 */
object CobblemonGameRules {

    @JvmField
    val BATTLE_INVULNERABILITY: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("battle_invulnerability", GameRuleCategory.PLAYER, false)
    @JvmField
    val DO_POKEMON_SPAWNING: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("do_pokemon_spawning", GameRuleCategory.SPAWNING, true)
    @JvmField
    val DO_POKEMON_LOOT: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("do_pokemon_loot", GameRuleCategory.DROPS, true)
    @JvmField
    val MOB_TARGET_IN_BATTLE: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("mob_target_in_battle", GameRuleCategory.MOBS, true)
    @JvmField
    val SHINY_STARTERS: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("do_shiny_starters", GameRuleCategory.MISC, false)
    @JvmField
    val HEALERS_HEAL_PC: GameRule<Boolean> = Cobblemon.implementation.registerBooleanGameRule("healers_heal_pc", GameRuleCategory.MISC, false)

}
