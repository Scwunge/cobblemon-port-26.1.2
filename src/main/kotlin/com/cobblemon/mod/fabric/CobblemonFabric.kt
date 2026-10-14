/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization
import com.cobblemon.mod.common.CobblemonRecipeCategories
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PoiHelper
import net.fabricmc.fabric.api.registry.CompostableRegistry
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl
import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.predicate.CobblemonEntitySubPredicates
import com.cobblemon.mod.common.CobblemonMobEffects
import com.cobblemon.mod.common.api.net.serializers.*
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.loot.LootInjector
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.platform.events.*
import com.cobblemon.mod.common.sherds.CobblemonSherds
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.didSleep
import com.cobblemon.mod.common.world.CobblemonStructures
import com.cobblemon.mod.common.world.feature.CobblemonFeatures
import com.cobblemon.mod.common.world.placementmodifier.CobblemonPlacementModifierTypes
import com.cobblemon.mod.common.world.predicate.CobblemonBlockPredicates
import com.cobblemon.mod.common.world.structureprocessors.CobblemonProcessorTypes
import com.cobblemon.mod.common.world.structureprocessors.CobblemonStructureProcessorListOverrides
import com.cobblemon.mod.fabric.net.CobblemonFabricNetworkManager
import com.cobblemon.mod.fabric.permission.FabricPermissionValidator
import com.mojang.brigadier.arguments.ArgumentType
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.stats.Stats
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.reflect.KClass

object CobblemonFabric : CobblemonImplementation {

    override val modAPI = ModAPI.FABRIC

    private var server: MinecraftServer? = null

    override val networkManager = CobblemonFabricNetworkManager

    fun initialize() {
        Cobblemon.preInitialize(this)

        Cobblemon.statistics.registerStats()
        Cobblemon.statistics.stats.forEach { entry ->
            val cobblemonStat = entry.value
            Registry.register(BuiltInRegistries.CUSTOM_STAT, cobblemonStat.resourceLocation, cobblemonStat.resourceLocation)
            Stats.CUSTOM.get(cobblemonStat.resourceLocation, cobblemonStat.formatter)
        }

        Cobblemon.initialize()
        networkManager.registerMessages()
        networkManager.registerServerHandlers()

        //This has to be registered elsewhere on forge so we cant do it in common
        CobblemonSherds.registerSherds()
        CobblemonBlockPredicates.touch()
        CobblemonPlacementModifierTypes.touch()
        CobblemonProcessorTypes.touch()
        CobblemonActivities.activities.forEach { Registry.register(BuiltInRegistries.ACTIVITY, cobblemonResource(it.name), it) }
        CobblemonSensors.sensors.forEach { (key, sensorType) -> Registry.register(BuiltInRegistries.SENSOR_TYPE, cobblemonResource(key), sensorType) }
        CobblemonMemories.memories.forEach { (key, memoryModuleType) -> Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, cobblemonResource(key), memoryModuleType) }

        EntitySleepEvents.STOP_SLEEPING.register { playerEntity, _ ->
            if (playerEntity !is ServerPlayer) {
                return@register
            }

            playerEntity.didSleep()
        }

        // This doesn't work (yet) because Fabric does not let us make non-potion ingredients/inputs/outputs.
        // Throws an exception about the ingredient not being a potion. Maybe one day?
//        FabricBrewingRecipeRegistryBuilder.BUILD.register { builder ->
//            BrewingRecipes.recipes.forEach { (input, ingredient, output) ->
//                if (input is CobblemonPotionIngredient) {
//                    // This doesn't respect the specific potion types that are considered inputs. This is less than perfect
//                    // but will at least let JEI/EMI etc fill in the ingredients.
//                    builder.addContainerRecipe(Items.POTION, ingredient, output)
//                } else {
//                    input as CobblemonItemIngredient
//                    builder.addContainerRecipe(input.item, ingredient, output)
//                }
//            }
//        }

