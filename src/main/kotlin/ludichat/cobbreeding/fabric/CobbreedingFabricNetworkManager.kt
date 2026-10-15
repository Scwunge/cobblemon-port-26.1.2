package ludichat.cobbreeding.fabric

import com.cobblemon.mod.common.NetworkManager
import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.fabric.net.FabricPacketInfo
import ludichat.cobbreeding.Cobbreeding
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer

object CobbreedingFabricNetworkManager: NetworkManager {
    fun registerMessages() {
        FabricPacketInfo(Cobbreeding.BREEDING_PACKET_INFO).registerPacket(false)
        FabricPacketInfo(Cobbreeding.NEUTER_PACKET_INFO).registerPacket(false)
        FabricPacketInfo(Cobbreeding.NEUTERED_PACKET_INFO).registerPacket(true)
    }

    fun registerServerHandlers() {
        FabricPacketInfo(Cobbreeding.BREEDING_PACKET_INFO).registerServerHandler()
        FabricPacketInfo(Cobbreeding.NEUTER_PACKET_INFO).registerServerHandler()
    }

    fun registerClientHandlers() {
        FabricPacketInfo(Cobbreeding.NEUTERED_PACKET_INFO).registerClientHandler()
    }

    override fun sendPacketToPlayer(player: ServerPlayer, packet: NetworkPacket<*>) {
        ServerPlayNetworking.send(player, packet)
    }

    override fun sendToServer(packet: NetworkPacket<*>) {
        ClientPlayNetworking.send(packet)
    }
}
