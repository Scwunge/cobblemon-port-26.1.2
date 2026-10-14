/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ItemOwner
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.multiplayer.ClientLevel
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.cobblemon.mod.common.item.components.TMMoveComponent
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty
import net.minecraft.world.item.ItemDisplayContext
import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.cooking.Seasonings
import com.cobblemon.mod.common.client.pot.CookingQuality
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity
import com.cobblemon.mod.common.item.interactive.PokerodItem
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player

/**
 * The item model properties Cobblemon's item models select on.
 *
 * These were item predicates (ItemProperties) used by model overrides before 1.21.4; they are now range select
 * properties (used by "range_dispatch" item models) that return the same values. The item lists below only document
 * which items use them, item model definitions refer to the properties by id.
 */
object CobblemonModelPredicateRegistry {

    private fun register(id: Identifier, function: (ItemStack, ClientLevel?, LivingEntity?, Int) -> Float) {
        lateinit var codec: MapCodec<Property>
        codec = MapCodec.unit { Property(function, codec) }
        RangeSelectItemModelProperties.ID_MAPPER.put(id, codec)
    }

    private class Property(
        val function: (ItemStack, ClientLevel?, LivingEntity?, Int) -> Float,
        val codec: MapCodec<Property>
    ) : RangeSelectItemModelProperty {
        override fun get(itemStack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int) = function(itemStack, level, owner?.asLivingEntity(), seed)
        override fun type() = codec
    }

    /** The elemental type of a TM's move, which picks its tm/<type>_tm model (ItemRendererMixin before 1.21.4). */
    private object TMTypeProperty : SelectItemModelProperty<String> {
        val TYPE: SelectItemModelProperty.Type<TMTypeProperty, String> = SelectItemModelProperty.Type.create(MapCodec.unit(TMTypeProperty), Codec.STRING)

        override fun get(itemStack: ItemStack, level: ClientLevel?, owner: LivingEntity?, seed: Int, displayContext: ItemDisplayContext): String? =
            TMMoveComponent.getTMMove(itemStack)?.elementalType?.showdownId

        override fun valueCodec(): Codec<String> = Codec.STRING

        override fun type() = TYPE
    }

