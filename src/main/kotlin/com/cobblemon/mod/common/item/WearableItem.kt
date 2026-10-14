/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.world.InteractionResult
import net.minecraft.resources.Identifier
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level

/**
 * Items worn on the head. Equipable became the equippable component in 1.21.2; [equippable] is what each wearable
 * gives that component.
 */
interface WearableItem {
    val name: String

    companion object {
        const val MODEL_PATH = "item/wearable"

        /** What Equipable#getEquipmentSlot and the default Equipable#getEquipSound amounted to. */
        fun equippable(): net.minecraft.world.item.equipment.Equippable =
            net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.HEAD).build()
    }

    fun getModel3d(): Identifier = cobblemonResource("${MODEL_PATH}/${this.name}")
    fun getModel2d(): Identifier = cobblemonResource(this.name)

    fun getEquipmentSlot(): EquipmentSlot {
        return EquipmentSlot.HEAD
    }

    /**
     * Swap stack with equipment slot
     * Copied from vanilla, modified to account for stackable items
     * @see net.minecraft.world.item.equipment.Equippable.swapWithEquipmentSlot
     *
     * @param item
     * @param level
     * @param player
     * @param hand
     * @return the interaction result holder
     */
    fun swapStackWithEquipmentSlot(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        var itemStack = player.getItemInHand(hand)
        val equipmentSlot = player.getEquipmentSlotForItem(itemStack)

        if (!player.canUseSlot(equipmentSlot)) return InteractionResult.PASS

        val slotStack = player.getItemBySlot(equipmentSlot)

        if (!(!EnchantmentHelper.has(slotStack, PREVENT_ARMOR_CHANGE) || player.isCreative) || ItemStack.matches(itemStack, slotStack)) return InteractionResult.FAIL
        if (!level.isClientSide()) player.awardStat(Stats.ITEM_USED.get(itemStack.item))

        val equipStack = itemStack.split(1)
        when {
            itemStack.isEmpty -> itemStack = slotStack.copyAndClear()
            else -> player.addItem(slotStack.copyAndClear())
        }

        player.setItemSlot(equipmentSlot, equipStack)
        return InteractionResult.SUCCESS
    }
}
