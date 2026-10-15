/*
 * Copyright (c) 2023 Ludichat, Fuzuki
 *
 * Part of Cobbreeding, ported to Minecraft 26.1.2 and merged into this Cobblemon port; see
 * licenses/cobbreeding/LICENSE (MIT).
 */

package ludichat.cobbreeding.fabric

import ludichat.cobbreeding.Cobbreeding
import ludichat.cobbreeding.commands.Commands
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** Starts Cobbreeding. Called from Cobblemon's own Fabric initialisers, so it has no entrypoints of its own. */
object CobbreedingFabric {
    fun initialize() {
        Cobbreeding.init()
        CommandRegistrationCallback.EVENT.register(Commands::register)
        CobbreedingFabricNetworkManager.registerMessages()
        CobbreedingFabricNetworkManager.registerServerHandlers()
    }

    fun initializeClient() {
        val container = FabricLoader.getInstance().getModContainer("cobblemon").get()
        ResourceLoaderImpl.registerBuiltinPack(
            Identifier.fromNamespaceAndPath(Cobbreeding.MOD_ID, "pasturefix"),
            "resourcepacks/pasturefix",
            container,
            Component.literal("Pasture Block Fix"),
            PackActivationType.ALWAYS_ENABLED
        )
        ResourceLoaderImpl.registerBuiltinPack(
            Identifier.fromNamespaceAndPath(Cobbreeding.MOD_ID, "coloredeggs"),
            "resourcepacks/coloredeggs",
            container,
            Component.literal("Colored Eggs"),
            PackActivationType.NORMAL
        )
        CobbreedingFabricNetworkManager.registerClientHandlers()
    }
}
