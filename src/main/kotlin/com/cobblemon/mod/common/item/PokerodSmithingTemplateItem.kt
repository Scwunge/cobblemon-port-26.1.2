/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item

import java.util.function.Consumer
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay

/**
 * The Pokérod smithing template. Smithing templates showed their own upgrade description as the first tooltip line
 * until 1.21.5 (now every template shows "Smithing Template"); this keeps the Pokérod upgrade line.
 */
class PokerodSmithingTemplateItem(
    private val appliesTo: Component,
    private val ingredients: Component,
    private val upgradeDescription: Component,
    baseSlotDescription: Component,
    additionsSlotDescription: Component,
    baseSlotEmptyIcons: List<Identifier>,
    additionalSlotEmptyIcons: List<Identifier>,
    properties: Properties
) : SmithingTemplateItem(appliesTo, ingredients, baseSlotDescription, additionsSlotDescription, baseSlotEmptyIcons, additionalSlotEmptyIcons, properties) {
    override fun appendHoverText(itemStack: ItemStack, context: TooltipContext, display: TooltipDisplay, builder: Consumer<Component>, tooltipFlag: TooltipFlag) {
        builder.accept(upgradeDescription)
        builder.accept(CommonComponents.EMPTY)
        builder.accept(Component.translatable("item.minecraft.smithing_template.applies_to").withStyle(net.minecraft.ChatFormatting.GRAY))
        builder.accept(CommonComponents.space().append(appliesTo))
        builder.accept(Component.translatable("item.minecraft.smithing_template.ingredients").withStyle(net.minecraft.ChatFormatting.GRAY))
        builder.accept(CommonComponents.space().append(ingredients))
    }
}