    fun registerPredicates() {
        SelectItemModelProperties.ID_MAPPER.put(cobblemonResource("tm_type"), TMTypeProperty.TYPE)
        CobblemonSpecialModelRenderers.register()

        val rods = listOf(
                CobblemonItems.AZURE_ROD,
                CobblemonItems.BEAST_ROD,
                CobblemonItems.CHERISH_ROD,
                CobblemonItems.CITRINE_ROD,
                CobblemonItems.DIVE_ROD,
                CobblemonItems.DREAM_ROD,
                CobblemonItems.DUSK_ROD,
                CobblemonItems.FAST_ROD,
                CobblemonItems.FRIEND_ROD,
                CobblemonItems.GREAT_ROD,
                CobblemonItems.HEAL_ROD,
                CobblemonItems.HEAVY_ROD,
                CobblemonItems.LEVEL_ROD,
                CobblemonItems.LOVE_ROD,
                CobblemonItems.LURE_ROD,
                CobblemonItems.LUXURY_ROD,
                CobblemonItems.MASTER_ROD,
                CobblemonItems.MOON_ROD,
                CobblemonItems.NEST_ROD,
                CobblemonItems.NET_ROD,
                CobblemonItems.PARK_ROD,
                CobblemonItems.POKE_ROD,
                CobblemonItems.PREMIER_ROD,
                CobblemonItems.QUICK_ROD,
                CobblemonItems.REPEAT_ROD,
                CobblemonItems.ROSEATE_ROD,
                CobblemonItems.SAFARI_ROD,
                CobblemonItems.SLATE_ROD,
                CobblemonItems.SPORT_ROD,
                CobblemonItems.TIMER_ROD,
                CobblemonItems.ULTRA_ROD,
                CobblemonItems.VERDANT_ROD,
                CobblemonItems.ANCIENT_AZURE_ROD,
                CobblemonItems.ANCIENT_CITRINE_ROD,
                CobblemonItems.ANCIENT_FEATHER_ROD,
                CobblemonItems.ANCIENT_GIGATON_ROD,
                CobblemonItems.ANCIENT_GREAT_ROD,
                CobblemonItems.ANCIENT_HEAVY_ROD,
                CobblemonItems.ANCIENT_IVORY_ROD,
                CobblemonItems.ANCIENT_JET_ROD,
                CobblemonItems.ANCIENT_LEADEN_ROD,
                CobblemonItems.ANCIENT_ORIGIN_ROD,
                CobblemonItems.ANCIENT_POKE_ROD,
                CobblemonItems.ANCIENT_ROSEATE_ROD,
                CobblemonItems.ANCIENT_SLATE_ROD,
                CobblemonItems.ANCIENT_ULTRA_ROD,
                CobblemonItems.ANCIENT_VERDANT_ROD,
                CobblemonItems.ANCIENT_WING_ROD
        )

        register(cobblemonResource("cast")) { stack, world, entity, seed ->
                if (entity !is Player || entity.fishing !is PokeRodFishingBobberEntity) return@register 0.0f

                val rodId = entity.fishing!!.entityData.get(PokeRodFishingBobberEntity.POKEROD_ID)

                val isMainHand = stack == entity.mainHandItem
                var isOffHand = stack == entity.offhandItem

                var mainHandItem = entity.mainHandItem.item
                val isFishingWithMainHand = mainHandItem is PokerodItem && rodId == mainHandItem.pokeRodId.toString()

                if (isFishingWithMainHand) {
                    isOffHand = false
                }

                if (isMainHand && isFishingWithMainHand || isOffHand) 1.0f else 0.0f
            }

        register(cobblemonResource("ponigiri_overlay")) { stack, world, entity, seed ->
            val component = stack.get(CobblemonItemComponents.INGREDIENT)
            val id = component?.ingredientIds?.firstOrNull()?.toString() ?: return@register 0.0f

            return@register when (id) {
                "minecraft:potato" -> 0.01f
                "minecraft:beetroot" -> 0.02f
                "minecraft:carrot" -> 0.03f
                "minecraft:dried_kelp" -> 0.04f
                "cobblemon:medicinal_leek" -> 0.05f
                "minecraft:red_mushroom" -> 0.06f
                "minecraft:brown_mushroom" -> 0.07f
                "minecraft:pumpkin" -> 0.08f
                "cobblemon:rice" -> 0.09f
                "minecraft:chicken" -> 0.10f
                "minecraft:cod" -> 0.11f
                "minecraft:mutton" -> 0.12f
                "minecraft:porkchop" -> 0.13f
                "minecraft:rabbit" -> 0.14f
                "minecraft:salmon" -> 0.15f
                "minecraft:beef" -> 0.16f
                "minecraft:egg" -> 0.17f
                "minecraft:poisonous_potato" -> 0.18f
                "minecraft:rotten_flesh" -> 0.19f
                "minecraft:golden_carrot" -> 0.20f
                "cobblemon:vivichoke" -> 0.21f
                "cobblemon:tasty_tail" -> 0.22f
                "minecraft:sweet_berries" -> 0.23f
                else -> 0.0f
            }
        }

        val aprijuice = listOf(
            CobblemonItems.APRIJUICE_BLACK,
            CobblemonItems.APRIJUICE_GREEN,
            CobblemonItems.APRIJUICE_WHITE,
            CobblemonItems.APRIJUICE_BLUE,
            CobblemonItems.APRIJUICE_PINK,
            CobblemonItems.APRIJUICE_RED,
            CobblemonItems.APRIJUICE_YELLOW,
        )

        register(cobblemonResource("aprijuice_quality")) { stack, world, entity, seed ->
                val rideBoostsComponent = stack.get(CobblemonItemComponents.RIDE_BOOST) ?: return@register 0.0f
                val quality = rideBoostsComponent.getQuality()

                return@register when (quality) {
                    CookingQuality.LOW -> 0.0f
                    CookingQuality.MEDIUM -> 0.1f
                    CookingQuality.HIGH -> 0.2f
                }
            }

        /*
        ItemProperties.register(CobblemonItems.POKE_PUFF, cobblemonResource("poke_puff_combined")) { stack, _, _, _ ->
            val dominantFlavours = stack.get(CobblemonItemComponents.FLAVOUR)?.getDominantFlavours()
            val flavour = when {
                dominantFlavours == null || dominantFlavours.isEmpty() -> "plain"
                dominantFlavours.size > 1 -> "mild"
                else -> dominantFlavours.first().name.lowercase()
            }

            val ingredients =
                stack.get(CobblemonItemComponents.INGREDIENT)?.ingredientIds?.map { it.toString() } ?: emptyList()

            val hasSugar = "minecraft:sugar" in ingredients

            // todo CRAB NOTE: we may want a map or item tag instead
            val sweet = ingredients.firstOrNull {
                it.startsWith("cobblemon:") && it.endsWith("_sweet")
            }?.removePrefix("cobblemon:")?.removeSuffix("_sweet")

            val key = when {
                hasSugar && sweet != null -> "overlay_${flavour}_${sweet}"
                sweet != null && flavour != null -> "overlay_${sweet}_${flavour}"
                sweet != null -> "overlay_${sweet}"
                hasSugar && flavour != null -> "overlay_${flavour}"
                flavour != null -> "overlay_${flavour}_only"
                else -> "overlay_plain_only"
            }

            return@register PokePuffItemModelRegistry.getModelId(key)
        }
        */
    }
}