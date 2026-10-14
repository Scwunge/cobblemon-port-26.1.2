/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation

/**
 * An item that is used (eaten or drunk) without being a consumable.
 *
 * Until 1.21.2 every item played eating/drinking sounds (and eating particles) while being used with the EAT or DRINK
 * animation. Those effects now come from the consumable component, so such items call [tickUseEffects] from
 * onUseTick to keep them.
 */
interface LegacyUseEffectsItem {
    fun getEatingSound(): SoundEvent = SoundEvents.GENERIC_EAT.value()
    fun getDrinkingSound(): SoundEvent = SoundEvents.GENERIC_DRINK.value()

    /** LivingEntity#updatingUsingItem's use effects from 1.21.1. */
    fun tickUseEffects(user: LivingEntity, stack: ItemStack, remainingUseDuration: Int) {
        val usedFor = stack.getUseDuration(user) - remainingUseDuration
        val waitTicks = (stack.getUseDuration(user) * 0.21875F).toInt()
        if (usedFor <= waitTicks || remainingUseDuration % 4 != 0 || stack.isEmpty || !user.isUsingItem) {
            return
        }
        val random = user.random
        when (stack.useAnimation) {
            ItemUseAnimation.DRINK -> user.playSound(getDrinkingSound(), 0.5F, user.level().random.nextFloat() * 0.1F + 0.9F)
            ItemUseAnimation.EAT -> {
                user.spawnItemParticles(stack, 5)
                user.playSound(getEatingSound(), 0.5F + 0.5F * random.nextInt(2), (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F)
            }
            else -> {}
        }
    }
}
