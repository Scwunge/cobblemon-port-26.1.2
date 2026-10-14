/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import com.cobblemon.mod.common.block.entity.*
import com.cobblemon.mod.common.block.habitat.HabitatBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockBuilder
import com.cobblemon.mod.common.platform.PlatformRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.entity.BlockEntityType

object CobblemonBlockEntities : PlatformRegistry<Registry<BlockEntityType<*>>, ResourceKey<Registry<BlockEntityType<*>>>, BlockEntityType<*>>() {

    override val registry: Registry<BlockEntityType<*>> = BuiltInRegistries.BLOCK_ENTITY_TYPE
    override val resourceKey: ResourceKey<Registry<BlockEntityType<*>>> = Registries.BLOCK_ENTITY_TYPE

    @JvmField
    val HEALING_MACHINE: BlockEntityType<HealingMachineBlockEntity> = this.create("healing_machine", FabricBlockEntityTypeBuilder.create(::HealingMachineBlockEntity, CobblemonBlocks.HEALING_MACHINE).build())

    @JvmField
    val PC: BlockEntityType<PCBlockEntity> = this.create("pc", FabricBlockEntityTypeBuilder.create(::PCBlockEntity, CobblemonBlocks.PC).build())

    @JvmField
    val LECTERN: BlockEntityType<LecternBlockEntity> = this.create("lectern", FabricBlockEntityTypeBuilder.create(::LecternBlockEntity, CobblemonBlocks.LECTERN).build())

    @JvmField
    val DISC_SHELF: BlockEntityType<DiscShelfBlockEntity> = this.create("disc_shelf", FabricBlockEntityTypeBuilder.create(::DiscShelfBlockEntity, CobblemonBlocks.DISC_SHELF).build())

    @JvmField
    val BERRY = this.create("berry", FabricBlockEntityTypeBuilder.create(::BerryBlockEntity, *CobblemonBlocks.berries().values.toTypedArray()).build())

    @JvmField
    val PASTURE: BlockEntityType<PokemonPastureBlockEntity> = this.create("pasture", FabricBlockEntityTypeBuilder.create(::PokemonPastureBlockEntity, CobblemonBlocks.PASTURE).build())
    @JvmField
    val SIGN: BlockEntityType<CobblemonSignBlockEntity> = this.create("sign", FabricBlockEntityTypeBuilder.create(::CobblemonSignBlockEntity, CobblemonBlocks.APRICORN_SIGN, CobblemonBlocks.APRICORN_WALL_SIGN, CobblemonBlocks.SACCHARINE_SIGN, CobblemonBlocks.SACCHARINE_WALL_SIGN).build())
    @JvmField
    val HANGING_SIGN: BlockEntityType<CobblemonHangingSignBlockEntity> = this.create("hanging_sign", FabricBlockEntityTypeBuilder.create(::CobblemonHangingSignBlockEntity, CobblemonBlocks.APRICORN_HANGING_SIGN, CobblemonBlocks.APRICORN_WALL_HANGING_SIGN, CobblemonBlocks.SACCHARINE_HANGING_SIGN, CobblemonBlocks.SACCHARINE_WALL_HANGING_SIGN).build())
    @JvmField
    val TM_MACHINE: BlockEntityType<TMMachineBlockEntity> = this.create("tm_machine", FabricBlockEntityTypeBuilder.create(::TMMachineBlockEntity, CobblemonBlocks.TM_MACHINE).build())

    @JvmField
    val HABITAT_BLOCK: BlockEntityType<HabitatBlockEntity> = this.create("habitat_block",
            FabricBlockEntityTypeBuilder.create(::HabitatBlockEntity, CobblemonBlocks.HABITAT_BLOCK).build()
    )

    @JvmField
    val GILDED_CHEST: BlockEntityType<GildedChestBlockEntity> = this.create("chest", FabricBlockEntityTypeBuilder.create(::GildedChestBlockEntity,
        CobblemonBlocks.GILDED_CHEST,
        CobblemonBlocks.BLUE_GILDED_CHEST,
        CobblemonBlocks.YELLOW_GILDED_CHEST,
        CobblemonBlocks.PINK_GILDED_CHEST,
        CobblemonBlocks.BLACK_GILDED_CHEST,
        CobblemonBlocks.WHITE_GILDED_CHEST,
        CobblemonBlocks.GREEN_GILDED_CHEST,
        CobblemonBlocks.GIMMIGHOUL_CHEST
    ).build())

    @JvmField
    val FOSSIL_MULTIBLOCK: BlockEntityType<FossilMultiblockEntity> = this.create("fossil_multiblock",
        FabricBlockEntityTypeBuilder.create({ pos, state -> FossilMultiblockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.MONITOR
        ).build()
    )

    @JvmField
    val RESTORATION_TANK: BlockEntityType<RestorationTankBlockEntity> = this.create("restoration_tank",
        FabricBlockEntityTypeBuilder.create({ pos, state -> RestorationTankBlockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.RESTORATION_TANK
        ).build()
    )

    @JvmField
    val FOSSIL_ANALYZER: BlockEntityType<FossilAnalyzerBlockEntity> = this.create("fossil_analyzer",
        FabricBlockEntityTypeBuilder.create({ pos, state -> FossilAnalyzerBlockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.FOSSIL_ANALYZER
        ).build()
    )

    @JvmField
    val DISPLAY_CASE: BlockEntityType<DisplayCaseBlockEntity> = this.create("display_case",
        FabricBlockEntityTypeBuilder.create(::DisplayCaseBlockEntity, CobblemonBlocks.DISPLAY_CASE).build()
    )

    // TODO after 1.7
//    @JvmField
//    val INCENSE_SWEET: BlockEntityType<SweetIncenseBlockEntity> = this.create("incense_sweet",
//            FabricBlockEntityTypeBuilder.create(::SweetIncenseBlockEntity, CobblemonBlocks.INCENSE_SWEET).build()
//    )

    @JvmField
    val CAMPFIRE: BlockEntityType<CampfireBlockEntity> = this.create("campfire_pot",
            FabricBlockEntityTypeBuilder.create(::CampfireBlockEntity,
                CobblemonBlocks.CAMPFIRE,
                CobblemonBlocks.SOUL_CAMPFIRE
            ).build()
    )

    @JvmField
    val POKE_SNACK: BlockEntityType<PokeSnackBlockEntity> = this.create(
        "poke_snack",
        FabricBlockEntityTypeBuilder.create(::PokeSnackBlockEntity, CobblemonBlocks.POKE_SNACK, CobblemonBlocks.POKE_CAKE).build()
    )

    @JvmField
    val TERRAIN_PLANT: BlockEntityType<TerrainPlantBlockEntity> = this.create(
        "terrain_plant",
        FabricBlockEntityTypeBuilder.create(::TerrainPlantBlockEntity,
            CobblemonBlocks.VOLTACONIA,
            CobblemonBlocks.LUCIBELL,
            CobblemonBlocks.VERDROOT
        ).build()
    )
    @JvmField
    val MISTLEAF_FLOWER: BlockEntityType<MistleafFlowerBlockEntity> = this.create(
        "mistleaf",
        FabricBlockEntityTypeBuilder.create(::MistleafFlowerBlockEntity,
            CobblemonBlocks.MISTLEAF
        ).build()
    )
}
