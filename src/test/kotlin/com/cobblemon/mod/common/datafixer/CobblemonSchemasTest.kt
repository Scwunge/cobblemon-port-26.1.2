/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer

import com.cobblemon.mod.common.junit.BootstrapMinecraft
import com.mojang.serialization.Dynamic
import java.time.Duration
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.TagParser
import net.minecraft.util.datafix.fixes.References
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier

/**
 * Pokémon saved before Cobblemon 1.6 have no data version, so they go through every fix from version 0.
 * Those rules are built on first use and used to hang the second caller forever if building them threw.
 */
@BootstrapMinecraft
internal class CobblemonSchemasTest {

    // A wild Rattata saved by Cobblemon 1.4.0 into CobbleTowns' Lavender Town tower, with an old style held item added.
    private val pokemon = """
        {Tradeable:1b,PersistentData:{},Gender:"MALE",HealingTimer:60,Friendship:50s,GmaxFactor:0b,CaughtBall:"cobblemon:poke_ball",
        alolan:0b,ScaleModifier:1.0f,FormId:"normal",UUID:[I;1936488240,-1835318988,-1186447479,741963696],CobblemonVersion:"1.4.0",
        Species:"cobblemon:rattata",Shiny:0b,PokemonData:[],Ability:{AbilityName:"guts",AbilityPriority:"LOWEST",AbilityForced:0b,AbilityIndex:1},
        Nature:"cobblemon:bold",BenchedMoves:[],EVs:{},Health:30s,TeraType:"normal",
        MoveSet:[{MovePP:25,MoveName:"bite",RaisedPPStages:0},{MovePP:30,MoveName:"focusenergy",RaisedPPStages:0}],
        IVs:{defence:15s,attack:5s,hp:13s,special_defence:8s,special_attack:25s,speed:22s},FaintedTimer:-1,DmaxLevel:0,
        Evolutions:{Pending:{progress:[],pending:[]}},Experience:1728,Level:12s,HeldItem:{id:"minecraft:apple",Count:3b}}
    """.trimIndent().replace("\n", "")

    private fun update(type: com.mojang.datafixers.DSL.TypeReference, tag: CompoundTag): CompoundTag =
        CobblemonSchemas.DATA_FIXER.update(type, Dynamic(NbtOps.INSTANCE, tag), 0, CobblemonSchemas.DATA_VERSION).value as CompoundTag

    @Test
    fun `pokemon without a data version is upgraded without hanging`() {
        repeat(2) {
            val fixed = assertTimeoutPreemptively(Duration.ofSeconds(60), ThrowingSupplier {
                update(CobblemonTypeReferences.POKEMON, TagParser.parseCompoundFully(pokemon))
            })
            println("Fixed Pokémon: $fixed")
            assertEquals("cobblemon:rattata", fixed.getStringOr("Species", ""))
            assertEquals(3, fixed.getCompoundOrEmpty("HeldItem").getIntOr("count", 0))
        }
    }

    @Test
    fun `pokemon entity without a data version is upgraded without hanging`() {
        repeat(2) {
            val fixed = assertTimeoutPreemptively(Duration.ofSeconds(60), ThrowingSupplier {
                update(References.ENTITY, TagParser.parseCompoundFully("{id:\"cobblemon:pokemon\",Pokemon:$pokemon}"))
            })
            println("Fixed entity: $fixed")
            assertEquals("cobblemon:rattata", fixed.getCompoundOrEmpty("Pokemon").getStringOr("Species", ""))
            assertEquals(3, fixed.getCompoundOrEmpty("Pokemon").getCompoundOrEmpty("HeldItem").getIntOr("count", 0))
        }
    }
}