        Cobblemon.builtinPacks
            .filter { it.neededMods.all(Cobblemon.implementation::isModInstalled) }
            .forEach {
                val mod = FabricLoader.getInstance().getModContainer(Cobblemon.MODID).get()
                val resourcePackActivationType = when (it.activationBehaviour) {
                    ResourcePackActivationBehaviour.NORMAL -> PackActivationType.NORMAL
                    ResourcePackActivationBehaviour.DEFAULT_ENABLED -> PackActivationType.DEFAULT_ENABLED
                    ResourcePackActivationBehaviour.ALWAYS_ENABLED -> PackActivationType.ALWAYS_ENABLED
                }
                val id = cobblemonResource(it.id)
                val subPath = "${ if (it.packType == PackType.CLIENT_RESOURCES) "resourcepacks" else "datapacks" }/${id.path}"
                ResourceLoaderImpl.registerBuiltinPack(id, subPath, mod, it.displayName, resourcePackActivationType)
            }

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register { player, isLogin ->
            if (isLogin) {
                Cobblemon.dataProvider.sync(player)
            }
        }
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            PlatformEvents.SERVER_STARTING.post(ServerEvent.Starting(server))
            CobblemonStructures.registerJigsaws(server)
            CobblemonStructureProcessorListOverrides.register(server)
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server -> PlatformEvents.SERVER_STARTED.post(ServerEvent.Started(server)) }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            server.playerList.players.forEach { player -> PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(player)) }
            PlatformEvents.SERVER_STOPPING.post(ServerEvent.Stopping(server))
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server -> PlatformEvents.SERVER_STOPPED.post(ServerEvent.Stopped(server)) }
        ServerTickEvents.START_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_PRE.post(ServerTickEvent.Pre(server)) }
        ServerTickEvents.END_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_POST.post(ServerTickEvent.Post(server)) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, server -> server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGIN.post(ServerPlayerEvent.Login(handler.player)) } }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            if (!server.isStopped) server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(handler.player)) }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                PlatformEvents.PLAYER_DEATH.postThen(
                    event = ServerPlayerEvent.Death(entity),
                    ifSucceeded = {},
                    ifCanceled = { return@register false }
                )
            }
            return@register true
        }

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register { player, origin, destination ->
            PlatformEvents.CHANGE_DIMENSION.post(ChangeDimensionEvent(player, origin, destination))
        }


        UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS
            PlatformEvents.RIGHT_CLICK_BLOCK.postThen(
                event = ServerPlayerEvent.RightClickBlock(serverPlayer, hitResult.blockPos, hand, hitResult.direction),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )
            return@register InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            val item = player.getItemInHand(hand)
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS

            PlatformEvents.RIGHT_CLICK_ENTITY.postThen(
                event = ServerPlayerEvent.RightClickEntity(serverPlayer, item, hand, entity),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )

            return@register InteractionResult.PASS
        }

        LootTableEvents.MODIFY.register { key, tableBuilder, _, _ ->
            LootInjector.attemptInjection(key.identifier(), tableBuilder::withPool)
        }

        CommandRegistrationCallback.EVENT.register(CobblemonCommands::register)
    }

    override fun isModInstalled(id: String) = FabricLoader.getInstance().isModLoaded(id)

    override fun environment(): Environment {
        return when(FabricLoader.getInstance().environmentType) {
            EnvType.CLIENT -> Environment.CLIENT
            EnvType.SERVER -> Environment.SERVER
            else -> throw IllegalStateException("Fabric implementation cannot resolve environment yet")
        }
    }

    override fun registerPermissionValidator() {
        if (this.isModInstalled("fabric-permissions-api-v0")) {
            Cobblemon.permissionValidator = FabricPermissionValidator()
        }
    }

    override fun registerSoundEvents() {
        CobblemonSounds.register { identifier, sound -> Registry.register(CobblemonSounds.registry, identifier, sound) }
    }

    override fun registerDataComponents() {
        CobblemonItemComponents.register { identifier, component -> Registry.register(CobblemonItemComponents.registry, identifier, component) }
    }

    override fun registerEntityDataSerializers() {
        EntityDataSerializers.registerSerializer(Vec3DataSerializer)
        EntityDataSerializers.registerSerializer(StringSetDataSerializer)
        EntityDataSerializers.registerSerializer(PoseTypeDataSerializer)
        EntityDataSerializers.registerSerializer(PlatformTypeDataSerializer)
        EntityDataSerializers.registerSerializer(IdentifierDataSerializer)
        EntityDataSerializers.registerSerializer(UUIDSetDataSerializer)
        EntityDataSerializers.registerSerializer(OptionalUUIDDataSerializer)
        EntityDataSerializers.registerSerializer(CompoundTagDataSerializer)
        EntityDataSerializers.registerSerializer(NPCPlayerTextureSerializer)
        EntityDataSerializers.registerSerializer(RideBoostsDataSerializer)
    }

    override fun registerItems() {
        CobblemonItems.register { identifier, item -> Registry.register(CobblemonItems.registry, identifier, item) }
        CobblemonItemGroups.register { provider ->
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, provider.key, FabricCreativeModeTab.builder()
                .title(provider.displayName)
                .icon(provider.displayIconProvider)
                .displayItems(provider.entryCollector)
                .build())
        }

        CobblemonItemGroups.injectorKeys().forEach { key ->
            CreativeModeTabEvents.modifyOutputEvent(key).register { content ->
                val fabricInjector = FabricItemGroupInjector(content)
                CobblemonItemGroups.inject(key, fabricInjector)
            }
        }
    }

    override fun registerBlocks() {
        CobblemonBlocks.register { identifier, item -> Registry.register(CobblemonBlocks.registry, identifier, item) }
        CobblemonBlocks.strippedBlocks().forEach(StrippableBlockRegistry::register)
    }

    override fun registerEntityTypes() {
        CobblemonEntities.register { identifier, type -> Registry.register(CobblemonEntities.registry, identifier, type) }
    }

    override fun registerEntityAttributes() {
        CobblemonEntities.registerAttributes { entityType, builder -> FabricDefaultAttributeRegistry.register(entityType, builder) }
    }

    override fun registerBlockEntityTypes() {
        CobblemonBlockEntities.register { identifier, type -> Registry.register(CobblemonBlockEntities.registry, identifier, type) }
    }

    override fun registerPoiTypes() {
        CobblemonPoiTypes.register { identifier, type -> PoiHelper.register(identifier, type.maxTickets, type.validRange, type.matchingStates) }
    }

    override fun registerVillagers() {
        CobblemonVillagerProfessions.register { identifier, profession -> Registry.register(CobblemonVillagerProfessions.registry, identifier, profession) }

        // Trades are data driven since 26.1, see CobblemonTradeOffers.
    }

    override fun registerRecipeSerializers() {
        CobblemonRecipeSerializers.register { identifier, factory -> Registry.register(CobblemonRecipeSerializers.registry, identifier, factory) }
        // Since 1.21.2 the client only receives recipes that are explicitly synchronized; the cooking pot and brewing
        // stand look their recipes up on the client (seasoning slots, tooltips, previews), as they did before.
        RecipeSynchronization.synchronizeRecipeSerializer(CobblemonRecipeSerializers.COOKING_POT_COOKING)
        RecipeSynchronization.synchronizeRecipeSerializer(CobblemonRecipeSerializers.COOKING_POT_SHAPELESS)
        RecipeSynchronization.synchronizeRecipeSerializer(CobblemonRecipeSerializers.BREWING_STAND)
    }

    override fun registerRecipeTypes() {
        CobblemonRecipeTypes.register { identifier, factory -> Registry.register(CobblemonRecipeTypes.registry, identifier, factory) }
        CobblemonRecipeCategories.register()
    }


    override fun registerWorldGenFeatures() {
        CobblemonFeatures.register { identifier, feature -> Registry.register(CobblemonFeatures.registry, identifier, feature) }
    }

    override fun registerParticles() {
        CobblemonParticles.register { identifier, particleType -> Registry.register(CobblemonParticles.registry, identifier, particleType) }
    }

    override fun registerMenu() {
        CobblemonMenuType.register { identifier, factory -> Registry.register(CobblemonMenuType.registry, identifier, factory) }
    }

    override fun addFeatureToWorldGen(feature: ResourceKey<PlacedFeature>, step: GenerationStep.Decoration, validTag: TagKey<Biome>?) {
        val predicate: (BiomeSelectionContext) -> Boolean = { context -> validTag == null || context.hasTag(validTag) }
        BiomeModifications.addFeature(predicate, step, feature)
    }

    override fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>> registerCommandArgument(identifier: Identifier, argumentClass: KClass<A>, serializer: ArgumentTypeInfo<A, T>) {
        ArgumentTypeRegistry.registerArgumentType(identifier, argumentClass.java, serializer)
    }

    override fun registerBooleanGameRule(name: String, category: GameRuleCategory, defaultValue: Boolean): GameRule<Boolean> =
        GameRuleBuilder.forBoolean(defaultValue).category(category).buildAndRegister(cobblemonResource(name))

    override fun registerCriteria() {
        CobblemonCriteria.register { id, obj ->
            Registry.register(CobblemonCriteria.registry, id, obj)
        }
    }

    override fun registerEntitySubPredicates() {
        CobblemonEntitySubPredicates.register { resourceLocation, mapCodec ->
            Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, resourceLocation, mapCodec)
        }
    }

    override fun registerResourceReloader(
        identifier: Identifier,
        type: PackType,
        dependencies: Collection<Identifier>,
        reloaderFactory: (HolderLookup.Provider) -> PreparableReloadListener
    ) {
        val resourceLoader = ResourceLoader.get(type)
        resourceLoader.registerReloadListener(identifier, CobblemonReloadListener(type, reloaderFactory))
        dependencies.forEach { dependency -> resourceLoader.addListenerOrdering(dependency, identifier) }
    }

    override fun server(): MinecraftServer? = if (this.environment() == Environment.CLIENT) Minecraft.getInstance().singleplayerServer else this.server

    override fun registerCompostable(item: ItemLike, chance: Float) {
        CompostableRegistry.INSTANCE.add(item, chance)
    }

    override fun registerMobEffects() {
        CobblemonMobEffects.register { identifier, effect ->
            Registry.register(CobblemonMobEffects.registry, identifier, effect)
        }
    }

    /**
     * Creates the actual reloader for each reload; server data reloaders get the registries being reloaded.
     */
    private class CobblemonReloadListener(
        private val type: PackType,
        private val reloaderFactory: (HolderLookup.Provider) -> PreparableReloadListener
    ) : PreparableReloadListener {
        override fun reload(
            currentReload: PreparableReloadListener.SharedState,
            taskExecutor: Executor,
            preparationBarrier: PreparableReloadListener.PreparationBarrier,
            reloadExecutor: Executor
        ): CompletableFuture<Void> {
            val registries = if (type == PackType.SERVER_DATA) currentReload.get(ResourceLoader.REGISTRY_LOOKUP_KEY) else RegistryAccess.EMPTY
            return reloaderFactory(registries).reload(currentReload, taskExecutor, preparationBarrier, reloadExecutor)
        }
    }

    private class FabricItemGroupInjector(private val fabricItemGroupEntries: FabricCreativeModeTabOutput) : CobblemonItemGroups.Injector {
        override fun putFirst(item: ItemLike) {
            this.fabricItemGroupEntries.prepend(item)
        }

        override fun putBefore(item: ItemLike, target: ItemLike) {
            this.fabricItemGroupEntries.insertBefore(target, item)
        }

        override fun putAfter(item: ItemLike, target: ItemLike) {
            this.fabricItemGroupEntries.insertAfter(target, item)
        }

        override fun putLast(item: ItemLike) {
            this.fabricItemGroupEntries.accept(item)
        }
    }
}
