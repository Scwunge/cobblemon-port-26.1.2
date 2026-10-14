/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import com.cobblemon.mod.common.client.render.legacy.DeferredBufferSource
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.function.Consumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.renderer.special.SpecialModelRenderers
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * Cobblemon's special item model renderers, which replaced the builtin/entity item models (and
 * BuiltinModelItemRendererMixin) in 1.21.4.
 *
 * Special renderers aren't told the display context, so item models give it to them: items/pokemon_model.json selects
 * on minecraft:display_context and gives each case a {"type": "cobblemon:pokemon", "display_context": <context>}.
 */
object CobblemonSpecialModelRenderers {
    fun register() {
        SpecialModelRenderers.ID_MAPPER.put(cobblemonResource("pokemon"), PokemonSpecialRenderer.Unbaked.CODEC)
    }
}

/** Renders the Pokémon of a Pokémon model item with [PokemonItemRenderer]. */
class PokemonSpecialRenderer(private val displayContext: ItemDisplayContext) : SpecialModelRenderer<ItemStack> {
    private val renderer = PokemonItemRenderer()

    override fun extractArgument(stack: ItemStack): ItemStack = stack.copy()

    override fun submit(stack: ItemStack?, poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, light: Int, overlay: Int, hasFoil: Boolean, outlineColor: Int) {
        if (stack == null) return
        renderer.render(stack, displayContext, poseStack, DeferredBufferSource(submitNodeCollector), light, overlay)
    }

    override fun getExtents(output: Consumer<Vector3fc>) {
        for (x in 0..1) for (y in 0..1) for (z in 0..1) output.accept(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    class Unbaked(val displayContext: ItemDisplayContext) : SpecialModelRenderer.Unbaked<ItemStack> {
        override fun bake(context: SpecialModelRenderer.BakingContext) = PokemonSpecialRenderer(displayContext)

        override fun type(): MapCodec<Unbaked> = CODEC

        companion object {
            val CODEC: MapCodec<Unbaked> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    ItemDisplayContext.CODEC.optionalFieldOf("display_context", ItemDisplayContext.NONE).forGetter(Unbaked::displayContext)
                ).apply(instance, ::Unbaked)
            }
        }
    }
}
