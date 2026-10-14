/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.boat

import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.mixin.accessor.BoatEntityAccessor
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerEntity
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.EntityType
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import java.util.function.Supplier
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.WoodType

@Suppress("unused")
/**
 * Cobblemon's boats. A single entity type covers every [CobblemonBoatType], which is synced and saved separately.
 *
 * Since 1.21.2 a boat's drop item is supplied when it is constructed (it used to be an overridable getter), so the
 * supplier resolves it from this boat's type.
 */
open class CobblemonBoatEntity private constructor(
    entityType: EntityType<out Boat>,
    world: Level,
    private val dropItemSupplier: DropItemSupplier
) : Boat(entityType, world, dropItemSupplier) {

    constructor(entityType: EntityType<out Boat>, world: Level) : this(entityType, world, DropItemSupplier())

    constructor(world: Level) : this(CobblemonEntities.BOAT, world)

    // This exists cause super passes in vanilla boat entity type
    constructor(world: Level, x: Double, y: Double, z: Double) : this(CobblemonEntities.BOAT, world) {
        this.setPos(x, y, z)
        this.xo = x
        this.yo = y
        this.zo = z
    }

    init {
        dropItemSupplier.boat = this
    }

    /**
     * The [CobblemonBoatType] of this boat.
     */
    var boatType: CobblemonBoatType
        get() = CobblemonBoatType.ofOrdinal(this.entityData.get(TYPE_TRACKED_DATA))
        set(value) {
            this.entityData.set(TYPE_TRACKED_DATA, value.ordinal)
        }

    /**
     * The [WoodType] that represents this boat.
     */
    val woodType: WoodType get() = this.boatType.woodType

    /**
     * The [Block] that represents the base material for this boat.
     */
    val baseBlock: Block get() = this.boatType.baseBlock

    /** The item this boat drops (AbstractBoat#getDropItem is final since 1.21.2). */
    protected open fun dropItem(): Item = this.boatType.boatItem

    override fun getAddEntityPacket(entityTrackerEntry: ServerEntity): Packet<ClientGamePacketListener> = ClientboundAddEntityPacket(this, entityTrackerEntry)

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(TYPE_TRACKED_DATA, CobblemonBoatType.APRICORN.ordinal)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        input.getString(TYPE_KEY).ifPresent { this.boatType = CobblemonBoatType.valueOf(it) }
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.putString(TYPE_KEY, this.boatType.name)
    }

    override fun getSinglePassengerXOffset(): Float = this.boatType.mountedOffset

    // Boats broke into planks and sticks when falling onto land until 1.21.2; Cobblemon's boats keep doing so.
    override fun checkFallDamage(heightDifference: Double, onGround: Boolean, state: BlockState, landedPosition: BlockPos) {
        val accessor = this.accessor()
        accessor.setFallVelocity(this.deltaMovement.y)
        if (!this.isPassenger()) {
            return
        }
        if (!this.level().getFluidState(this.blockPosition().below()).`is`(FluidTags.WATER) && heightDifference < 0.0) {
            this.fallDistance -= heightDifference
        }
        if (!onGround) {
            return
        }
        if (this.fallDistance < 3.0 || accessor.location != AbstractBoat.Status.ON_LAND) {
            this.resetFallDistance()
            return
        }
        this.causeFallDamage(this.fallDistance, 1F, this.damageSources().fall())
        val level = this.level() as? ServerLevel ?: return
        if (this.isRemoved) {
            return
        }
        this.kill(level)
        if (level.gameRules.get(GameRules.ENTITY_DROPS)) {
            repeat(3) {
                this.spawnAtLocation(level, this.boatType.baseBlock)
            }
            repeat(2) {
                this.spawnAtLocation(level, Items.STICK)
            }
        }
    }

    protected fun accessor(): BoatEntityAccessor = this as BoatEntityAccessor

    private class DropItemSupplier : Supplier<Item> {
        lateinit var boat: CobblemonBoatEntity
        override fun get(): Item = boat.dropItem()
    }

    companion object {

        private const val TYPE_KEY = "type"
        private val TYPE_TRACKED_DATA = SynchedEntityData.defineId(CobblemonBoatEntity::class.java, EntityDataSerializers.INT)

    }

}
