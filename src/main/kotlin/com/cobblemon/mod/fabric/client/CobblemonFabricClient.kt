/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.client

import com.cobblemon.mod.common.client.render.gui.CobblemonModelPipRenderer
import com.cobblemon.mod.common.api.snowstorm.ParticleMaterials

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonClient.pokedexUsageContext
import com.cobblemon.mod.common.client.CobblemonClient.reloadCodedAssets
import com.cobblemon.mod.common.client.keybind.CobblemonKeyBinds
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.client.render.item.CobblemonModelPredicateRegistry
import com.cobblemon.mod.common.item.PokedexItem
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.particle.SnowstormParticleType
import com.cobblemon.mod.common.platform.events.ClientEntityEvent
import com.cobblemon.mod.common.platform.events.ClientPlayerEvent
import com.cobblemon.mod.common.platform.events.ClientTickEvent
import com.cobblemon.mod.common.platform.events.ItemTooltipEvent
import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.cobblemon.mod.common.platform.events.RenderEvent
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.fabric.CobblemonFabric
import com.mojang.blaze3d.vertex.PoseStack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import com.cobblemon.mod.common.CobblemonClientImplementation
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys

import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.render.color.BlockColor
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

class CobblemonFabricClient: ClientModInitializer, CobblemonClientImplementation {
    override fun onInitializeClient() {
        ParticleMaterials.register()
        registerParticleFactory(CobblemonParticles.SNOWSTORM_PARTICLE_TYPE, SnowstormParticleType::Factory)
        CobblemonClient.initialize(this)
        // Poké Ball, Pokédex and wearable models are chosen by their item model definitions since 1.21.4.
        ModelLoadingPlugin.register { context ->
            CobblemonBakingOverrides.models.forEach { override ->
                context.addModel(override.key, SimpleUnbakedExtraModel.blockStateModel(override.modelLocation))
            }
        }

        CobblemonFabric.networkManager.registerClientHandlers()

        // Atlases are stitched by the game's atlas manager since 1.21.9 (see CobblemonAtlases); coded assets are
        // loaded once they are, as they need the atlases' sprites.
        CobblemonAtlases.atlases
        val assetsReloader = cobblemonResource("atlases")
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(assetsReloader, PreparableReloadListener { sharedState, _, barrier, reloadExecutor ->
            barrier.wait(Unit).thenRunAsync({ reloadCodedAssets(sharedState.resourceManager()) }, reloadExecutor)
        })
        ResourceLoader.get(PackType.CLIENT_RESOURCES).addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, assetsReloader)

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            val player = client.player
            if (player != null) {
                val itemStack = player.mainHandItem
                val offhandStack = player.offhandItem
                if (((itemStack.item is PokedexItem && player.usedItemHand == InteractionHand.MAIN_HAND) ||
                    (offhandStack.item is PokedexItem && player.usedItemHand == InteractionHand.OFF_HAND)) &&
                    player.isUsingItem &&
                    pokedexUsageContext.scanningGuiOpen
                ) {
                    val keyAttack = client.options.keyAttack
                    pokedexUsageContext.attackKeyHeld(keyAttack.isDown)
                }
            }
        })

        CobblemonKeyBinds.register(KeyMappingHelper::registerKeyMapping)

        ClientEntityEvents.ENTITY_LOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_LOAD.post(ClientEntityEvent.Load(entity, level))}
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_UNLOAD.post(ClientEntityEvent.Unload(entity, level))}
        ClientTickEvents.START_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_PRE.post(ClientTickEvent.Pre(client)) }
        ClientTickEvents.END_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_POST.post(ClientTickEvent.Post(client)) }
        ClientPlayConnectionEvents.JOIN.register { _, _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGIN.post(ClientPlayerEvent.Login(it)) } }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGOUT.post(ClientPlayerEvent.Logout(it)) } }
        ItemTooltipCallback.EVENT.register { stack, context, type, lines -> PlatformEvents.CLIENT_ITEM_TOOLTIP.post(ItemTooltipEvent(stack, context, type, lines)) }

        LevelRenderEvents.COLLECT_SUBMITS.register { context -> PlatformEvents.RENDER.post(
                RenderEvent(
                    stage = RenderEvent.Stage.TRANSLUCENT,
                    levelRenderer = context.levelRenderer(),
                    poseStack = context.poseStack(),
                    submitNodeCollector = context.submitNodeCollector(),
                    tickCounter = Minecraft.getInstance().deltaTracker,
                    camera = Minecraft.getInstance().gameRenderer.mainCamera
                )
            )
        }

        LivingEntityRenderLayerRegistrationCallback.EVENT.register { entityType, renderer, helper, _ ->
            if (entityType == EntityType.PLAYER && renderer is AvatarRenderer<*>) {
                CobblemonClient.onAddLayer(renderer) { helper.register(it) }
            }
        }

        CobblemonModelPredicateRegistry.registerPredicates()
        // 3D models inside GUIs (party, PC, Pokedex, summary, starter selection) are drawn as picture-in-picture elements.
        PictureInPictureRendererRegistry.register { CobblemonModelPipRenderer(it.minecraft().renderBuffers().bufferSource()) }
    }

    override fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>) {
        ModelLayerRegistry.registerModelLayer(modelLayer) { supplier.get() }
    }

    override fun <T : ParticleOptions> registerParticleFactory(type: ParticleType<T>, factory: (SpriteSet) -> ParticleProvider<T>) {
        ParticleProviderRegistry.getInstance().register(type, ParticleProviderRegistry.PendingParticleProvider { factory(it) })
    }

    override fun registerBlockColors(provider: BlockColor, vararg blocks: Block) {
        // Cobblemon's block models only use tint index 0.
        BlockColorRegistry.register({ state, level, pos, tints -> tints.add(provider.getColor(state, level, pos, 0)) }, *blocks)
    }

    override fun <T : BlockEntity, S : BlockEntityRenderState> registerBlockEntityRenderer(type: BlockEntityType<out T>, factory: BlockEntityRendererProvider<T, S>) {
        @Suppress("UNCHECKED_CAST")
        BlockEntityRendererRegistry.register(type as BlockEntityType<T>, factory)
    }

    override fun <T : Entity> registerEntityRenderer(type: EntityType<out T>, factory: EntityRendererProvider<T>) {
        EntityRendererRegistry.register(type, factory)
    }
}
